package com.myrent.app.ui.beranda

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.myrent.app.data.auth.AuthRepository
import com.myrent.app.data.auth.UserDoc
import com.myrent.app.data.rentals.Rental
import com.myrent.app.data.rentals.RentalRepository
import com.myrent.app.data.settings.DefaultStoreConfig
import com.myrent.app.data.settings.StoreConfigDoc
import com.myrent.app.data.settings.StoreConfigRepository
import com.myrent.app.data.settings.TemplateReplacer
import com.myrent.app.ui.screens.formatDate
import com.myrent.app.ui.screens.formatRupiah
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Calendar

data class FollowUpItem(
    val rental: Rental,
    val type: String,          // "TELAT", "Hari H", "H-1"
    val overdueDays: Int = 0,   // Selisih hari terlambat jika type == "TELAT"
    val isFollowedUp: Boolean
)

/**
 * ViewModel pengelola layar Beranda & Follow-Up WhatsApp
 */
class HomeViewModel(
    private val authRepository: AuthRepository = AuthRepository(),
    private val rentalRepository: RentalRepository = RentalRepository(),
    private val storeConfigRepository: StoreConfigRepository = StoreConfigRepository(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : ViewModel() {

    private val _userDocState = MutableStateFlow(UserDoc())
    val userDocState: StateFlow<UserDoc> = _userDocState.asStateFlow()

    private val _userNamaToko = MutableStateFlow("")
    val userNamaToko: StateFlow<String> = _userNamaToko.asStateFlow()

    private val _allRentals = MutableStateFlow<List<Rental>>(emptyList())

    val sewaBerjalanCount: StateFlow<Int> = _allRentals.map { rentals ->
        rentals.filter { it.status == "berjalan" }.size
    }.stateIn(viewModelScope, SharingStarted.Lazily, 0)

    val jatuhTempoHariIniCount: StateFlow<Int> = _allRentals.map { rentals ->
        rentals.filter { it.status == "berjalan" && isSameDay(it.tanggalKembali, System.currentTimeMillis()) }.size
    }.stateIn(viewModelScope, SharingStarted.Lazily, 0)

    val omzetHariIni: StateFlow<Long> = _allRentals.map { rentals ->
        val todayMs = System.currentTimeMillis()
        var uangMasukRiil = 0L

        rentals.forEach { rental ->
            if (isSameDay(rental.dibuatPada, todayMs)) {
                val dpAwal = if (rental.pelunasanPada != null && isSameDay(rental.pelunasanPada, todayMs)) {
                    rental.hargaTotal - rental.jumlahPelunasan
                } else {
                    rental.dp
                }
                uangMasukRiil += dpAwal
            }

            if (rental.pelunasanPada != null && isSameDay(rental.pelunasanPada, todayMs)) {
                uangMasukRiil += rental.jumlahPelunasan
            }
        }
        uangMasukRiil
    }.stateIn(viewModelScope, SharingStarted.Lazily, 0L)

    val followUpList: StateFlow<List<FollowUpItem>> = _allRentals.map { rentals ->
        val list = mutableListOf<FollowUpItem>()
        val todayMs = System.currentTimeMillis()

        val calToday = Calendar.getInstance().apply {
            timeInMillis = todayMs
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val todayStartMs = calToday.timeInMillis

        val calTomorrow = Calendar.getInstance().apply {
            timeInMillis = todayMs
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val tomorrowStartMs = calTomorrow.timeInMillis

        rentals.filter { it.status == "berjalan" }.forEach { rental ->
            val calReturn = Calendar.getInstance().apply {
                timeInMillis = rental.tanggalKembali
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val returnStartMs = calReturn.timeInMillis

            if (returnStartMs < todayStartMs) {
                val daysOverdue = ((todayStartMs - returnStartMs) / (24 * 60 * 60 * 1000L)).toInt().coerceAtLeast(1)
                val isFollowedUp = rental.sudahFollowUpH
                list.add(FollowUpItem(rental, "TELAT", daysOverdue, isFollowedUp))
            } else if (returnStartMs == todayStartMs) {
                list.add(FollowUpItem(rental, "Hari H", 0, rental.sudahFollowUpH))
            } else if (returnStartMs == tomorrowStartMs) {
                list.add(FollowUpItem(rental, "H-1", 0, rental.sudahFollowUpH1))
            }
        }

        list.sortedWith(
            compareBy<FollowUpItem> { it.isFollowedUp }
                .thenBy { item ->
                    when (item.type) {
                        "TELAT" -> 0
                        "Hari H" -> 1
                        else -> 2
                    }
                }
                .thenByDescending { it.overdueDays }
        )
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            launch {
                authRepository.getUserProfileFlow().collect { userDoc ->
                    _userDocState.value = userDoc
                    _userNamaToko.value = userDoc.namaToko.ifEmpty { "Kusuma Rent" }
                }
            }

            launch {
                rentalRepository.getRentalsFlow().collect { rentals ->
                    _allRentals.value = rentals
                }
            }
        }
    }

    fun kirimWaFollowUp(context: Context, item: FollowUpItem) {
        val rental = item.rental
        var formattedNo = rental.noWa.replace("[^0-9]".toRegex(), "")
        if (formattedNo.startsWith("0")) {
            formattedNo = "62" + formattedNo.substring(1)
        }

        viewModelScope.launch {
            val userDoc = authRepository.getUserProfile().getOrDefault(UserDoc())
            val storeConfig = storeConfigRepository.getStoreConfig().getOrDefault(StoreConfigDoc())

            val rawTemplate = when (item.type) {
                "TELAT" -> storeConfig.templatePesanTelat.ifEmpty { DefaultStoreConfig.TEMPLATE_TELAT_DEFAULT }
                "H-1" -> storeConfig.templatePesanH1.ifEmpty { DefaultStoreConfig.TEMPLATE_H1_DEFAULT }
                else -> storeConfig.templatePesanH.ifEmpty { DefaultStoreConfig.TEMPLATE_H_DEFAULT }
            }

            val tglStr = formatDate(rental.tanggalKembali)
            val pesan = TemplateReplacer.replaceVariables(
                template = rawTemplate,
                nama = rental.namaPenyewa,
                barang = rental.getSummaryBarangWA(),
                tanggal = tglStr,
                total = formatRupiah(rental.hargaTotal),
                dp = formatRupiah(rental.dp),
                sisa = formatRupiah(rental.sisaBayar),
                invoice = rental.invoiceId,
                toko = userDoc.namaToko.ifEmpty { "Kusuma Rent" },
                telat = item.overdueDays.toString()
            )

            val fieldToUpdate = if (item.type == "H-1") "sudahFollowUpH1" else "sudahFollowUpH"
            val uid = authRepository.currentUser?.uid ?: ""
            if (uid.isNotEmpty()) {
                firestore.collection("toko").document(uid).collection("rentals").document(rental.id).update(fieldToUpdate, true).await()
            }

            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$formattedNo?text=${Uri.encode(pesan)}"))
                context.startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun isSameDay(ts1: Long, ts2: Long): Boolean {
        val cal1 = Calendar.getInstance().apply { timeInMillis = ts1 }
        val cal2 = Calendar.getInstance().apply { timeInMillis = ts2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }
}
