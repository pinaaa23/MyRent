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
 * Repository pengelola koleksi "rentals" terisolasi per akun toko (toko/{uid}/rentals) dengan Dukungan Atomic Multi-Item Stock
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
            val list = snapshot?.documents?.mapNotNull { doc ->
                try {
                    @Suppress("UNCHECKED_CAST")
                    val rawItems = doc.get("items") as? List<Map<String, Any>>
                    val itemsList = rawItems?.mapNotNull { iMap ->
                        RentalItem(
                            itemId = iMap["itemId"] as? String ?: "",
                            nama = iMap["nama"] as? String ?: "",
                            kategori = iMap["kategori"] as? String ?: "",
                            hargaSatuan = (iMap["hargaSatuan"] as? Number)?.toLong() ?: 0L,
                            jumlah = (iMap["jumlah"] as? Number)?.toInt() ?: 1,
                            fotoBase64 = iMap["fotoBase64"] as? String ?: ""
                        )
                    } ?: emptyList()

                    @Suppress("UNCHECKED_CAST")
                    val rawPayments = doc.get("payments") as? List<Map<String, Any>>
                    val paymentsList = rawPayments?.mapNotNull { pMap ->
                        val metRaw = pMap["metode"] as? String ?: ""
                        val metStr = metRaw.ifEmpty { "Tidak diketahui" }
                        PaymentRecord(
                            id = pMap["id"] as? String ?: "",
                            rentalId = pMap["rentalId"] as? String ?: doc.id,
                            jumlah = (pMap["jumlah"] as? Number)?.toLong() ?: 0L,
                            jenis = pMap["jenis"] as? String ?: "DP",
                            metode = metStr,
                            jenisMetode = pMap["jenisMetode"] as? String ?: mapJenisMetode(metRaw),
                            tanggal = (pMap["tanggal"] as? Number)?.toLong() ?: System.currentTimeMillis()
                        )
                    } ?: emptyList()

                    Rental(
                        id = doc.id,
                        cabangId = doc.getString("cabangId") ?: "",
                        itemId = doc.getString("itemId") ?: "",
                        namaBarang = doc.getString("namaBarang") ?: "",
                        customerId = doc.getString("customerId") ?: "",
                        namaPenyewa = doc.getString("namaPenyewa") ?: "",
                        noWa = doc.getString("noWa") ?: "",
                        medsosPenyewa = doc.getString("medsosPenyewa") ?: "",
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
                        metodePembayaran = doc.getString("metodePembayaran") ?: "",
                        pelunasanPada = doc.getLong("pelunasanPada"),
                        jumlahPelunasan = doc.getLong("jumlahPelunasan") ?: 0L,
                        items = itemsList,
                        payments = paymentsList
                    )
                } catch (e: Exception) {
                    null
                }
            }?.sortedByDescending { it.dibuatPada } ?: emptyList()

            trySend(list)
        }
        awaitClose { listener.remove() }
    }

    /**
     * Menyimpan transaksi sewa baru (Multi-Item) dan mengurangi stok semua barang secara atomic
     */
    suspend fun tambahSewa(rental: Rental, singleItemId: String = ""): Result<Rental> = runCatching {
        val uid = currentUid
        require(uid.isNotEmpty() && isEmailVerified) { "Pengguna belum login atau email belum diverifikasi" }

        val noWaNorm = normalizeNoWa(rental.noWa)
        val customerRes = customerRepository.upsertCustomer(
            nama = rental.namaPenyewa,
            noWaRaw = rental.noWa
        )
        val customerId = customerRes.getOrNull()?.id ?: noWaNorm

        val itemList = rental.getItemsList()
        require(itemList.isNotEmpty()) { "Daftar barang yang disewa tidak boleh kosong." }

        val itemsCollectionRef = getItemsCollection(uid)

        val initialPayments = mutableListOf<PaymentRecord>()
        if (rental.dp > 0L) {
            val met = rental.metodePembayaran.ifEmpty { "Tidak diketahui" }
            initialPayments.add(
                PaymentRecord(
                    id = "pay_dp_${System.currentTimeMillis()}",
                    rentalId = "",
                    jumlah = rental.dp,
                    jenis = "DP",
                    metode = met,
                    jenisMetode = mapJenisMetode(rental.metodePembayaran),
                    tanggal = rental.dibuatPada
                )
            )
        }

        val itemsMapList = itemList.map {
            mapOf(
                "itemId" to it.itemId,
                "nama" to it.nama,
                "kategori" to it.kategori,
                "hargaSatuan" to it.hargaSatuan,
                "jumlah" to it.jumlah,
                "fotoBase64" to it.fotoBase64
            )
        }

        val paymentsMapList = initialPayments.map {
            mapOf(
                "id" to it.id,
                "rentalId" to it.rentalId,
                "jumlah" to it.jumlah,
                "jenis" to it.jenis,
                "metode" to it.metode,
                "jenisMetode" to it.jenisMetode,
                "tanggal" to it.tanggal
            )
        }

        val primaryItem = itemList.first()
        val firstItemId = primaryItem.itemId.ifEmpty { singleItemId }

        val map = mapOf(
            "cabangId" to rental.cabangId,
            "itemId" to firstItemId,
            "namaBarang" to rental.getNamaDisplay(),
            "customerId" to customerId,
            "namaPenyewa" to rental.namaPenyewa.trim(),
            "noWa" to noWaNorm,
            "medsosPenyewa" to rental.medsosPenyewa.trim(),
            "tanggalMulai" to rental.tanggalMulai,
            "tanggalKembali" to rental.tanggalKembali,
            "lamaHari" to rental.lamaHari,
            "jumlah" to rental.getTotalUnitCount(),
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
            "jumlahPelunasan" to rental.jumlahPelunasan,
            "items" to itemsMapList,
            "payments" to paymentsMapList
        )

        // Lakukan verifikasi stok & pengurangan stok atomic
        for (rItem in itemList) {
            if (rItem.itemId.isNotEmpty()) {
                val itemDocRef = itemsCollectionRef.document(rItem.itemId)
                val itemDoc = itemDocRef.get().await()
                if (itemDoc.exists()) {
                    val stokTersediaSaatIni = (itemDoc.getLong("stokTersedia") ?: 1L).toInt()
                    if (stokTersediaSaatIni < rItem.jumlah) {
                        error("Stok barang \"${rItem.nama}\" tidak mencukupi (tersisa $stokTersediaSaatIni, dibutuhkan ${rItem.jumlah}).")
                    }
                    val stokTersediaBaru = (stokTersediaSaatIni - rItem.jumlah).coerceAtLeast(0)
                    val statusBaru = if (stokTersediaBaru == 0) "disewa" else "tersedia"

                    itemDocRef.update(
                        mapOf(
                            "stokTersedia" to stokTersediaBaru,
                            "status" to statusBaru
                        )
                    ).await()
                }
            }
        }

        val docRef = getRentalsCollection(uid).add(map).await()

        rental.copy(
            id = docRef.id,
            customerId = customerId,
            noWa = noWaNorm,
            itemId = firstItemId,
            namaBarang = rental.getNamaDisplay(),
            payments = initialPayments.map { it.copy(rentalId = docRef.id) }
        )
    }

    suspend fun updateSewa(rental: Rental): Result<Unit> = runCatching {
        val uid = currentUid
        require(uid.isNotEmpty() && isEmailVerified) { "Pengguna belum login atau email belum diverifikasi" }

        val noWaNorm = normalizeNoWa(rental.noWa)
        if (rental.namaPenyewa.isNotEmpty()) {
            customerRepository.upsertCustomer(rental.namaPenyewa, rental.noWa)
        }

        val currentPayments = rental.getAllPaymentRecords().toMutableList()
        val dpIndex = currentPayments.indexOfFirst { it.jenis == "DP" }
        if (dpIndex >= 0) {
            if (rental.dp > 0L) {
                currentPayments[dpIndex] = currentPayments[dpIndex].copy(jumlah = rental.dp)
            } else {
                currentPayments.removeAt(dpIndex)
            }
        } else if (rental.dp > 0L) {
            currentPayments.add(
                0,
                PaymentRecord(
                    id = "pay_dp_${System.currentTimeMillis()}",
                    rentalId = rental.id,
                    jumlah = rental.dp,
                    jenis = "DP",
                    metode = rental.metodePembayaran.ifEmpty { "Tidak diketahui" },
                    jenisMetode = mapJenisMetode(rental.metodePembayaran),
                    tanggal = rental.dibuatPada
                )
            )
        }

        val itemList = rental.getItemsList()
        val itemsMapList = itemList.map {
            mapOf(
                "itemId" to it.itemId,
                "nama" to it.nama,
                "kategori" to it.kategori,
                "hargaSatuan" to it.hargaSatuan,
                "jumlah" to it.jumlah,
                "fotoBase64" to it.fotoBase64
            )
        }

        val paymentsMapList = currentPayments.map {
            mapOf(
                "id" to it.id,
                "rentalId" to rental.id,
                "jumlah" to it.jumlah,
                "jenis" to it.jenis,
                "metode" to it.metode,
                "jenisMetode" to it.jenisMetode,
                "tanggal" to it.tanggal
            )
        }

        val map = mapOf(
            "namaPenyewa" to rental.namaPenyewa.trim(),
            "noWa" to noWaNorm,
            "medsosPenyewa" to rental.medsosPenyewa.trim(),
            "tanggalMulai" to rental.tanggalMulai,
            "tanggalKembali" to rental.tanggalKembali,
            "lamaHari" to rental.lamaHari,
            "jumlah" to rental.getTotalUnitCount(),
            "hargaTotal" to rental.hargaTotal,
            "dp" to rental.dp,
            "sisaBayar" to rental.sisaBayar,
            "statusPembayaran" to rental.statusPembayaran,
            "catatan" to rental.catatan,
            "invoiceId" to rental.invoiceId,
            "metodePembayaran" to rental.metodePembayaran,
            "pelunasanPada" to rental.pelunasanPada,
            "jumlahPelunasan" to rental.jumlahPelunasan,
            "items" to itemsMapList,
            "payments" to paymentsMapList
        )
        getRentalsCollection(uid).document(rental.id).update(map).await()
    }

    /**
     * Tandai sewa dikembalikan: Mengembalikan stok SEMUA barang yang ada di dalam transaksi ini
     */
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

        val itemsCollectionRef = getItemsCollection(uid)
        for (rItem in rental.getItemsList()) {
            if (rItem.itemId.isNotEmpty()) {
                val itemDocRef = itemsCollectionRef.document(rItem.itemId)
                val itemDoc = itemDocRef.get().await()
                if (itemDoc.exists()) {
                    val stokTotal = (itemDoc.getLong("stok") ?: 1L).toInt()
                    val stokTersediaSaatIni = (itemDoc.getLong("stokTersedia") ?: 0L).toInt()
                    val stokTersediaBaru = (stokTersediaSaatIni + rItem.jumlah).coerceAtMost(stokTotal)

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

    suspend fun hapusSewa(rental: Rental): Result<Unit> = runCatching {
        val uid = currentUid
        require(uid.isNotEmpty() && isEmailVerified) { "Pengguna belum login atau email belum diverifikasi" }

        getRentalsCollection(uid).document(rental.id).delete().await()

        if (rental.status == "berjalan") {
            val itemsCollectionRef = getItemsCollection(uid)
            for (rItem in rental.getItemsList()) {
                if (rItem.itemId.isNotEmpty()) {
                    val itemDocRef = itemsCollectionRef.document(rItem.itemId)
                    val itemDoc = itemDocRef.get().await()
                    if (itemDoc.exists()) {
                        val stokTotal = (itemDoc.getLong("stok") ?: 1L).toInt()
                        val stokTersediaSaatIni = (itemDoc.getLong("stokTersedia") ?: 0L).toInt()
                        val stokTersediaBaru = (stokTersediaSaatIni + rItem.jumlah).coerceAtMost(stokTotal)

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
    }
}
