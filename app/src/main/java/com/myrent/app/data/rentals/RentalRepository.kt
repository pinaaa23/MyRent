package com.myrent.app.data.rentals

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Repository pengelola koleksi "rentals" di Firestore
 */
class RentalRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    fun getRentalsFlow(cabangId: String): Flow<List<Rental>> = callbackFlow {
        if (cabangId.isEmpty()) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }

        val listener = firestore.collection("rentals")
            .whereEqualTo("cabangId", cabangId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.map { doc ->
                    Rental(
                        id = doc.id,
                        cabangId = doc.getString("cabangId") ?: "",
                        itemId = doc.getString("itemId") ?: "",
                        namaBarang = doc.getString("namaBarang") ?: "",
                        customerId = doc.getString("customerId") ?: "",
                        namaPenyewa = doc.getString("namaPenyewa") ?: "",
                        noWa = doc.getString("noWa") ?: "",
                        tanggalMulai = doc.getLong("tanggalMulai") ?: System.currentTimeMillis(),
                        tanggalKembali = doc.getLong("tanggalKembali") ?: System.currentTimeMillis(),
                        lamaHari = (doc.getLong("lamaHari") ?: 1L).toInt(),
                        jumlah = (doc.getLong("jumlah") ?: 1L).toInt(),
                        hargaTotal = doc.getLong("hargaTotal") ?: 0L,
                        dp = doc.getLong("dp") ?: 0L,
                        sisaBayar = doc.getLong("sisaBayar") ?: 0L,
                        statusPembayaran = doc.getString("statusPembayaran") ?: "lunas",
                        status = doc.getString("status") ?: "berjalan",
                        sudahFollowUpH1 = doc.getBoolean("sudahFollowUpH1") ?: false,
                        sudahFollowUpH = doc.getBoolean("sudahFollowUpH") ?: false,
                        dibuatOleh = doc.getString("dibuatOleh") ?: "",
                        dibuatPada = doc.getLong("dibuatPada") ?: System.currentTimeMillis(),
                        dikembalikanPada = doc.getLong("dikembalikanPada"),
                        catatan = doc.getString("catatan") ?: "",
                        invoiceId = doc.getString("invoiceId") ?: "",
                        metodePembayaran = doc.getString("metodePembayaran") ?: "Tunai",
                        pelunasanPada = doc.getLong("pelunasanPada"),
                        jumlahPelunasan = doc.getLong("jumlahPelunasan") ?: 0L
                    )
                }?.sortedByDescending { it.dibuatPada } ?: emptyList()

                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    suspend fun tambahSewa(rental: Rental, itemId: String): Result<Rental> = runCatching {
        val map = mapOf(
            "cabangId" to rental.cabangId,
            "itemId" to itemId,
            "namaBarang" to rental.namaBarang,
            "customerId" to rental.customerId,
            "namaPenyewa" to rental.namaPenyewa,
            "noWa" to rental.noWa,
            "tanggalMulai" to rental.tanggalMulai,
            "tanggalKembali" to rental.tanggalKembali,
            "lamaHari" to rental.lamaHari,
            "jumlah" to rental.jumlah,
            "hargaTotal" to rental.hargaTotal,
            "dp" to rental.dp,
            "sisaBayar" to rental.sisaBayar,
            "statusPembayaran" to rental.statusPembayaran,
            "status" to "berjalan",
            "sudahFollowUpH1" to false,
            "sudahFollowUpH" to false,
            "dibuatOleh" to rental.dibuatOleh,
            "dibuatPada" to rental.dibuatPada,
            "catatan" to rental.catatan,
            "invoiceId" to rental.invoiceId,
            "metodePembayaran" to rental.metodePembayaran,
            "pelunasanPada" to rental.pelunasanPada,
            "jumlahPelunasan" to rental.jumlahPelunasan
        )

        val docRef = firestore.collection("rentals").add(map).await()

        val itemDoc = firestore.collection("items").document(itemId).get().await()
        if (itemDoc.exists()) {
            val stokTersediaSaatIni = (itemDoc.getLong("stokTersedia") ?: 1L).toInt()
            val stokTersediaBaru = (stokTersediaSaatIni - rental.jumlah).coerceAtLeast(0)
            val statusBaru = if (stokTersediaBaru == 0) "disewa" else "tersedia"

            firestore.collection("items").document(itemId).update(
                mapOf(
                    "stokTersedia" to stokTersediaBaru,
                    "status" to statusBaru
                )
            ).await()
        }

        val customerMap = mapOf(
            "cabangId" to rental.cabangId,
            "nama" to rental.namaPenyewa,
            "noWa" to rental.noWa,
            "dibuatPada" to System.currentTimeMillis()
        )
        firestore.collection("customers").add(customerMap).await()

        rental.copy(id = docRef.id)
    }

    suspend fun tandaiDikembalikan(rental: Rental): Result<Unit> = runCatching {
        val now = System.currentTimeMillis()
        firestore.collection("rentals").document(rental.id).update(
            mapOf(
                "status" to "selesai",
                "dikembalikanPada" to now
            )
        ).await()

        if (rental.itemId.isNotEmpty()) {
            val itemDoc = firestore.collection("items").document(rental.itemId).get().await()
            if (itemDoc.exists()) {
                val stokTotal = (itemDoc.getLong("stok") ?: 1L).toInt()
                val stokTersediaSaatIni = (itemDoc.getLong("stokTersedia") ?: 0L).toInt()
                val stokTersediaBaru = (stokTersediaSaatIni + rental.jumlah).coerceAtMost(stokTotal)

                firestore.collection("items").document(rental.itemId).update(
                    mapOf(
                        "stokTersedia" to stokTersediaBaru,
                        "status" to "tersedia"
                    )
                ).await()
            }
        }
    }

    suspend fun updateSewa(rental: Rental): Result<Unit> = runCatching {
        val map = mapOf(
            "namaPenyewa" to rental.namaPenyewa,
            "noWa" to rental.noWa,
            "tanggalMulai" to rental.tanggalMulai,
            "tanggalKembali" to rental.tanggalKembali,
            "lamaHari" to rental.lamaHari,
            "jumlah" to rental.jumlah,
            "hargaTotal" to rental.hargaTotal,
            "dp" to rental.dp,
            "sisaBayar" to rental.sisaBayar,
            "statusPembayaran" to rental.statusPembayaran,
            "catatan" to rental.catatan,
            "metodePembayaran" to rental.metodePembayaran,
            "pelunasanPada" to rental.pelunasanPada,
            "jumlahPelunasan" to rental.jumlahPelunasan
        )
        firestore.collection("rentals").document(rental.id).update(map).await()
    }

    suspend fun hapusSewa(rental: Rental): Result<Unit> = runCatching {
        firestore.collection("rentals").document(rental.id).delete().await()
        if (rental.itemId.isNotEmpty() && rental.status == "berjalan") {
            val itemDoc = firestore.collection("items").document(rental.itemId).get().await()
            if (itemDoc.exists()) {
                val stokTotal = (itemDoc.getLong("stok") ?: 1L).toInt()
                val stokTersediaSaatIni = (itemDoc.getLong("stokTersedia") ?: 0L).toInt()
                val stokTersediaBaru = (stokTersediaSaatIni + rental.jumlah).coerceAtMost(stokTotal)

                firestore.collection("items").document(rental.itemId).update(
                    mapOf(
                        "stokTersedia" to stokTersediaBaru,
                        "status" to "tersedia"
                    )
                ).await()
            }
        }
    }
}
