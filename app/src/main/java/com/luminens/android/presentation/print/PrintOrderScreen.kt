package com.luminens.android.presentation.print

import android.annotation.SuppressLint
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.luminens.android.R
import com.luminens.android.data.model.CartItem
import com.luminens.android.data.model.ShippingAddress

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrintOrderScreen(
    onBack: () -> Unit,
    onOrderSuccess: () -> Unit,
    viewModel: CartViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val products = remember(uiState.productCategory) { viewModel.availableProducts() }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { snackbarHostState.showSnackbar(it); viewModel.clearError() }
    }

    LaunchedEffect(uiState.orderSuccess) {
        if (uiState.orderSuccess) onOrderSuccess()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when (uiState.step) {
                            PrintStep.PRODUCT -> stringResource(R.string.print_cart)
                            PrintStep.CROP_PREVIEW -> stringResource(R.string.print_crop_preview)
                            PrintStep.SHIPPING -> stringResource(R.string.print_shipping)
                            PrintStep.QUOTE -> stringResource(R.string.print_quote)
                            PrintStep.PAYMENT -> stringResource(R.string.print_payment)
                            else -> stringResource(R.string.print_title)
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (uiState.step == PrintStep.PRODUCT) onBack()
                        else viewModel.setStep(PrintStep.entries[uiState.step.ordinal - 1])
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        }
    ) { padding ->
        when (uiState.step) {
            PrintStep.PRODUCT -> CartStep(
                cart = uiState.cart,
                products = products,
                selectedCategory = uiState.productCategory,
                modifier = Modifier.fillMaxSize().padding(padding),
                onCategoryChange = viewModel::setProductCategory,
                onAddProduct = viewModel::addProductToCart,
                onQuantityChange = viewModel::updateQuantity,
                onRemoveItem = viewModel::removeItem,
                onProceed = { viewModel.setStep(PrintStep.CROP_PREVIEW) },
            )
            PrintStep.CROP_PREVIEW -> CropPreviewStep(
                cart = uiState.cart,
                modifier = Modifier.fillMaxSize().padding(padding),
                onFitModeChange = viewModel::updateFitMode,
                onProceed = { viewModel.setStep(PrintStep.SHIPPING) },
            )
            PrintStep.SHIPPING -> ShippingStep(
                address = uiState.shippingAddress,
                modifier = Modifier.fillMaxSize().padding(padding),
                onAddressChange = viewModel::updateAddress,
                onProceed = viewModel::proceedToPayment,
                isLoading = uiState.isLoading,
            )
            PrintStep.PAYMENT -> if (!uiState.checkoutUrl.isNullOrBlank()) {
                PaymentWebView(
                    url = uiState.checkoutUrl!!,
                    modifier = Modifier.fillMaxSize().padding(padding),
                    onSuccess = { sessionId -> viewModel.onPaymentSuccess(sessionId) },
                    onCancel = { viewModel.setStep(PrintStep.SHIPPING) },
                )
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            else -> {}
        }
    }
}

@Composable
private fun CartStep(
    cart: List<CartItem>,
    products: List<PrintProduct>,
    selectedCategory: PrintProductCategory,
    modifier: Modifier,
    onCategoryChange: (PrintProductCategory) -> Unit,
    onAddProduct: (PrintProduct) -> Unit,
    onQuantityChange: (String, Int) -> Unit,
    onRemoveItem: (String) -> Unit,
    onProceed: () -> Unit,
) {
    Column(modifier = modifier) {
        ProductPickerSection(
            selectedCategory = selectedCategory,
            products = products,
            onCategoryChange = onCategoryChange,
            onAddProduct = onAddProduct,
        )

        if (cart.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.print_choose_product_hint))
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(cart, key = { it.id }) { item ->
                    CartItemRow(
                        item = item,
                        onQtyIncrease = { onQuantityChange(item.id, item.quantity + 1) },
                        onQtyDecrease = { onQuantityChange(item.id, item.quantity - 1) },
                        onRemove = { onRemoveItem(item.id) },
                    )
                }
            }
            Button(
                onClick = onProceed,
                modifier = Modifier.fillMaxWidth().padding(16.dp).height(52.dp),
                enabled = cart.isNotEmpty(),
            ) { Text(stringResource(R.string.proceed_to_shipping)) }
        }
    }
}

@Composable
private fun ProductPickerSection(
    selectedCategory: PrintProductCategory,
    products: List<PrintProduct>,
    onCategoryChange: (PrintProductCategory) -> Unit,
    onAddProduct: (PrintProduct) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(stringResource(R.string.select_product), style = MaterialTheme.typography.titleMedium)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { onCategoryChange(PrintProductCategory.PHOTO_PRINTS) }) {
                Text(stringResource(R.string.print_category_photo_prints))
            }
            OutlinedButton(onClick = { onCategoryChange(PrintProductCategory.CANVAS) }) {
                Text(stringResource(R.string.print_category_canvas))
            }
            OutlinedButton(onClick = { onCategoryChange(PrintProductCategory.FRAMES) }) {
                Text(stringResource(R.string.print_category_frames))
            }
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 4.dp),
        ) {
            items(products, key = { it.id }) { product ->
                Card(modifier = Modifier.fillParentMaxWidth(0.7f)) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        AsyncImage(
                            model = product.previewUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxWidth().aspectRatio(4f / 5f),
                            contentScale = ContentScale.Crop,
                        )
                        Text(product.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text("EUR ${product.priceEur}", style = MaterialTheme.typography.bodySmall)
                        Button(onClick = { onAddProduct(product) }, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.add_to_cart))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CartItemRow(
    item: CartItem,
    onQtyIncrease: () -> Unit,
    onQtyDecrease: () -> Unit,
    onRemove: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AsyncImage(
            model = item.photoUrl,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            contentScale = ContentScale.Crop,
        )
        Column(Modifier.weight(1f)) {
            Text(item.productName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onQtyDecrease, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Remove, contentDescription = "Remove")
            }
            Text("${item.quantity}", modifier = Modifier.padding(horizontal = 4.dp))
            IconButton(onClick = onQtyIncrease, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        }
    }
}

@Composable
private fun CropPreviewStep(
    cart: List<CartItem>,
    modifier: Modifier,
    onFitModeChange: (itemId: String, fitMode: String) -> Unit,
    onProceed: () -> Unit,
) {
    val firstItem = cart.firstOrNull()
    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.print_crop_preview), style = MaterialTheme.typography.titleMedium)
        if (firstItem == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.cart_empty))
            }
            return
        }

        AsyncImage(
            model = firstItem.photoUrl,
            contentDescription = null,
            modifier = Modifier.fillMaxWidth().aspectRatio(4f / 5f),
            contentScale = if (firstItem.fitMode == "fit") ContentScale.Fit else ContentScale.Crop,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { onFitModeChange(firstItem.id, "fit") }) {
                Text(stringResource(R.string.fit))
            }
            OutlinedButton(onClick = { onFitModeChange(firstItem.id, "fill") }) {
                Text(stringResource(R.string.fill))
            }
        }

        Button(onClick = onProceed, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Text(stringResource(R.string.proceed_to_shipping))
        }
    }
}

@Composable
private fun ShippingStep(
    address: ShippingAddress,
    modifier: Modifier,
    onAddressChange: (ShippingAddress) -> Unit,
    onProceed: () -> Unit,
    isLoading: Boolean,
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(stringResource(R.string.shipping_info), style = MaterialTheme.typography.titleMedium)

        OutlinedTextField(
            value = address.firstName, onValueChange = { onAddressChange(address.copy(firstName = it)) },
            label = { Text(stringResource(R.string.full_name)) }, modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = address.addressLine1, onValueChange = { onAddressChange(address.copy(addressLine1 = it)) },
            label = { Text(stringResource(R.string.address_line1)) }, modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = address.city, onValueChange = { onAddressChange(address.copy(city = it)) },
            label = { Text(stringResource(R.string.city)) }, modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = address.postCode, onValueChange = { onAddressChange(address.copy(postCode = it)) },
            label = { Text(stringResource(R.string.postal_code)) }, modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = address.country, onValueChange = { onAddressChange(address.copy(country = it)) },
            label = { Text(stringResource(R.string.country_code)) }, modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = onProceed,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            enabled = address.firstName.isNotBlank() && address.addressLine1.isNotBlank() &&
                      address.city.isNotBlank() && address.postCode.isNotBlank() && !isLoading,
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Text(stringResource(R.string.proceed_to_payment))
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun PaymentWebView(
    url: String,
    modifier: Modifier,
    onSuccess: (String?) -> Unit,
    onCancel: () -> Unit,
) {
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                        val reqUri = request?.url ?: return false
                        val u = reqUri.toString()
                        val sessionId = reqUri.getQueryParameter("session_id")
                        return when {
                            u.startsWith("luminens://payment/success") -> { onSuccess(sessionId); true }
                            u.startsWith("luminens://payment/cancel") -> { onCancel(); true }
                            u.contains("/order-success") -> { onSuccess(sessionId); true }
                            u.contains("/print-order") -> { onCancel(); true }
                            else -> false
                        }
                    }
                }
                loadUrl(url)
            }
        },
        modifier = modifier,
    )
}
