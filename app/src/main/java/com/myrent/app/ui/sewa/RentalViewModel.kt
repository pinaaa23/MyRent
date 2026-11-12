package com.myrent.app.ui.sewa

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myrent.app.data.auth.AuthRepository
import com.myrent.app.data.auth.UserDoc
import com.myrent.app.data.customer.Customer
import com.myrent.app.data.customer.CustomerRepository
import com.myrent.app.data.items.Item
import com.myrent.app.data.items.ItemRepository
import com.myrent.app.data.rentals.PaymentRecord
import com.myrent.app.data.rentals.Rental
import com.myrent.app.data.rentals.RentalItem
import com.myrent.app.data.rentals.RentalRepository
import com.myrent.app.data.rentals.mapJenisMetode
import com.myrent.app.data.settings.DefaultStoreConfig
import com.myrent.app.data.settings.StoreConfigDoc
import com.myrent.app.data.settings.StoreConfigRepository
import com.myrent.app.data.settings.StoreProfileDoc
import com.myrent.app.data.settings.StoreProfileRepository
import com.myrent.app.data.settings.TemplateReplacer
import com.myrent.app.ui.screens.formatDate
import com.myrent.app.ui.screens.formatRupiah
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class RentalDraftForm(
    val namaPenyewa: String = "",
    val noWa: String = "",
    val medsosPenyewa: String = "",
    val tglMulaiMs: Long = System.currentTimeMillis(),
    val tglKembaliMs: Long = System.currentTimeMillis() + 86400000L,
    val dpText: String = "",
    val catatan: String = "",
    val selectedItems: List<Item> = emptyList()
)

sealed class RentalUiState {
    object Idle : RentalUiState()
    object Loading : RentalUiState()
    data class Success(val message: String? = null) : RentalUiState()
    data class Error(val message: String) : RentalUiState()
}

/**
 * ViewModel untuk mengelola Transaksi Sewa Barang, Pembayaran DP & Pelunasan
 */
class RentalViewModel(
    private val rentalRepository: RentalRepository = RentalRepository(),
    private val itemRepository: ItemRepository = ItemRepository(),
    private val customerRepository: CustomerRepository = CustomerRepository(),
    private val authRepository: AuthRepository = AuthRepository(),
    private val storeConfigRepository: StoreConfigRepository = StoreConfigRepository(),
    private val storeProfileRepository: StoreProfileRepository = StoreProfileRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<RentalUiState>(RentalUiState.Idle)
    val uiState: StateFlow<RentalUiState> = _uiState.asStateFlow()

    private val _draftForm = MutableStateFlow(RentalDraftForm())
    val draftForm: StateFlow<RentalDraftForm> = _draftForm.asStateFlow()

    fun updateDraftForm(
        namaPenyewa: String = _draftForm.value.namaPenyewa,
        noWa: String = _draftForm.value.noWa,
        medsosPenyewa: String = _draftForm.value.medsosPenyewa,
        tglMulaiMs: Long = _draftForm.value.tglMulaiMs,
        tglKembaliMs: Long = _draftForm.value.tglKembaliMs,
        dpText: String = _draftForm.value.dpText,
        catatan: String = _draftForm.value.catatan,
        selectedItems: List<Item> = _draftForm.value.selectedItems
    ) {
        _draftForm.value = RentalDraftForm(
            namaPenyewa = namaPenyewa,
            noWa = noWa,
            medsosPenyewa = medsosPenyewa,
            tglMulaiMs = tglMulaiMs,
            tglKembaliMs = tglKembaliMs,
            dpText = dpText,
            catatan = catatan,
            selectedItems = selectedItems
        )
    }

    fun clearDraftForm() {
        _draftForm.value = RentalDraftForm()
    }

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedFilter = MutableStateFlow("Semua")
    val selectedFilter: StateFlow<String> = _selectedFilter.asStateFlow()

    private val _allRentals = MutableStateFlow<List<Rental>>(emptyList())

    val userDocState: StateFlow<UserDoc> = authRepository.getUserProfileFlow()
        .stateIn(viewModelScope, SharingStarted.Lazily, UserDoc())

    val storeConfig: StateFlow<StoreConfigDoc> = storeConfigRepository.getStoreConfigFlow()
        .stateIn(viewModelScope, SharingStarted.Lazily, StoreConfigDoc())

    val customerList: StateFlow<List<Customer>> = customerRepository.getCustomersFlow()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Dipulihkan dari versi git: Daftar barang berstatus tersedia untuk formulir sewa baru
    val availableItems: StateFlow<List<Item>> = itemRepository.getItemsFlow()
        .map { items -> items.filter { it.stokTersedia > 0 && it.status != "perbaikan" } }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Dipulihkan dari versi git: Logika filter daftar sewa asli
    val filteredRentals: StateFlow<List<Rental>> = combine(_allRentals, _searchQuery, _selectedFilter) { rentals, query, filter ->
        val q = query.trim().lowercase()
        rentals.filter { r ->
            val matchQuery = q.isEmpty() || r.namaPenyewa.lowercase().contains(q) || r.noWa.contains(q) || r.namaBarang.lowercase().contains(q)
            val matchFilter = when (filter) {
                "Berjalan" -> r.status == "berjalan"
                "Hampir Jatuh Tempo" -> r.status == "berjalan" && isSameDay(r.tanggalKembali, System.currentTimeMillis())
                "Terlambat" -> r.status == "terlambat"
                "Selesai" -> r.status == "selesai"
                else -> true
            }
            matchQuery && matchFilter
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        loadRentals()
    }

    private fun loadRentals() {
        viewModelScope.launch {
            rentalRepository.getRentalsFlow().collect { rentals ->
                _allRentals.value = rentals
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onFilterSelected(filter: String) {
        _selectedFilter.value = filter
    }

    /**
     * Menyimpan transaksi sewa baru dan memproses catatan pembayaran DP pertama.
     * Prefix invoice dikembalikan ke "INV-"
     */
    fun prosesPembayaranDanSimpanSewa(
        selectedItem: Item?,
        namaPenyewa: String,
        noWa: String,
        medsosPenyewa: String = "",
        tanggalMulai: Long,
        tanggalKembali: Long,
        lamaHari: Int,
        jumlah: Int,
        hargaTotal: Long,
        dp: Long,
        metodePembayaran: String,
        catatan: String,
        rentedItems: List<RentalItem> = emptyList(),
        onSuccess: (Rental) -> Unit
    ) {
        if (selectedItem == null && rentedItems.isEmpty()) {
            _uiState.value = RentalUiState.Error("Silakan pilih barang terlebih dahulu.")
            return
        }
        if (namaPenyewa.trim().isEmpty()) {
            _uiState.value = RentalUiState.Error("Nama penyewa wajib diisi.")
            return
        }
        if (noWa.trim().isEmpty()) {
            _uiState.value = RentalUiState.Error("Nomor WA penyewa wajib diisi.")
            return
        }
        if (medsosPenyewa.trim().isEmpty()) {
            _uiState.value = RentalUiState.Error("Media sosial konsumen (IG/TikTok/FB) wajib diisi.")
            return
        }
        if (dp < 0L) {
            _uiState.value = RentalUiState.Error("Uang muka / DP tidak boleh negatif.")
            return
        }
        if (dp > hargaTotal) {
            _uiState.value = RentalUiState.Error("Uang Muka / DP tidak boleh melebihi Total Biaya Sewa.")
            return
        }

        viewModelScope.launch {
            _uiState.value = RentalUiState.Loading

            val now = System.currentTimeMillis()
            val sisaAmount = if (hargaTotal > dp) hargaTotal - dp else 0L
            val statusBayar = if (sisaAmount == 0L) "lunas" else "belum_lunas"

            val sdfId = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.getDefault())
            val invoiceId = "INV-${sdfId.format(Date(now))}"

            val finalItemList = if (rentedItems.isNotEmpty()) {
                rentedItems
            } else if (selectedItem != null) {
                listOf(
                    RentalItem(
                        itemId = selectedItem.id,
                        nama = selectedItem.nama,
                        kategori = selectedItem.kategori,
                        hargaSatuan = selectedItem.hargaSewa,
                        jumlah = jumlah,
                        fotoBase64 = selectedItem.fotoBase64
                    )
                )
            } else emptyList()

            val primaryItem = finalItemList.firstOrNull()
            val firstItemId = primaryItem?.itemId ?: (selectedItem?.id ?: "")

            val rentalToCreate = Rental(
                cabangId = "",
                itemId = firstItemId,
                namaBarang = primaryItem?.nama ?: (selectedItem?.nama ?: "Barang Sewa"),
                namaPenyewa = namaPenyewa.trim(),
                noWa = noWa.trim(),
                medsosPenyewa = medsosPenyewa.trim(),
                tanggalMulai = tanggalMulai,
                tanggalKembali = tanggalKembali,
                lamaHari = lamaHari,
                jumlah = jumlah,
                hargaTotal = hargaTotal,
                dp = dp,
                sisaBayar = sisaAmount,
                statusPembayaran = statusBayar,
                status = "berjalan",
                dibuatOleh = authRepository.currentUser?.uid ?: "",
                dibuatPada = now,
                catatan = catatan.trim(),
                invoiceId = invoiceId,
                metodePembayaran = metodePembayaran,
                items = finalItemList
            )

            val result = rentalRepository.tambahSewa(rentalToCreate, firstItemId)
            result.onSuccess { savedRental ->
                _uiState.value = RentalUiState.Idle
                onSuccess(savedRental)
            }.onFailure { e ->
                _uiState.value = RentalUiState.Error(e.message ?: "Gagal menyimpan transaksi sewa.")
            }
        }
    }

    /**
     * Memproses pelunasan sisa pembayaran tanpa menimpa nilai DP awal
     */
    fun lunasiPembayaran(
        rental: Rental,
        metodePembayaran: String,
        onSuccess: (Rental) -> Unit
    ) {
        if (rental.sisaBayar == 0L) {
            _uiState.value = RentalUiState.Error("Transaksi ini sudah lunas.")
            return
        }

        viewModelScope.launch {
            _uiState.value = RentalUiState.Loading

            val now = System.currentTimeMillis()
            val sisaAmount = rental.sisaBayar

            val newPayment = PaymentRecord(
                id = "pay_pelunasan_${now}",
                rentalId = rental.id,
                jumlah = sisaAmount,
                jenis = "Pelunasan",
                metode = metodePembayaran.ifEmpty { "Tidak diketahui" },
                jenisMetode = mapJenisMetode(metodePembayaran),
                tanggal = now
            )

            val currentPayments = rental.getAllPaymentRecords().toMutableList()
            currentPayments.add(newPayment)

            val updatedRental = rental.copy(
                statusPembayaran = "lunas",
                sisaBayar = 0L,
                metodePembayaran = metodePembayaran,
                pelunasanPada = now,
                jumlahPelunasan = sisaAmount,
                payments = currentPayments
            )

            val result = rentalRepository.updateSewa(updatedRental)
            result.onSuccess {
                _uiState.value = RentalUiState.Idle
                onSuccess(updatedRental)
            }.onFailure {
                _uiState.value = RentalUiState.Error("Gagal memproses pelunasan pembayaran.")
            }
        }
    }

    fun tandaiDikembalikan(rental: Rental, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = RentalUiState.Loading
            val res = rentalRepository.tandaiDikembalikan(rental)
            res.onSuccess {
                _uiState.value = RentalUiState.Idle
                onSuccess()
            }.onFailure {
                _uiState.value = RentalUiState.Error("Gagal memperbarui status sewa.")
            }
        }
    }

    fun updateSewa(
        rental: Rental,
        namaPenyewa: String,
        noWa: String,
        medsosPenyewa: String = "",
        tanggalMulai: Long,
        tanggalKembali: Long,
        lamaHari: Int,
        jumlah: Int,
        hargaTotal: Long,
        dp: Long,
        metodePembayaran: String,
        catatan: String,
        onSuccess: () -> Unit
    ) {
        if (namaPenyewa.trim().isEmpty() || noWa.trim().isEmpty()) {
            _uiState.value = RentalUiState.Error("Nama dan Nomor WA penyewa wajib diisi.")
            return
        }
        if (medsosPenyewa.trim().isEmpty()) {
            _uiState.value = RentalUiState.Error("Media sosial konsumen (IG/TikTok/FB) wajib diisi.")
            return
        }
        if (dp < 0L) {
            _uiState.value = RentalUiState.Error("Uang muka / DP tidak boleh negatif.")
            return
        }
        if (dp > hargaTotal) {
            _uiState.value = RentalUiState.Error("Uang Muka / DP tidak boleh melebihi Total Biaya Sewa.")
            return
        }

        viewModelScope.launch {
            _uiState.value = RentalUiState.Loading
            val sisa = if (hargaTotal > dp) hargaTotal - dp else 0L
            val statusBayar = if (sisa == 0L) "lunas" else "belum_lunas"

            val updated = rental.copy(
                namaPenyewa = namaPenyewa.trim(),
                noWa = noWa.trim(),
                medsosPenyewa = medsosPenyewa.trim(),
                tanggalMulai = tanggalMulai,
                tanggalKembali = tanggalKembali,
                lamaHari = lamaHari,
                jumlah = jumlah,
                hargaTotal = hargaTotal,
                dp = dp,
                sisaBayar = sisa,
                statusPembayaran = statusBayar,
                metodePembayaran = metodePembayaran,
                catatan = catatan.trim()
            )

            val result = rentalRepository.updateSewa(updated)
            result.onSuccess {
                _uiState.value = RentalUiState.Idle
                onSuccess()
            }.onFailure {
                _uiState.value = RentalUiState.Error("Gagal memperbarui transaksi sewa.")
            }
        }
    }

    fun hapusSewa(rental: Rental, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = RentalUiState.Loading
            val result = rentalRepository.hapusSewa(rental)
            result.onSuccess {
                _uiState.value = RentalUiState.Idle
                onSuccess()
            }.onFailure {
                _uiState.value = RentalUiState.Error("Gagal menghapus transaksi sewa.")
            }
        }
    }

    /**
     * Dipulihkan dari git: Buka obrolan WhatsApp biasa ke nomor penyewa
     */
    fun kirimWhatsApp(context: Context, rental: Rental) {
        viewModelScope.launch {
            var formattedNo = rental.noWa.replace("[^0-9]".toRegex(), "")
            if (formattedNo.startsWith("0")) {
                formattedNo = "62" + formattedNo.substring(1)
            }
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$formattedNo"))
                context.startActivity(intent)
            } catch (e: Exception) {
                _uiState.value = RentalUiState.Error("Gagal membuka WhatsApp. Pastikan WhatsApp terpasang.")
            }
        }
    }

    fun kirimWhatsAppNota(
        context: Context,
        rental: Rental
    ) {
        viewModelScope.launch {
            var formattedNo = rental.noWa.replace("[^0-9]".toRegex(), "")
            if (formattedNo.startsWith("0")) {
                formattedNo = "62" + formattedNo.substring(1)
            }

            val userDoc = authRepository.getUserProfile().getOrDefault(UserDoc())
            val storeProfileDoc = storeProfileRepository.getStoreProfile().getOrDefault(StoreProfileDoc())
            val storeConfigDoc = storeConfigRepository.getStoreConfig().getOrDefault(StoreConfigDoc())

            val template = storeConfigDoc.templatePesanNota.ifEmpty { DefaultStoreConfig.TEMPLATE_NOTA_DEFAULT }
            val tglMulai = formatDate(rental.tanggalMulai)
            val tglKembali = formatDate(rental.tanggalKembali)

            val tokoNama = storeProfileDoc.nama.ifEmpty { userDoc.namaToko.ifEmpty { "MYRENT STORE" } }
            val alamatStr = storeProfileDoc.alamat.ifEmpty { userDoc.alamatToko }
            val telpStr = storeProfileDoc.noTelp.ifEmpty { userDoc.noWaToko }

            val pelunasanTglStr = if (rental.pelunasanPada != null) formatDate(rental.pelunasanPada) else ""

            val pesan = TemplateReplacer.replaceVariables(
                template = template,
                nama = rental.namaPenyewa,
                barang = rental.getSummaryBarangWA(),
                tanggal = "$tglMulai s/d $tglKembali (${rental.lamaHari} Hari)",
                total = formatRupiah(rental.hargaTotal),
                dp = formatRupiah(rental.dp),
                sisa = formatRupiah(rental.sisaBayar),
                invoice = rental.invoiceId.ifEmpty { "INV-${rental.dibuatPada}" },
                toko = tokoNama,
                catatan = rental.catatan,
                telpToko = telpStr,
                alamatToko = alamatStr,
                jumlahPelunasan = rental.jumlahPelunasan,
                pelunasanMetode = rental.metodePembayaran,
                pelunasanTanggalStr = pelunasanTglStr
            )

            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$formattedNo?text=${Uri.encode(pesan)}"))
                context.startActivity(intent)
            } catch (e: Exception) {
                _uiState.value = RentalUiState.Error("Gagal membuka WhatsApp. Pastikan WhatsApp terpasang.")
            }
        }
    }

    fun setErrorMessage(msg: String) {
        _uiState.value = RentalUiState.Error(msg)
    }

    fun resetUiState() {
        _uiState.value = RentalUiState.Idle
    }

    private fun isSameDay(ts1: Long, ts2: Long): Boolean {
        val cal1 = Calendar.getInstance().apply { timeInMillis = ts1 }
        val cal2 = Calendar.getInstance().apply { timeInMillis = ts2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }
}
