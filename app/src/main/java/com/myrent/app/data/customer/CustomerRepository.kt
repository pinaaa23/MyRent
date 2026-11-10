package com.myrent.app.data.customer

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.myrent.app.data.rentals.Rental
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Repository pengelola koleksi "customers" terisolasi per akun toko (toko/{uid}/customers)
 */
class CustomerRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val currentUid: String
        get() = auth.currentUser?.uid ?: ""

    private val isEmailVerified: Boolean
        get() = auth.currentUser?.isEmailVerified == true

    private fun getCustomersCollection(uid: String = currentUid) =
        firestore.collection("toko").document(uid.ifEmpty { "global" }).collection("customers")

    private fun getRentalsCollection(uid: String = currentUid) =
        firestore.collection("toko").document(uid.ifEmpty { "global" }).collection("rentals")

    /**
     * Mendapatkan semua penyewa secara realtime via Flow milik akun toko (toko/{uid}/customers).
     * Membersihkan duplikat berdasarkan noWaNorm secara otomatis.
     */
    fun getCustomersFlow(): Flow<List<Customer>> = callbackFlow {
        val uid = currentUid
        if (uid.isEmpty() || !isEmailVerified) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }

        val userCustomersRef = getCustomersCollection(uid)

        val listener = userCustomersRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
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

            val grouped = allDocs.groupBy { it.noWa }
            val uniqueList = mutableListOf<Customer>()

            grouped.forEach { (_, docs) ->
                if (docs.size == 1) {
                    uniqueList.add(docs[0])
                } else if (docs.size > 1) {
                    val primary = docs.maxByOrNull { it.diubahPada } ?: docs[0]
                    uniqueList.add(primary)

                    docs.filter { it.id != primary.id }.forEach { duplicateDoc ->
                        userCustomersRef.document(duplicateDoc.id).delete()
                    }
                }
            }

            val sortedList = uniqueList.sortedByDescending { it.diubahPada }
            trySend(sortedList)
        }

        awaitClose { listener.remove() }
    }

    /**
     * Membuat atau memperbarui dokumen customer unik berdasarkan noWaNorm di toko/{uid}/customers
     */
    suspend fun upsertCustomer(nama: String, noWaRaw: String): Result<Customer> = runCatching {
        val uid = currentUid
        require(uid.isNotEmpty() && isEmailVerified) { "Pengguna belum login atau email belum diverifikasi" }

        val noWaNorm = normalizeNoWa(noWaRaw)
        require(noWaNorm.length in 10..14) { "Nomor WA harus 10 sampai 14 digit." }

        val docId = noWaNorm
        val docRef = getCustomersCollection(uid).document(docId)
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
                cabangId = snapshot.getString("cabangId") ?: "",
                nama = nama.trim(),
                noWa = noWaNorm,
                dibuatPada = snapshot.getLong("dibuatPada") ?: now,
                diubahPada = now
            )
        } else {
            val customer = Customer(
                id = docId,
                cabangId = "",
                nama = nama.trim(),
                noWa = noWaNorm,
                dibuatPada = now,
                diubahPada = now
            )
            val map = mapOf(
                "cabangId" to "",
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
     * Memperbarui data nama / nomor WA penyewa di toko/{uid}/customers
     */
    suspend fun updateCustomer(
        oldCustomer: Customer,
        newNama: String,
        newNoWaRaw: String
    ): Result<Customer> = runCatching {
        val uid = currentUid
        require(uid.isNotEmpty() && isEmailVerified) { "Pengguna belum login atau email belum diverifikasi" }

        val newNoWaNorm = normalizeNoWa(newNoWaRaw)
        require(newNoWaNorm.length in 10..14) { "Nomor WA harus 10 sampai 14 digit." }

        val newDocId = newNoWaNorm
        val now = System.currentTimeMillis()
        val custCollection = getCustomersCollection(uid)

        if (newDocId != oldCustomer.id) {
            val targetDoc = custCollection.document(newDocId).get().await()
            if (targetDoc.exists()) {
                error("Nomor WA $newNoWaNorm sudah terdaftar untuk penyewa lain.")
            }

            custCollection.document(oldCustomer.id).delete().await()

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
            custCollection.document(newDocId).set(map).await()
            newCustomer
        } else {
            custCollection.document(oldCustomer.id).update(
                mapOf(
                    "nama" to newNama.trim(),
                    "diubahPada" to now
                )
            ).await()

            oldCustomer.copy(nama = newNama.trim(), diubahPada = now)
        }
    }

    /**
     * Migrasi data penyewa dari rentals ke customers jika koleksi toko/{uid}/customers masih kosong
     */
    suspend fun migrateCustomersFromRentalsIfEmpty(rentalsList: List<Rental>): Result<Unit> = runCatching {
        val uid = currentUid
        if (uid.isEmpty() || !isEmailVerified || rentalsList.isEmpty()) return@runCatching

        val custCollection = getCustomersCollection(uid)
        val snapshot = custCollection.limit(1).get().await()

        if (snapshot.isEmpty) {
            val grouped = rentalsList.groupBy { normalizeNoWa(it.noWa) }
            grouped.forEach { (noWaNorm, rentals) ->
                if (noWaNorm.length in 10..14) {
                    val latestRental = rentals.maxByOrNull { it.dibuatPada }
                    if (latestRental != null) {
                        val docId = noWaNorm
                        val docRef = custCollection.document(docId)
                        val docSnap = docRef.get().await()

                        if (!docSnap.exists()) {
                            val map = mapOf(
                                "cabangId" to latestRental.cabangId,
                                "nama" to latestRental.namaPenyewa.trim(),
                                "noWa" to noWaNorm,
                                "dibuatPada" to latestRental.dibuatPada,
                                "diubahPada" to latestRental.dibuatPada
                            )
                            docRef.set(map).await()
                        }

                        rentals.forEach { r ->
                            if (r.id.isNotEmpty() && r.customerId.isEmpty()) {
                                getRentalsCollection(uid).document(r.id)
                                    .update("customerId", docId)
                            }
                        }
                    }
                }
            }
        }
    }
}
