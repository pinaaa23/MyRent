package com.myrent.app.ui.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.myrent.app.data.items.Item
import com.myrent.app.data.rentals.Rental
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class TopItemStat(
    val namaBarang: String,
    val jumlahSewaCount: Int
)

data class CategoryStat(
    val namaKategori: String,
    val totalOmzet: Long,
    val proportion: Float
)

data class ReportUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val selectedPeriodType: PeriodType = PeriodType.HARI,
    val periodOffset: Int = 0,               // 0 = current
    val periodRange: PeriodRange? = null,
    val omzetKotor: Long = 0L,
    val sudahDiterima: Long = 0L,
    val belumDibayar: Long = 0L,
    val totalTransaksi: Int = 0,
    val chartBars: List<BarChartItem> = emptyList(),
    val selectedBarIndex: Int? = null,
    val topItems: List<TopItemStat> = emptyList(),
    val categoryStats: List<CategoryStat> = emptyList(),
    val recentRentals: List<Rental> = emptyList(),
    val allRentalsInPeriod: List<Rental> = emptyList(),
    val showAllRentalsDialog: Boolean = false
)

/**
 * ViewModel pengelola Data Laporan Omzet, Grafik, Kategori, & Statistik Sewa (Terisolasi per Toko)
 */
class ReportViewModel(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReportUiState(isLoading = true))
    val uiState: StateFlow<ReportUiState> = _uiState.asStateFlow()

    private var cachedItems: List<Item> = emptyList()

    private val currentUid: String
        get() = auth.currentUser?.uid ?: ""

    init {
        initInitialState()
    }

    private fun initInitialState() {
        val periodRange = ReportPeriodHelper.calculatePeriodRange(PeriodType.HARI, 0)
        _uiState.value = _uiState.value.copy(
            selectedPeriodType = PeriodType.HARI,
            periodOffset = 0,
            periodRange = periodRange
        )
        loadReportData()
    }

    fun loadReportData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            val uid = currentUid
            if (uid.isEmpty()) {
                _uiState.value = _uiState.value.copy(isLoading = false)
                return@launch
            }

            val state = _uiState.value
            val periodType = state.selectedPeriodType
            val offset = state.periodOffset

            val range = ReportPeriodHelper.calculatePeriodRange(periodType, offset)

            try {
                val chartRange = if (periodType == PeriodType.HARI) {
                    val bars = ReportPeriodHelper.generateChartBars(periodType, range)
                    val firstBarStart = bars.firstOrNull()?.startMs ?: range.startMs
                    Pair(firstBarStart, range.endMs)
                } else {
                    Pair(range.startMs, range.endMs)
                }

                val rentalsRef = firestore.collection("toko").document(uid).collection("rentals")
                val querySnapshot = rentalsRef
                    .whereGreaterThanOrEqualTo("dibuatPada", chartRange.first)
                    .whereLessThanOrEqualTo("dibuatPada", chartRange.second)
                    .get()
                    .await()

                val rawRentals = querySnapshot.documents.map { doc ->
                    Rental(
                        id = doc.id,
                        cabangId = doc.getString("cabangId") ?: "",
                        itemId = doc.getString("itemId") ?: "",
                        namaBarang = doc.getString("namaBarang") ?: "",
                        customerId = doc.getString("customerId") ?: "",
                        namaPenyewa = doc.getString("namaPenyewa") ?: "",
                        noWa = doc.getString("noWa") ?: "",
                        tanggalMulai = doc.getLong("tanggalMulai") ?: 0L,
                        tanggalKembali = doc.getLong("tanggalKembali") ?: 0L,
                        lamaHari = (doc.getLong("lamaHari") ?: 1L).toInt(),
                        jumlah = (doc.getLong("jumlah") ?: 1L).toInt(),
                        hargaTotal = doc.getLong("hargaTotal") ?: 0L,
                        dp = doc.getLong("dp") ?: 0L,
                        sisaBayar = doc.getLong("sisaBayar") ?: 0L,
                        statusPembayaran = doc.getString("statusPembayaran") ?: "lunas",
                        status = doc.getString("status") ?: "berjalan",
                        dibuatPada = doc.getLong("dibuatPada") ?: 0L,
                        invoiceId = doc.getString("invoiceId") ?: "",
                        metodePembayaran = doc.getString("metodePembayaran") ?: "Tunai"
                    )
                }

                val itemsSnapshot = firestore.collection("toko").document(uid).collection("items").get().await()
                cachedItems = itemsSnapshot.documents.map { doc ->
                    Item(
                        id = doc.id,
                        cabangId = doc.getString("cabangId") ?: "",
                        nama = doc.getString("nama") ?: "",
                        kategori = doc.getString("kategori") ?: ""
                    )
                }

                val rentalsInPeriod = rawRentals.filter { it.dibuatPada in range.startMs..range.endMs }

                val omzetKotor = rentalsInPeriod.sumOf { it.hargaTotal }
                val sudahDiterima = rentalsInPeriod.sumOf { it.hargaTotal - it.sisaBayar }
                val belumDibayar = rentalsInPeriod.sumOf { it.sisaBayar }
                val totalTransaksi = rentalsInPeriod.size

                val initialBars = ReportPeriodHelper.generateChartBars(periodType, range)
                val populatedBars = initialBars.map { bar ->
                    val barRentals = rawRentals.filter { it.dibuatPada in bar.startMs..bar.endMs }
                    bar.copy(value = barRentals.sumOf { it.hargaTotal })
                }

                val topItems = rentalsInPeriod
                    .groupBy { it.namaBarang }
                    .map { (nama, list) ->
                        TopItemStat(
                            namaBarang = nama,
                            jumlahSewaCount = list.sumOf { it.jumlah }
                        )
                    }
                    .sortedByDescending { it.jumlahSewaCount }
                    .take(5)

                val itemCategoryMap = cachedItems.associate { it.id to it.kategori }
                val categoryStats = rentalsInPeriod
                    .groupBy { rental ->
                        val kat = itemCategoryMap[rental.itemId]
                        if (!kat.isNullOrEmpty()) kat else "Tanpa kategori"
                    }
                    .map { (kat, list) ->
                        val catTotal = list.sumOf { it.hargaTotal }
                        val prop = if (omzetKotor > 0) catTotal.toFloat() / omzetKotor.toFloat() else 0f
                        CategoryStat(
                            namaKategori = kat,
                            totalOmzet = catTotal,
                            proportion = prop
                        )
                    }
                    .sortedByDescending { it.totalOmzet }

                val recent10 = rentalsInPeriod.sortedByDescending { it.dibuatPada }.take(10)

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = null,
                    periodRange = range,
                    omzetKotor = omzetKotor,
                    sudahDiterima = sudahDiterima,
                    belumDibayar = belumDibayar,
                    totalTransaksi = totalTransaksi,
                    chartBars = populatedBars,
                    selectedBarIndex = if (populatedBars.isNotEmpty()) populatedBars.lastIndex else null,
                    topItems = topItems,
                    categoryStats = categoryStats,
                    recentRentals = recent10,
                    allRentalsInPeriod = rentalsInPeriod.sortedByDescending { it.dibuatPada }
                )

            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Gagal memuat laporan, periksa koneksi internet."
                )
            }
        }
    }

    fun selectPeriodType(periodType: PeriodType) {
        val range = ReportPeriodHelper.calculatePeriodRange(periodType, 0)
        _uiState.value = _uiState.value.copy(
            selectedPeriodType = periodType,
            periodOffset = 0,
            periodRange = range
        )
        loadReportData()
    }

    fun navigatePeriodOffset(delta: Int) {
        val newOffset = _uiState.value.periodOffset + delta
        if (newOffset > 0) return

        val periodType = _uiState.value.selectedPeriodType
        val range = ReportPeriodHelper.calculatePeriodRange(periodType, newOffset)

        _uiState.value = _uiState.value.copy(
            periodOffset = newOffset,
            periodRange = range
        )
        loadReportData()
    }

    fun resetToCurrentPeriod() {
        val periodType = _uiState.value.selectedPeriodType
        val range = ReportPeriodHelper.calculatePeriodRange(periodType, 0)

        _uiState.value = _uiState.value.copy(
            periodOffset = 0,
            periodRange = range
        )
        loadReportData()
    }

    fun selectBar(index: Int) {
        _uiState.value = _uiState.value.copy(selectedBarIndex = index)
    }

    fun setShowAllRentalsDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showAllRentalsDialog = show)
    }
}
