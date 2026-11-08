package com.myrent.app.ui.beranda

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.myrent.app.data.auth.AuthRepository
import com.myrent.app.data.cabang.CabangRepository
import com.myrent.app.data.rentals.Rental
import com.myrent.app.data.rentals.RentalRepository
import com.myrent.app.ui.screens.formatDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Calendar

data class FollowUpItem(
    val rental: Rental,
    val type: String, // "H-1" atau "Hari H"
    val isFollowedUp: Boolean
)

/**
 * ViewModel pengelola layar Beranda & Follow-Up WhatsApp
 */
class HomeViewModel(
    private val authRepository: AuthRepository = AuthRepository(),
    private val cabangRepository: CabangRepository = CabangRepository(),
    private val rentalRepository: RentalRepository = RentalRepository(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : ViewModel() {

    private val _userCabangNama = MutableStateFlow("")
    val userCabangNama: StateFlow<String> = _userCabangNama.asStateFlow()

    private val _userCabangId = MutableStateFlow("")

    private val _allRentals = MutableStateFlow<List<Rental>>(emptyList())

    // Metric 1: Jumlah Sewa Berjalan
    val sewaBerjalanCount: StateFlow<Int> = combine(_allRentals) { rentals ->
        rentals[0].filter { it.status == "berjalan" }.size
    }.stateIn(viewModelScope, SharingStarted.Lazily, 0)

    // Metric 2: Jatuh Tempo Hari Ini
    val jatuhTempoHariIniCount: StateFlow<Int> = combine(_allRentals) { rentals ->
        rentals[0].filter { it.status == "berjalan" && isSameDay(it.tanggalKembali, System.currentTimeMillis()) }.size
    }.stateIn(viewModelScope, SharingStarted.Lazily, 0)

    // Metric 3: Omzet Hari Ini (Uang Masuk Riil Hari Ini: DP Sewa Hari Ini + Pelunasan Hari Ini)
    val omzetHariIni: StateFlow<Long> = combine(_allRentals) { rentals ->
        val todayMs = System.currentTimeMillis()
        var uangMasukRiil = 0L

        rentals[0].forEach { rental ->
            // 1. Uang DP / Lunas awal dari sewa yang dibuat hari ini
            if (isSameDay(rental.dibuatPada, todayMs)) {
                val dpAwal = if (rental.pelunasanPada != null && isSameDay(rental.pelunasanPada, todayMs)) {
                    rental.hargaTotal - rental.jumlahPelunasan
                } else {
                    rental.dp
                }
                uangMasukRiil += dpAwal
            }

            // 2. Uang Pelunasan sisa bayar yang dilakukan hari ini
            if (rental.pelunasanPada != null && isSameDay(rental.pelunasanPada, todayMs)) {
                uangMasukRiil += rental.jumlahPelunasan
            }
        }
        uangMasukRiil
    }.stateIn(viewModelScope, SharingStarted.Lazily, 0L)

    // Daftar Perlu di-Follow Up (H-1 & Hari H)
    val followUpList: StateFlow<List<FollowUpItem>> = combine(_allRentals) { rentals ->
        val list = mutableListOf<FollowUpItem>()
        val todayMs = System.currentTimeMillis()
        val tomorrowMs = todayMs + (24 * 60 * 60 * 1000L)

        rentals[0].filter { it.status == "berjalan" }.forEach { rental ->
            if (isSameDay(rental.tanggalKembali, todayMs)) {
                list.add(FollowUpItem(rental, "Hari H", rental.sudahFollowUpH))
            } else if (isSameDay(rental.tanggalKembali, tomorrowMs)) {
                list.add(FollowUpItem(rental, "H-1", rental.sudahFollowUpH1))
            }
        }
        list.sortedBy { it.isFollowedUp }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            val profileRes = authRepository.getUserProfile()
            profileRes.onSuccess { userDoc ->
                val cabangId = userDoc.cabangId
                _userCabangId.value = cabangId
                if (cabangId.isNotEmpty()) {
                    val cabangRes = cabangRepository.getCabangById(cabangId)
                    cabangRes.onSuccess { c -> _userCabangNama.value = c.nama }

                    rentalRepository.getRentalsFlow(cabangId).collect { rentals ->
                        _allRentals.value = rentals
                    }
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

        val tglStr = formatDate(rental.tanggalKembali)
        val pesan = if (item.type == "H-1") {
            "Halo ${rental.namaPenyewa}, kami mengingatkan bahwa ${rental.namaBarang} dijadwalkan dikembalikan besok, $tglStr. Terima kasih."
        } else {
            "Halo ${rental.namaPenyewa}, hari ini jadwal pengembalian ${rental.namaBarang}. Mohon dikembalikan hari ini ya. Terima kasih."
        }

        viewModelScope.launch {
            val fieldToUpdate = if (item.type == "H-1") "sudahFollowUpH1" else "sudahFollowUpH"
            firestore.collection("rentals").document(rental.id).update(fieldToUpdate, true).await()
        }

        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$formattedNo?text=${Uri.encode(pesan)}"))
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun isSameDay(ts1: Long, ts2: Long): Boolean {
        val cal1 = Calendar.getInstance().apply { timeInMillis = ts1 }
        val cal2 = Calendar.getInstance().apply { timeInMillis = ts2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }
}
