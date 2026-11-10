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
import com.myrent.app.data.customer.normalizeNoWa
import com.myrent.app.data.items.Item
import com.myrent.app.data.items.ItemRepository
import com.myrent.app.data.rentals.Rental
import com.myrent.app.data.rentals.RentalRepository
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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed class RentalUiState {
    object Idle : RentalUiState()
    object Loading : RentalUiState()
    data class Error(val message: String) : RentalUiState()
}

/**
 * ViewModel untuk mengelola Transaksi Sewa, Pembayaran QRIS/Tunai, Pelunasan, Nota, dan Daftar Customer
 */
class RentalViewModel(
    private val authRepository: AuthRepository = AuthRepository(),
    private val rentalRepository: RentalRepository = RentalRepository(),
    private val itemRepository: ItemRepository = ItemRepository(),
    private val customerRepository: CustomerRepository = CustomerRepository(),
    private val storeConfigRepository: StoreConfigRepository = StoreConfigRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<RentalUiState>(RentalUiState.Idle)
    val uiState: StateFlow<RentalUiState> = _uiState.asStateFlow()

    private val _kasirNama = MutableStateFlow("")
    val kasirNama: StateFlow<String> = _kasirNama.asStateFlow()

    private val _userDocState = MutableStateFlow(UserDoc())
    val userDocState: StateFlow<UserDoc> = _userDocState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedFilter = MutableStateFlow("Semua")
    val selectedFilter: StateFlow<String> = _selectedFilter.asStateFlow()

    private val _allRentals = MutableStateFlow<List<Rental>>(emptyList())
    private val _availableItems = MutableStateFlow<List<Item>>(emptyList())
    val availableItems: StateFlow<List<Item>> = _availableItems.asStateFlow()

    private val _customerList = MutableStateFlow<List<Customer>>(emptyList())
    val customerList: StateFlow<List<Customer>> = _customerList.asStateFlow()

    val storeConfig: StateFlow<StoreConfigDoc> = storeConfigRepository.getStoreConfigFlow()
        .stateIn(viewModelScope, SharingStarted.Lazily, StoreConfigDoc())

    val filteredRentals: StateFlow<List<Rental>> = combine(
        _allRentals,
        _searchQuery,
        _selectedFilter
    ) { rentals, query, filter ->
        var list = rentals

        if (query.trim().isNotEmpty()) {
            val q = query.trim().lowercase()
            list = list.filter {
                it.namaPenyewa.lowercase().contains(q) || it.noWa.contains(q) || it.namaBarang.lowercase().contains(q)
            }
        }

        when (filter) {
            "Berjalan" -> list.filter { it.status == "berjalan" }
            "Hampir Jatuh Tempo" -> {
                val now = System.currentTimeMillis()
                val oneDayMs = 24 * 60 * 60 * 1000L
                list.filter { it.status == "berjalan" && (it.tanggalKembali - now) in 0..oneDayMs }
            }
            "Terlambat" -> {
                val now = System.currentTimeMillis()
                list.filter { it.status == "berjalan" && it.tanggalKembali < now }
            }
            "Selesai" -> list.filter { it.status == "selesai" }
            else -> list
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            val profileRes = authRepository.getUserProfile()
            profileRes.onSuccess { userDoc ->
                _userDocState.value = userDoc
                _kasirNama.value = userDoc.nama
            }

            launch {
                rentalRepository.getRentalsFlow().collect { list ->
                    _allRentals.value = list
                }
            }
            launch {
                itemRepository.getItemsFlow().collect { list ->
                    _availableItems.value = list.filter { it.stokTersedia > 0 && it.status != "perbaikan" }
                }
            }
            launch {
                customerRepository.getCustomersFlow().collect { list ->
                    _customerList.value = list.distinctBy { normalizeNoWa(it.noWa) }
                }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onFilterSelected(filter: String) {
        _selectedFilter.value = filter
    }

    fun setErrorMessage(msg: String) {
        _uiState.value = RentalUiState.Error(msg)
    }

    fun prosesPembayaranDanSimpanSewa(
        selectedItem: Item?,
        namaPenyewa: String,
        noWa: String,
        tanggalMulai: Long,
        tanggalKembali: Long,
        lamaHari: Int,
        jumlah: Int,
        hargaTotal: Long,
        dp: Long,
        metodePembayaran: String,
        catatan: String,
        onSuccess: (Rental) -> Unit
    ) {
        if (selectedItem == null) {
            _uiState.value = RentalUiState.Error("Silakan pilih barang terlebih dahulu.")
            return
        }
        if (namaPenyewa.trim().isEmpty() || noWa.trim().isEmpty()) {
            _uiState.value = RentalUiState.Error("Nama dan Nomor WA penyewa wajib diisi.")
            return
        }
        if (jumlah <= 0) {
            _uiState.value = RentalUiState.Error("Jumlah disewa minimal 1.")
            return
        }
        if (jumlah > selectedItem.stokTersedia) {
            _uiState.value = RentalUiState.Error("Jumlah disewa ($jumlah) melebihi stok yang tersedia (${selectedItem.stokTersedia}).")
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

            val now = System.currentTimeMillis()
            val sdfId = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.getDefault())
            val generatedInvoiceId = "INV-${sdfId.format(Date(now))}"

            val rental = Rental(
                itemId = selectedItem.id,
                namaBarang = selectedItem.nama,
                namaPenyewa = namaPenyewa.trim(),
                noWa = noWa.trim(),
                tanggalMulai = tanggalMulai,
                tanggalKembali = tanggalKembali,
                lamaHari = lamaHari,
                jumlah = jumlah,
                hargaTotal = hargaTotal,
                dp = dp,
                sisaBayar = sisa,
                statusPembayaran = statusBayar,
                status = "berjalan",
                dibuatOleh = _kasirNama.value.ifEmpty { authRepository.currentUser?.email ?: "" },
                dibuatPada = now,
                catatan = catatan.trim(),
                invoiceId = generatedInvoiceId,
                metodePembayaran = metodePembayaran
            )

            val res = rentalRepository.tambahSewa(rental, selectedItem.id)
            res.onSuccess { savedRental ->
                _uiState.value = RentalUiState.Idle
                onSuccess(savedRental)
            }.onFailure {
                _uiState.value = RentalUiState.Error("Gagal menyimpan sewa, periksa koneksi internet.")
            }
        }
    }

    fun lunasiPembayaran(
        rental: Rental,
        metodePembayaran: String,
        onSuccess: (Rental) -> Unit
    ) {
        viewModelScope.launch {
            _uiState.value = RentalUiState.Loading
            val now = System.currentTimeMillis()
            val sisaAmount = rental.sisaBayar

            val updatedRental = rental.copy(
                dp = rental.hargaTotal,
                sisaBayar = 0L,
                statusPembayaran = "lunas",
                metodePembayaran = metodePembayaran,
                pelunasanPada = now,
                jumlahPelunasan = sisaAmount
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
        viewModelScope.launch {
            _uiState.value = RentalUiState.Loading
            val sisa = if (hargaTotal > dp) hargaTotal - dp else 0L
            val statusBayar = if (sisa == 0L) "lunas" else "belum_lunas"

            val updated = rental.copy(
                namaPenyewa = namaPenyewa.trim(),
                noWa = noWa.trim(),
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

            val res = rentalRepository.updateSewa(updated)
            res.onSuccess {
                _uiState.value = RentalUiState.Idle
                onSuccess()
            }.onFailure {
                _uiState.value = RentalUiState.Error("Gagal memperbarui data sewa.")
            }
        }
    }

    fun hapusSewa(rental: Rental, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = RentalUiState.Loading
            val res = rentalRepository.hapusSewa(rental)
            res.onSuccess {
                _uiState.value = RentalUiState.Idle
                onSuccess()
            }.onFailure {
                _uiState.value = RentalUiState.Error("Gagal menghapus sewa.")
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
            val storeProfileDoc = StoreProfileRepository().getStoreProfile().getOrDefault(
                StoreProfileDoc()
            )
            val storeConfigDoc = storeConfigRepository.getStoreConfig().getOrDefault(StoreConfigDoc())

            val template = storeConfigDoc.templatePesanNota.ifEmpty { DefaultStoreConfig.TEMPLATE_NOTA_DEFAULT }
            val tglMulai = formatDate(rental.tanggalMulai)
            val tglKembali = formatDate(rental.tanggalKembali)

            val tokoNama = storeProfileDoc.nama.ifEmpty { userDoc.namaToko.ifEmpty { "MYRENT STORE" } }
            val alamatStr = storeProfileDoc.alamat.ifEmpty { userDoc.alamatToko }
            val telpStr = storeProfileDoc.noTelp.ifEmpty { userDoc.noWaToko }

            val pesan = TemplateReplacer.replaceVariables(
                template = template,
                nama = rental.namaPenyewa,
                barang = "${rental.namaBarang} (${rental.jumlah} unit)",
                tanggal = "$tglMulai s/d $tglKembali (${rental.lamaHari} Hari)",
                total = formatRupiah(rental.hargaTotal),
                dp = formatRupiah(rental.dp),
                sisa = formatRupiah(rental.sisaBayar),
                invoice = rental.invoiceId.ifEmpty { "NOTA-${rental.dibuatPada}" },
                toko = tokoNama,
                catatan = rental.catatan,
                telpToko = telpStr,
                alamatToko = alamatStr
            )

            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$formattedNo?text=${Uri.encode(pesan)}"))
                context.startActivity(intent)
            } catch (e: Exception) {
                _uiState.value = RentalUiState.Error("Gagal membuka WhatsApp. Pastikan WhatsApp terpasang.")
            }
        }
    }

    fun kirimWhatsApp(context: Context, rental: Rental) {
        viewModelScope.launch {
            var formattedNo = rental.noWa.replace("[^0-9]".toRegex(), "")
            if (formattedNo.startsWith("0")) {
                formattedNo = "62" + formattedNo.substring(1)
            }

            val userDoc = authRepository.getUserProfile().getOrDefault(UserDoc())
            val storeConfigDoc = storeConfigRepository.getStoreConfig().getOrDefault(StoreConfigDoc())

            val template = storeConfigDoc.templatePesanH.ifEmpty { DefaultStoreConfig.TEMPLATE_H_DEFAULT }
            val tglStr = formatDate(rental.tanggalKembali)

            val pesan = TemplateReplacer.replaceVariables(
                template = template,
                nama = rental.namaPenyewa,
                barang = "${rental.namaBarang} (${rental.jumlah} unit)",
                tanggal = tglStr,
                total = formatRupiah(rental.hargaTotal),
                dp = formatRupiah(rental.dp),
                sisa = formatRupiah(rental.sisaBayar),
                invoice = rental.invoiceId,
                toko = userDoc.namaToko.ifEmpty { "MYRENT STORE" },
                catatan = rental.catatan
            )

            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$formattedNo?text=${Uri.encode(pesan)}"))
                context.startActivity(intent)
            } catch (e: Exception) {
                _uiState.value = RentalUiState.Error("Gagal membuka WhatsApp. Pastikan WhatsApp terpasang.")
            }
        }
    }

    fun resetUiState() {
        _uiState.value = RentalUiState.Idle
    }
}
