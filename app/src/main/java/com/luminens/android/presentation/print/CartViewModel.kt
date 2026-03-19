package com.luminens.android.presentation.print

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.luminens.android.data.model.CartItem
import com.luminens.android.data.model.ShipmentMethod
import com.luminens.android.data.model.ShippingAddress
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class PrintStep { PRODUCT, CROP_PREVIEW, SHIPPING, QUOTE, PAYMENT }

data class PrintUiState(
    val step: PrintStep = PrintStep.PRODUCT,
    val cart: List<CartItem> = emptyList(),
    val shippingAddress: ShippingAddress = ShippingAddress(),
    val availableShipments: List<ShipmentMethod> = emptyList(),
    val selectedShipment: ShipmentMethod? = null,
    val isLoading: Boolean = false,
    val checkoutUrl: String? = null,
    val error: String? = null,
    val orderSuccess: Boolean = false,
)

@HiltViewModel
class CartViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(PrintUiState())
    val uiState: StateFlow<PrintUiState> = _uiState.asStateFlow()

    fun addItem(item: CartItem) {
        val existing = _uiState.value.cart.find {
            it.photoUrl == item.photoUrl && it.productId == item.productId
        }
        val newCart = if (existing != null) {
            _uiState.value.cart.map {
                if (it.id == existing.id) it.copy(quantity = it.quantity + 1) else it
            }
        } else {
            _uiState.value.cart + item
        }
        _uiState.value = _uiState.value.copy(cart = newCart)
    }

    fun removeItem(itemId: String) {
        _uiState.value = _uiState.value.copy(
            cart = _uiState.value.cart.filter { it.id != itemId }
        )
    }

    fun updateQuantity(itemId: String, qty: Int) {
        if (qty < 1) { removeItem(itemId); return }
        _uiState.value = _uiState.value.copy(
            cart = _uiState.value.cart.map { if (it.id == itemId) it.copy(quantity = qty) else it }
        )
    }

    fun updateAddress(address: ShippingAddress) {
        _uiState.value = _uiState.value.copy(shippingAddress = address)
    }

    fun selectShipment(method: ShipmentMethod) {
        _uiState.value = _uiState.value.copy(selectedShipment = method)
    }

    fun setStep(step: PrintStep) { _uiState.value = _uiState.value.copy(step = step) }

    fun clearError() { _uiState.value = _uiState.value.copy(error = null) }

    fun onPaymentSuccess() {
        _uiState.value = _uiState.value.copy(
            orderSuccess = true,
            cart = emptyList(),
            checkoutUrl = null,
        )
    }

    fun setCheckoutUrl(url: String) { _uiState.value = _uiState.value.copy(checkoutUrl = url) }

    fun totalItems(): Int = _uiState.value.cart.sumOf { it.quantity }
}
