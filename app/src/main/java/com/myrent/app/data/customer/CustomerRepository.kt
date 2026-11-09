package com.myrent.app.data.customer

import com.google.firebase.firestore.FirebaseFirestore
import com.myrent.app.data.rentals.Rental
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Repository pengelola koleksi "customers" di Firestore dengan Self-Healing Deduplikasi
 */
class CustomerRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    /**
     * Mendapatkan daftar penyewa unik secara realtime via Flow berdasarkan cabangId.
     * Secara otomatis membersihkan dokumen duplikat dari Firestore jika ada.
     */
    fun getCustomersFlow(cabangId: String): Flow<List<Customer>> = callbackFlow {
        if (cabangId.isEmpty()) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }

        val listener = firestore.collection("customers")
            .whereEqualTo("cabangId", cabangId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val allDocs = snapshot?.documents?.mapNotNull { doc ->
                    val noWa = doc.getString("noWa") ?: ""
                    val noWaNorm = normalizeNoWa(noWa)
                    if (noWaNorm.length in 10..14) {
                        Customer(
                            id = doc.id,
                            cabangId = doc.getString("cabangId") ?: "",
                            nama = doc.getString("nama") ?: "",
                            noWa = noWaNorm,
                            dibuatPada = doc.getLong("dibuatPada") ?: System.currentTimeMillis(),
                            diubahPada = doc.getLong("diubahPada") ?: System.currentTimeMillis()
                        )
                    } else null
                } ?: emptyList()

                // Kelompokkan berdasarkan noWaNorm untuk menghilangkan duplikat
                val grouped = allDocs.groupBy { it.noWa }
                val uniqueList = mutableListOf<Customer>()

                grouped.forEach { (_, docs) ->
                    if (docs.size == 1) {
                        uniqueList.add(docs[0])
                    } else if (docs.size > 1) {
                        // Jika ada duplikat di Firestore, ambil dokumen yang paling baru diubah
                        val primary = docs.maxByOrNull { it.diubahPada } ?: docs[0]
                        uniqueList.add(primary)

                        // Bersihkan dokumen duplikat lama di Firestore
                        docs.filter { it.id != primary.id }.forEach { duplicateDoc ->
                            firestore.collection("customers").document(duplicateDoc.id).delete()
                        }
                    }
                }

                val sortedList = uniqueList.sortedByDescending { it.diubahPada }
                trySend(sortedList)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Membuat atau memperbarui dokumen customer unik berdasarkan cabangId + noWaNorm
     * Document ID: "{cabangId}_{noWaNorm}"
     */
    suspend fun upsertCustomer(cabangId: String, nama: String, noWaRaw: String): Result<Customer> = runCatching {
        val noWaNorm = normalizeNoWa(noWaRaw)
        require(noWaNorm.length in 10..14) { "Nomor WA harus 10 sampai 14 digit." }

        val docId = "${cabangId}_${noWaNorm}"
        val docRef = firestore.collection("customers").document(docId)
        val snapshot = docRef.get().await()

        val now = System.currentTimeMillis()

        if (snapshot.exists()) {
            val currentNama = snapshot.getString("nama") ?: ""
            if (currentNama != nama.trim()) {
                docRef.update(
                    mapOf(
                        "nama" to nama.trim(),
                        "diubahPada" to now
                    )
                ).await()
            }
            Customer(
                id = docId,
                cabangId = cabangId,
                nama = nama.trim(),
                noWa = noWaNorm,
                dibuatPada = snapshot.getLong("dibuatPada") ?: now,
                diubahPada = now
            )
        } else {
            val customer = Customer(
                id = docId,
                cabangId = cabangId,
                nama = nama.trim(),
                noWa = noWaNorm,
                dibuatPada = now,
                diubahPada = now
            )
            val map = mapOf(
                "cabangId" to cabangId,
                "nama" to customer.nama,
                "noWa" to customer.noWa,
                "dibuatPada" to customer.dibuatPada,
                "diubahPada" to customer.diubahPada
            )
            docRef.set(map).await()
            customer
        }
    }

    /**
     * Memperbarui data nama / nomor WA penyewa
     */
    suspend fun updateCustomer(
        oldCustomer: Customer,
        newNama: String,
        newNoWaRaw: String
    ): Result<Customer> = runCatching {
        val newNoWaNorm = normalizeNoWa(newNoWaRaw)
        require(newNoWaNorm.length in 10..14) { "Nomor WA harus 10 sampai 14 digit." }

        val newDocId = "${oldCustomer.cabangId}_${newNoWaNorm}"
        val now = System.currentTimeMillis()

        if (newDocId != oldCustomer.id) {
            val targetDoc = firestore.collection("customers").document(newDocId).get().await()
            if (targetDoc.exists()) {
                error("Nomor WA $newNoWaNorm sudah terdaftar untuk penyewa lain.")
            }

            firestore.collection("customers").document(oldCustomer.id).delete().await()

            val newCustomer = Customer(
                id = newDocId,
                cabangId = oldCustomer.cabangId,
                nama = newNama.trim(),
                noWa = newNoWaNorm,
                dibuatPada = oldCustomer.dibuatPada,
                diubahPada = now
            )

            val map = mapOf(
                "cabangId" to oldCustomer.cabangId,
                "nama" to newCustomer.nama,
                "noWa" to newCustomer.noWa,
                "dibuatPada" to newCustomer.dibuatPada,
                "diubahPada" to newCustomer.diubahPada
            )
            firestore.collection("customers").document(newDocId).set(map).await()
            newCustomer
        } else {
            firestore.collection("customers").document(oldCustomer.id).update(
                mapOf(
                    "nama" to newNama.trim(),
                    "diubahPada" to now
                )
            ).await()

            oldCustomer.copy(nama = newNama.trim(), diubahPada = now)
        }
    }

    /**
     * Migrasi data penyewa dari rentals lama ke koleksi customers
     */
    suspend fun migrateCustomersFromRentalsIfEmpty(
        cabangId: String,
        rentalsList: List<Rental>
    ): Result<Unit> = runCatching {
        if (cabangId.isEmpty() || rentalsList.isEmpty()) return@runCatching

        val grouped = rentalsList.groupBy { normalizeNoWa(it.noWa) }
        grouped.forEach { (noWaNorm, rentals) ->
            if (noWaNorm.length in 10..14) {
                val latestRental = rentals.maxByOrNull { it.dibuatPada }
                if (latestRental != null) {
                    val docId = "${cabangId}_${noWaNorm}"
                    val docRef = firestore.collection("customers").document(docId)
                    val snapshot = docRef.get().await()

                    if (!snapshot.exists()) {
                        val map = mapOf(
                            "cabangId" to cabangId,
                            "nama" to latestRental.namaPenyewa.trim(),
                            "noWa" to noWaNorm,
                            "dibuatPada" to latestRental.dibuatPada,
                            "diubahPada" to latestRental.dibuatPada
                        )
                        docRef.set(map).await()
                    }

                    rentals.forEach { r ->
                        if (r.id.isNotEmpty() && r.customerId.isEmpty()) {
                            firestore.collection("rentals").document(r.id)
                                .update("customerId", docId)
                        }
                    }
                }
            }
        }
    }
}
