package dev.diegoflassa.bipsale.feature.products

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.diegoflassa.bipsale.core.qrcode.LabelData
import dev.diegoflassa.bipsale.core.qrcode.QrGenerator
import dev.diegoflassa.bipsale.core.qrcode.QrLabelSheetRenderer
import dev.diegoflassa.bipsale.core.ui.theme.BipSaleTheme
import dev.diegoflassa.bipsale.core.ui.util.UiText
import dev.diegoflassa.bipsale.feature.products.components.ProductItem
import dev.diegoflassa.bipsale.feature.products.print.QrLabelPrinter

@Composable
fun ProductListScreen(
    onAddProduct: () -> Unit,
    onEditProduct: (String) -> Unit,
    viewModel: ProductViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val printer = remember { QrLabelPrinter(QrLabelSheetRenderer(QrGenerator())) }

    LaunchedEffect(viewModel, context) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is ProductContract.Effect.ShowSnackbar ->
                    snackbarHostState.showSnackbar(effect.message.asString(context))

                is ProductContract.Effect.PrintLabels ->
                    printer.print(
                        context = context,
                        documentName = context.getString(R.string.products_qr_labels_title),
                        labels = effect.labels
                    )

                is ProductContract.Effect.NavigationBack -> Unit
            }
        }
    }

    ProductListContent(
        state = uiState,
        snackbarHostState = snackbarHostState,
        onAddProduct = onAddProduct,
        onEditProduct = onEditProduct,
        onIntent = viewModel::onIntent
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ProductListContent(
    state: ProductContract.State,
    snackbarHostState: SnackbarHostState,
    onAddProduct: () -> Unit,
    onEditProduct: (String) -> Unit,
    onIntent: (ProductContract.Intent) -> Unit,
    modifier: Modifier = Modifier
) {
    val hasSelection = state.selectedProductCodes.isNotEmpty()

    Scaffold(
        modifier = modifier.testTag(ProductListScreenTestTags.ROOT),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            ProductListTopBar(
                state = state,
                hasSelection = hasSelection,
                onIntent = onIntent
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddProduct,
                modifier = Modifier.testTag(ProductListScreenTestTags.ADD_BUTTON)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = stringResource(R.string.products_add_product)
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                state.isLoading -> CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .testTag(ProductListScreenTestTags.LOADING)
                )

                state.errorMessage != null -> ProductListMessage(
                    text = state.errorMessage.asString(),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .testTag(ProductListScreenTestTags.ERROR)
                )

                state.products.isEmpty() -> ProductListMessage(
                    text = stringResource(R.string.products_empty_state),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .testTag(ProductListScreenTestTags.EMPTY)
                )

                else -> ProductRows(
                    state = state,
                    hasSelection = hasSelection,
                    onEditProduct = onEditProduct,
                    onIntent = onIntent
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProductListTopBar(
    state: ProductContract.State,
    hasSelection: Boolean,
    onIntent: (ProductContract.Intent) -> Unit
) {
    TopAppBar(
        title = {
            Text(
                if (hasSelection) {
                    stringResource(
                        R.string.products_selected_count,
                        state.selectedProductCodes.size
                    )
                } else {
                    stringResource(R.string.products_manage_title)
                }
            )
        },
        navigationIcon = {
            if (hasSelection) {
                IconButton(
                    onClick = { onIntent(ProductContract.Intent.ClearSelection) },
                    modifier = Modifier
                        .testTag(ProductListScreenTestTags.CLEAR_SELECTION_BUTTON)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = stringResource(R.string.products_clear_selection)
                    )
                }
            }
        },
        actions = {
            when {
                hasSelection -> IconButton(
                    onClick = { onIntent(ProductContract.Intent.PrintSelectedQrCodes) },
                    modifier = Modifier
                        .testTag(ProductListScreenTestTags.PRINT_SELECTED_BUTTON)
                ) {
                    Icon(
                        Icons.Default.Print,
                        contentDescription = stringResource(R.string.products_print_selected)
                    )
                }

                state.products.isNotEmpty() -> IconButton(
                    onClick = { onIntent(ProductContract.Intent.PrintAllQrCodes) },
                    modifier = Modifier.testTag(ProductListScreenTestTags.PRINT_ALL_BUTTON)
                ) {
                    Icon(
                        Icons.Default.Print,
                        contentDescription = stringResource(R.string.products_print_all_qr_codes)
                    )
                }
            }
        }
    )
}

@Composable
private fun ProductRows(
    state: ProductContract.State,
    hasSelection: Boolean,
    onEditProduct: (String) -> Unit,
    onIntent: (ProductContract.Intent) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag(ProductListScreenTestTags.LIST),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(state.products, key = { it.code }) { product ->
            ProductItem(
                product = product,
                isSelected = product.code in state.selectedProductCodes,
                onClick = {
                    if (hasSelection) {
                        onIntent(ProductContract.Intent.ToggleProductSelection(product.code))
                    } else {
                        onEditProduct(product.code)
                    }
                },
                onLongClick = {
                    onIntent(ProductContract.Intent.ToggleProductSelection(product.code))
                },
                onDelete = { onIntent(ProductContract.Intent.DeleteProduct(product.code)) }
            )
        }
    }
}

@Composable
private fun ProductListMessage(text: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Inventory2,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// region Previews

private fun previewProduct(
    code: String,
    name: String,
    price: String,
    imagePath: String? = null
) = ProductContract.ProductUiModel(
    code = code,
    name = name,
    priceFormatted = price,
    imagePath = imagePath,
    label = LabelData("bipsale://product?code=$code", name, price)
)

private val previewProducts = listOf(
    previewProduct("7891000100103", "Café Premium 200ml", "R$ 12,50"),
    previewProduct("CT-A-RoS", "Coturno cano alto rosa", "R$ 130,00"),
    previewProduct(
        "7891000100202",
        "Padaria e Confeitaria Gourmet do Centro - Combo Especial",
        "R$ 45,90"
    )
)

@Preview(
    name = "ProductListContent · Carregando · Phone",
    showBackground = true,
    locale = "pt",
    device = "spec:width=1080px,height=2520px,dpi=420"
)
@Preview(
    name = "ProductListContent · Carregando · Tablet",
    showBackground = true,
    locale = "pt",
    device = "spec:width=1200px,height=2000px,dpi=240"
)
@Composable
private fun ProductListContentLoadingPreview() {
    BipSaleTheme {
        ProductListContent(
            state = ProductContract.State(isLoading = true),
            snackbarHostState = SnackbarHostState(),
            onAddProduct = {},
            onEditProduct = {},
            onIntent = {}
        )
    }
}

@Preview(
    name = "ProductListContent · Vazio · Phone",
    showBackground = true,
    locale = "pt",
    device = "spec:width=1080px,height=2520px,dpi=420"
)
@Preview(
    name = "ProductListContent · Vazio · Tablet",
    showBackground = true,
    locale = "pt",
    device = "spec:width=1200px,height=2000px,dpi=240"
)
@Composable
private fun ProductListContentEmptyPreview() {
    BipSaleTheme {
        ProductListContent(
            state = ProductContract.State(isLoading = false),
            snackbarHostState = SnackbarHostState(),
            onAddProduct = {},
            onEditProduct = {},
            onIntent = {}
        )
    }
}

@Preview(
    name = "ProductListContent · Erro · Phone",
    showBackground = true,
    locale = "pt",
    device = "spec:width=1080px,height=2520px,dpi=420"
)
@Preview(
    name = "ProductListContent · Erro · Tablet",
    showBackground = true,
    locale = "pt",
    device = "spec:width=1200px,height=2000px,dpi=240"
)
@Composable
private fun ProductListContentErrorPreview() {
    BipSaleTheme {
        ProductListContent(
            state = ProductContract.State(
                isLoading = false,
                errorMessage = UiText.DynamicString("Falha ao carregar produtos")
            ),
            snackbarHostState = SnackbarHostState(),
            onAddProduct = {},
            onEditProduct = {},
            onIntent = {}
        )
    }
}

@Preview(
    name = "ProductListContent · Conteudo · Phone",
    showBackground = true,
    locale = "pt",
    device = "spec:width=1080px,height=2520px,dpi=420"
)
@Preview(
    name = "ProductListContent · Conteudo · Tablet",
    showBackground = true,
    locale = "pt",
    device = "spec:width=1200px,height=2000px,dpi=240"
)
@Composable
private fun ProductListContentDataPreview() {
    BipSaleTheme {
        ProductListContent(
            state = ProductContract.State(isLoading = false, products = previewProducts),
            snackbarHostState = SnackbarHostState(),
            onAddProduct = {},
            onEditProduct = {},
            onIntent = {}
        )
    }
}

@Preview(
    name = "ProductListContent · Conteudo · Phone · Dark",
    showBackground = true,
    locale = "pt",
    device = "spec:width=1080px,height=2520px,dpi=420",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun ProductListContentDataDarkPreview() {
    BipSaleTheme {
        ProductListContent(
            state = ProductContract.State(isLoading = false, products = previewProducts),
            snackbarHostState = SnackbarHostState(),
            onAddProduct = {},
            onEditProduct = {},
            onIntent = {}
        )
    }
}

@Preview(
    name = "ProductListContent · Selecao · Phone",
    showBackground = true,
    locale = "pt",
    device = "spec:width=1080px,height=2520px,dpi=420"
)
@Preview(
    name = "ProductListContent · Selecao · Tablet",
    showBackground = true,
    locale = "pt",
    device = "spec:width=1200px,height=2000px,dpi=240"
)
@Composable
private fun ProductListContentSelectionPreview() {
    BipSaleTheme {
        ProductListContent(
            state = ProductContract.State(
                isLoading = false,
                products = previewProducts,
                selectedProductCodes = setOf("CT-A-RoS")
            ),
            snackbarHostState = SnackbarHostState(),
            onAddProduct = {},
            onEditProduct = {},
            onIntent = {}
        )
    }
}

@Preview(
    name = "ProductListMessage · Vazio · Phone",
    showBackground = true,
    locale = "pt",
    device = "spec:width=1080px,height=2520px,dpi=420"
)
@Preview(
    name = "ProductListMessage · Vazio · Tablet",
    showBackground = true,
    locale = "pt",
    device = "spec:width=1200px,height=2000px,dpi=240"
)
@Composable
private fun ProductListMessageEmptyPreview() {
    BipSaleTheme {
        ProductListMessage(text = stringResource(R.string.products_empty_state))
    }
}

@Preview(
    name = "ProductListMessage · Erro · Phone",
    showBackground = true,
    locale = "pt",
    device = "spec:width=1080px,height=2520px,dpi=420"
)
@Preview(
    name = "ProductListMessage · Erro · Tablet",
    showBackground = true,
    locale = "pt",
    device = "spec:width=1200px,height=2000px,dpi=240"
)
@Composable
private fun ProductListMessageErrorPreview() {
    BipSaleTheme {
        ProductListMessage(text = stringResource(R.string.products_generic_error))
    }
}

@Preview(
    name = "ProductListTopBar · Padrao · Phone",
    showBackground = true,
    locale = "pt",
    device = "spec:width=1080px,height=2520px,dpi=420"
)
@Preview(
    name = "ProductListTopBar · Padrao · Tablet",
    showBackground = true,
    locale = "pt",
    device = "spec:width=1200px,height=2000px,dpi=240"
)
@Composable
private fun ProductListTopBarDefaultPreview() {
    BipSaleTheme {
        ProductListTopBar(
            state = ProductContract.State(isLoading = false, products = previewProducts),
            hasSelection = false,
            onIntent = {}
        )
    }
}

@Preview(
    name = "ProductListTopBar · Selecao · Phone",
    showBackground = true,
    locale = "pt",
    device = "spec:width=1080px,height=2520px,dpi=420"
)
@Preview(
    name = "ProductListTopBar · Selecao · Tablet",
    showBackground = true,
    locale = "pt",
    device = "spec:width=1200px,height=2000px,dpi=240"
)
@Composable
private fun ProductListTopBarSelectionPreview() {
    BipSaleTheme {
        ProductListTopBar(
            state = ProductContract.State(
                isLoading = false,
                products = previewProducts,
                selectedProductCodes = setOf("CT-A-RoS")
            ),
            hasSelection = true,
            onIntent = {}
        )
    }
}

@Preview(
    name = "ProductRows · Padrao · Phone",
    showBackground = true,
    locale = "pt",
    device = "spec:width=1080px,height=2520px,dpi=420"
)
@Preview(
    name = "ProductRows · Padrao · Tablet",
    showBackground = true,
    locale = "pt",
    device = "spec:width=1200px,height=2000px,dpi=240"
)
@Composable
private fun ProductRowsDefaultPreview() {
    BipSaleTheme {
        ProductRows(
            state = ProductContract.State(isLoading = false, products = previewProducts),
            hasSelection = false,
            onEditProduct = {},
            onIntent = {}
        )
    }
}

// endregion
