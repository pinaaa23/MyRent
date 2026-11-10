package com.myrent.app.ui.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myrent.app.data.items.Item
import com.myrent.app.data.items.ItemRepository
import com.myrent.app.data.rentals.Rental
import com.myrent.app.data.rentals.RentalRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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
    val periodOffset: Int = 0,
    val selectedMethodFilter: String = "Semua", // "Semua", "TUNAI", "NON_TUNAI"
    val periodRange: PeriodRange? = null,
    val omzetKotor: Long = 0L,
    val sudahDiterima: Long = 0L,
    val sudahDiterimaTunai: Long = 0L,
    val sudahDiterimaNonTunai: Long = 0L,
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
 * ViewModel pengelola Data Laporan Omzet Realtime dengan Filter Metode (Tunai / Non-Tunai)
 */
class ReportViewModel(
    private val rentalRepository: RentalRepository = RentalRepository(),
    private val itemRepository: ItemRepository = ItemRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReportUiState(isLoading = true))
    val uiState: StateFlow<ReportUiState> = _uiState.asStateFlow()

    private var allRawRentals: List<Rental> = emptyList()
    private var cachedItems: List<Item> = emptyList()

    init {
        initInitialState()
        observeDataRealtime()
    }

    private fun initInitialState() {
        val periodRange = ReportPeriodHelper.calculatePeriodRange(PeriodType.HARI, 0)
        _uiState.value = _uiState.value.copy(
            selectedPeriodType = PeriodType.HARI,
            periodOffset = 0,
            selectedMethodFilter = "Semua",
            periodRange = periodRange
        )
    }

    private fun observeDataRealtime() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            launch {
                itemRepository.getItemsFlow().collect { itemList ->
                    cachedItems = itemList
                    recalculateReportData()
                }
            }

            launch {
                rentalRepository.getRentalsFlow().collect { rentalList ->
                    allRawRentals = rentalList
                    recalculateReportData()
                }
            }
        }
    }

    fun loadReportData() {
        recalculateReportData()
    }

    private fun recalculateReportData() {
        val state = _uiState.value
        val periodType = state.selectedPeriodType
        val offset = state.periodOffset
        val methodFilter = state.selectedMethodFilter

        val range = ReportPeriodHelper.calculatePeriodRange(periodType, offset)

        val rentalsInPeriod = allRawRentals.filter { it.dibuatPada in range.startMs..range.endMs }

        // Omzet Kotor: Total biaya sewa transaksi baru dalam periode
        val omzetKotor = rentalsInPeriod.sumOf { it.hargaTotal }
        val belumDibayar = rentalsInPeriod.sumOf { it.sisaBayar }
        val totalTransaksi = rentalsInPeriod.size

        // Hitung Diterima dari catatan pembayaran berdasarkan TANGGAL UANG MASUK riil
        val allPaymentsInPeriod = allRawRentals.flatMap { it.getAllPaymentRecords() }
            .filter { it.tanggal in range.startMs..range.endMs }

        val diterimaTunai = allPaymentsInPeriod.filter { it.jenisMetode == "TUNAI" }.sumOf { it.jumlah }
        val diterimaNonTunai = allPaymentsInPeriod.filter { it.jenisMetode == "NON_TUNAI" }.sumOf { it.jumlah }
        val totalDiterima = allPaymentsInPeriod.sumOf { it.jumlah }

        val filteredDiterima = when (methodFilter) {
            "TUNAI" -> diterimaTunai
            "NON_TUNAI" -> diterimaNonTunai
            else -> totalDiterima
        }

        val initialBars = ReportPeriodHelper.generateChartBars(periodType, range)
        val populatedBars = initialBars.map { bar ->
            val barPayments = allRawRentals.flatMap { it.getAllPaymentRecords() }
                .filter { it.tanggal in bar.startMs..bar.endMs }
            val barAmount = when (methodFilter) {
                "TUNAI" -> barPayments.filter { it.jenisMetode == "TUNAI" }.sumOf { it.jumlah }
                "NON_TUNAI" -> barPayments.filter { it.jenisMetode == "NON_TUNAI" }.sumOf { it.jumlah }
                else -> barPayments.sumOf { it.jumlah }
            }
            bar.copy(value = barAmount)
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
            sudahDiterima = filteredDiterima,
            sudahDiterimaTunai = diterimaTunai,
            sudahDiterimaNonTunai = diterimaNonTunai,
            belumDibayar = belumDibayar,
            totalTransaksi = totalTransaksi,
            chartBars = populatedBars,
            selectedBarIndex = if (populatedBars.isNotEmpty()) populatedBars.lastIndex else null,
            topItems = topItems,
            categoryStats = categoryStats,
            recentRentals = recent10,
            allRentalsInPeriod = rentalsInPeriod.sortedByDescending { it.dibuatPada }
        )
    }

    fun selectMethodFilter(filter: String) {
        _uiState.value = _uiState.value.copy(selectedMethodFilter = filter)
        recalculateReportData()
    }

    fun selectPeriodType(periodType: PeriodType) {
        val range = ReportPeriodHelper.calculatePeriodRange(periodType, 0)
        _uiState.value = _uiState.value.copy(
            selectedPeriodType = periodType,
            periodOffset = 0,
            periodRange = range
        )
        recalculateReportData()
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
        recalculateReportData()
    }

    fun resetToCurrentPeriod() {
        val periodType = _uiState.value.selectedPeriodType
        val range = ReportPeriodHelper.calculatePeriodRange(periodType, 0)

        _uiState.value = _uiState.value.copy(
            periodOffset = 0,
            periodRange = range
        )
        recalculateReportData()
    }

    fun selectBar(index: Int) {
        _uiState.value = _uiState.value.copy(selectedBarIndex = index)
    }

    fun setShowAllRentalsDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showAllRentalsDialog = show)
    }
}
