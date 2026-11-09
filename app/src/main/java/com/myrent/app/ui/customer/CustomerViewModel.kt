package com.myrent.app.ui.customer

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myrent.app.data.customer.Customer
import com.myrent.app.data.customer.CustomerRepository
import com.myrent.app.data.customer.normalizeNoWa
import com.myrent.app.data.rentals.Rental
import com.myrent.app.data.rentals.RentalRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class CustomerUiState {
    object Idle : CustomerUiState()
    object Loading : CustomerUiState()
    data class Success(val message: String? = null) : CustomerUiState()
    data class Error(val message: String) : CustomerUiState()
}

data class CustomerItemUi(
    val customer: Customer,
    val jumlahSewa: Int,
    val terakhirSewaMs: Long,
    val riwayatSewa: List<Rental>
)

/**
 * ViewModel pengelola Halaman Data Penyewa (Tanpa Filter Cabang)
 */
class CustomerViewModel(
    private val customerRepository: CustomerRepository = CustomerRepository(),
    private val rentalRepository: RentalRepository = RentalRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<CustomerUiState>(CustomerUiState.Idle)
    val uiState: StateFlow<CustomerUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _allCustomers = MutableStateFlow<List<Customer>>(emptyList())
    private val _allRentals = MutableStateFlow<List<Rental>>(emptyList())

    val filteredCustomerList: StateFlow<List<CustomerItemUi>> = combine(
        _allCustomers,
        _allRentals,
        _searchQuery
    ) { customers, rentals, query ->
        val uiList = customers.map { cust ->
            val custNoWaNorm = normalizeNoWa(cust.noWa)
            val custRentals = rentals.filter { r ->
                r.customerId == cust.id || normalizeNoWa(r.noWa) == custNoWaNorm
            }.sortedByDescending { it.dibuatPada }

            val lastSewaMs = custRentals.firstOrNull()?.dibuatPada ?: cust.diubahPada

            CustomerItemUi(
                customer = cust,
                jumlahSewa = custRentals.size,
                terakhirSewaMs = lastSewaMs,
                riwayatSewa = custRentals
            )
        }.sortedByDescending { it.terakhirSewaMs }

        if (query.trim().isEmpty()) {
            uiList
        } else {
            val q = query.trim().lowercase()
            uiList.filter {
                it.customer.nama.lowercase().contains(q) || it.customer.noWa.contains(q)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            launch {
                rentalRepository.getRentalsFlow().collect { rentals ->
                    _allRentals.value = rentals
                    customerRepository.migrateCustomersFromRentalsIfEmpty(rentals)
                }
            }
            launch {
                customerRepository.getCustomersFlow().collect { customers ->
                    _allCustomers.value = customers
                }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun editCustomer(
        customer: Customer,
        namaBaru: String,
        noWaBaruRaw: String,
        onSuccess: () -> Unit
    ) {
        if (namaBaru.trim().isEmpty()) {
            _uiState.value = CustomerUiState.Error("Nama penyewa tidak boleh kosong.")
            return
        }
        val normNoWa = normalizeNoWa(noWaBaruRaw)
        if (normNoWa.length !in 10..14) {
            _uiState.value = CustomerUiState.Error("Nomor WA harus 10 sampai 14 digit.")
            return
        }

        viewModelScope.launch {
            _uiState.value = CustomerUiState.Loading
            val res = customerRepository.updateCustomer(customer, namaBaru, noWaBaruRaw)
            res.onSuccess {
                _uiState.value = CustomerUiState.Idle
                onSuccess()
            }.onFailure { e ->
                _uiState.value = CustomerUiState.Error(e.message ?: "Gagal memperbarui data penyewa.")
            }
        }
    }

    fun chatWhatsApp(context: Context, noWa: String) {
        var formattedNo = normalizeNoWa(noWa)
        if (formattedNo.startsWith("0")) {
            formattedNo = "62" + formattedNo.substring(1)
        }
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$formattedNo"))
            context.startActivity(intent)
        } catch (e: Exception) {
            _uiState.value = CustomerUiState.Error("Gagal membuka WhatsApp. Pastikan WhatsApp terpasang.")
        }
    }

    fun resetUiState() {
        _uiState.value = CustomerUiState.Idle
    }
}
