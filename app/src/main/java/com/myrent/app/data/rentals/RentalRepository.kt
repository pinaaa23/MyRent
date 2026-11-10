package com.myrent.app.data.rentals

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.myrent.app.data.customer.CustomerRepository
import com.myrent.app.data.customer.normalizeNoWa
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Repository pengelola koleksi "rentals" terisolasi per akun toko (toko/{uid}/rentals)
 */
class RentalRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val customerRepository: CustomerRepository = CustomerRepository()
) {
    private val currentUid: String
        get() = auth.currentUser?.uid ?: ""

    private val isEmailVerified: Boolean
        get() = auth.currentUser?.isEmailVerified == true

    private fun getRentalsCollection(uid: String = currentUid) =
        firestore.collection("toko").document(uid.ifEmpty { "global" }).collection("rentals")

    private fun getItemsCollection(uid: String = currentUid) =
        firestore.collection("toko").document(uid.ifEmpty { "global" }).collection("items")

    fun getRentalsFlow(): Flow<List<Rental>> = callbackFlow {
        val uid = currentUid
        if (uid.isEmpty() || !isEmailVerified) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }

        val userRentalsRef = getRentalsCollection(uid)

        val listener = userRentalsRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
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
        val uid = currentUid
        require(uid.isNotEmpty() && isEmailVerified) { "Pengguna belum login atau email belum diverifikasi" }

        val noWaNorm = normalizeNoWa(rental.noWa)
        val customerRes = customerRepository.upsertCustomer(
            nama = rental.namaPenyewa,
            noWaRaw = rental.noWa
        )
        val customerId = customerRes.getOrNull()?.id ?: noWaNorm

        val map = mapOf(
            "cabangId" to rental.cabangId,
            "itemId" to itemId,
            "namaBarang" to rental.namaBarang,
            "customerId" to customerId,
            "namaPenyewa" to rental.namaPenyewa.trim(),
            "noWa" to noWaNorm,
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

        val docRef = getRentalsCollection(uid).add(map).await()

        val itemDocRef = getItemsCollection(uid).document(itemId)
        val itemDoc = itemDocRef.get().await()
        if (itemDoc.exists()) {
            val stokTersediaSaatIni = (itemDoc.getLong("stokTersedia") ?: 1L).toInt()
            val stokTersediaBaru = (stokTersediaSaatIni - rental.jumlah).coerceAtLeast(0)
            val statusBaru = if (stokTersediaBaru == 0) "disewa" else "tersedia"

            itemDocRef.update(
                mapOf(
                    "stokTersedia" to stokTersediaBaru,
                    "status" to statusBaru
                )
            ).await()
        }

        rental.copy(id = docRef.id, customerId = customerId, noWa = noWaNorm)
    }

    suspend fun tandaiDikembalikan(rental: Rental): Result<Unit> = runCatching {
        val uid = currentUid
        require(uid.isNotEmpty() && isEmailVerified) { "Pengguna belum login atau email belum diverifikasi" }

        val now = System.currentTimeMillis()
        getRentalsCollection(uid).document(rental.id).update(
            mapOf(
                "status" to "selesai",
                "dikembalikanPada" to now
            )
        ).await()

        if (rental.itemId.isNotEmpty()) {
            val itemDocRef = getItemsCollection(uid).document(rental.itemId)
            val itemDoc = itemDocRef.get().await()
            if (itemDoc.exists()) {
                val stokTotal = (itemDoc.getLong("stok") ?: 1L).toInt()
                val stokTersediaSaatIni = (itemDoc.getLong("stokTersedia") ?: 0L).toInt()
                val stokTersediaBaru = (stokTersediaSaatIni + rental.jumlah).coerceAtMost(stokTotal)

                itemDocRef.update(
                    mapOf(
                        "stokTersedia" to stokTersediaBaru,
                        "status" to "tersedia"
                    )
                ).await()
            }
        }
    }

    suspend fun updateSewa(rental: Rental): Result<Unit> = runCatching {
        val uid = currentUid
        require(uid.isNotEmpty() && isEmailVerified) { "Pengguna belum login atau email belum diverifikasi" }

        val noWaNorm = normalizeNoWa(rental.noWa)
        if (rental.namaPenyewa.isNotEmpty()) {
            customerRepository.upsertCustomer(rental.namaPenyewa, rental.noWa)
        }

        val map = mapOf(
            "namaPenyewa" to rental.namaPenyewa.trim(),
            "noWa" to noWaNorm,
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
        getRentalsCollection(uid).document(rental.id).update(map).await()
    }

    suspend fun hapusSewa(rental: Rental): Result<Unit> = runCatching {
        val uid = currentUid
        require(uid.isNotEmpty() && isEmailVerified) { "Pengguna belum login atau email belum diverifikasi" }

        getRentalsCollection(uid).document(rental.id).delete().await()
        if (rental.itemId.isNotEmpty() && rental.status == "berjalan") {
            val itemDocRef = getItemsCollection(uid).document(rental.itemId)
            val itemDoc = itemDocRef.get().await()
            if (itemDoc.exists()) {
                val stokTotal = (itemDoc.getLong("stok") ?: 1L).toInt()
                val stokTersediaSaatIni = (itemDoc.getLong("stokTersedia") ?: 0L).toInt()
                val stokTersediaBaru = (stokTersediaSaatIni + rental.jumlah).coerceAtMost(stokTotal)

                itemDocRef.update(
                    mapOf(
                        "stokTersedia" to stokTersediaBaru,
                        "status" to "tersedia"
                    )
                ).await()
            }
        }
    }
}
