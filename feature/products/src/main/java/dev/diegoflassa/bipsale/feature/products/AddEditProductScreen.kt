package dev.diegoflassa.bipsale.feature.products

import android.content.res.Configuration
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.diegoflassa.bipsale.core.qrcode.LabelData
import dev.diegoflassa.bipsale.core.qrcode.QrGenerator
import dev.diegoflassa.bipsale.core.qrcode.QrLabelSheetRenderer
import dev.diegoflassa.bipsale.core.ui.theme.BipSaleTheme
import dev.diegoflassa.bipsale.feature.products.components.ProductImagePicker

@Composable
fun AddEditProductScreen(
    productCode: String?,
    onBack: () -> Unit,
    viewModel: ProductViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            if (effect is ProductContract.Effect.NavigationBack) onBack()
        }
    }

    LaunchedEffect(productCode) {
        if (productCode != null) {
            viewModel.onIntent(ProductContract.Intent.LoadProduct(productCode))
        }
    }

    AddEditProductContent(
        editor = uiState.editor,
        isEdit = productCode != null,
        onBack = onBack,
        onIntent = viewModel::onIntent
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddEditProductContent(
    editor: ProductContract.Editor,
    isEdit: Boolean,
    onBack: () -> Unit,
    onIntent: (ProductContract.Intent) -> Unit,
    modifier: Modifier = Modifier
) {
    val renderer = remember { QrLabelSheetRenderer(QrGenerator()) }
    val labelBitmap = remember(editor.label) {
        editor.label?.let { renderer.renderLabelPreview(it, LABEL_PREVIEW_PX) }
    }

    Scaffold(
        modifier = modifier.testTag(AddEditProductScreenTestTags.ROOT),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (isEdit) R.string.products_edit_product
                            else R.string.products_new_product
                        )
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag(AddEditProductScreenTestTags.BACK_BUTTON)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.products_back)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ProductImagePicker(
                imagePath = editor.imagePath,
                onImagePicked = {
                    onIntent(ProductContract.Intent.ImagePicked(it.toString()))
                },
                modifier = Modifier.testTag(AddEditProductScreenTestTags.IMAGE_PICKER)
            )

            ProductEditorFields(editor = editor, isEdit = isEdit, onIntent = onIntent)

            Button(
                onClick = { onIntent(ProductContract.Intent.SaveProduct) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(AddEditProductScreenTestTags.SAVE_BUTTON),
                enabled = editor.canSave
            ) {
                Text(stringResource(R.string.products_save))
            }

            if (labelBitmap != null) {
                LabelPrintPreview(
                    labelBitmap = labelBitmap,
                    onPrint = { onIntent(ProductContract.Intent.PrintEditorLabel) }
                )
            }
        }
    }
}

@Composable
private fun ProductEditorFields(
    editor: ProductContract.Editor,
    isEdit: Boolean,
    onIntent: (ProductContract.Intent) -> Unit
) {
    val priceRejected = editor.priceInput.isNotBlank() && editor.label == null

    OutlinedTextField(
        value = editor.code,
        onValueChange = { onIntent(ProductContract.Intent.CodeChanged(it)) },
        label = { Text(stringResource(R.string.products_code_label)) },
        modifier = Modifier
            .fillMaxWidth()
            .testTag(AddEditProductScreenTestTags.CODE_FIELD),
        enabled = !isEdit,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
    )
    OutlinedTextField(
        value = editor.name,
        onValueChange = { onIntent(ProductContract.Intent.NameChanged(it)) },
        label = { Text(stringResource(R.string.products_name_label)) },
        modifier = Modifier
            .fillMaxWidth()
            .testTag(AddEditProductScreenTestTags.NAME_FIELD)
    )
    OutlinedTextField(
        value = editor.priceInput,
        onValueChange = { onIntent(ProductContract.Intent.PriceChanged(it)) },
        label = { Text(stringResource(R.string.products_price_label)) },
        modifier = Modifier
            .fillMaxWidth()
            .testTag(AddEditProductScreenTestTags.PRICE_FIELD),
        singleLine = true,
        isError = priceRejected,
        supportingText = {
            if (priceRejected) Text(stringResource(R.string.products_price_invalid))
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
    )
}

@Composable
private fun LabelPrintPreview(labelBitmap: Bitmap, onPrint: () -> Unit) {
    Text(
        stringResource(R.string.products_qr_code_label),
        style = MaterialTheme.typography.labelMedium
    )
    Image(
        bitmap = labelBitmap.asImageBitmap(),
        contentDescription = stringResource(R.string.products_qr_label_preview),
        modifier = Modifier
            .width(LABEL_PREVIEW_WIDTH_DP.dp)
            .testTag(AddEditProductScreenTestTags.LABEL_PREVIEW)
    )
    Text(
        text = stringResource(R.string.products_print_preview_hint),
        style = MaterialTheme.typography.bodySmall,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    OutlinedButton(
        onClick = onPrint,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(AddEditProductScreenTestTags.PRINT_BUTTON)
    ) {
        Text(stringResource(R.string.products_print_qr_code))
    }
}

private const val LABEL_PREVIEW_PX = 420
private const val LABEL_PREVIEW_WIDTH_DP = 200

// region Previews

private val previewEditorFilled = ProductContract.Editor(
    code = "CT-A-RoS",
    name = "Coturno cano alto rosa",
    priceInput = "130,00",
    label = LabelData(
        qrData = "bipsale://product?code=CT-A-RoS&price=130.0",
        productName = "Coturno cano alto rosa",
        priceFormatted = "R$ 130,00"
    )
)

@Preview(
    name = "AddEditProductContent · Novo · Phone",
    showBackground = true,
    locale = "pt",
    device = "spec:width=1080px,height=2520px,dpi=420"
)
@Preview(
    name = "AddEditProductContent · Novo · Tablet",
    showBackground = true,
    locale = "pt",
    device = "spec:width=1200px,height=2000px,dpi=240"
)
@Composable
private fun AddEditProductContentNewPreview() {
    BipSaleTheme {
        AddEditProductContent(
            editor = ProductContract.Editor(),
            isEdit = false,
            onBack = {},
            onIntent = {}
        )
    }
}

@Preview(
    name = "AddEditProductContent · Novo · Phone · Dark",
    showBackground = true,
    locale = "pt",
    device = "spec:width=1080px,height=2520px,dpi=420",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun AddEditProductContentNewDarkPreview() {
    BipSaleTheme {
        AddEditProductContent(
            editor = ProductContract.Editor(),
            isEdit = false,
            onBack = {},
            onIntent = {}
        )
    }
}

@Preview(
    name = "AddEditProductContent · Preco Invalido · Phone",
    showBackground = true,
    locale = "pt",
    device = "spec:width=1080px,height=2520px,dpi=420"
)
@Preview(
    name = "AddEditProductContent · Preco Invalido · Tablet",
    showBackground = true,
    locale = "pt",
    device = "spec:width=1200px,height=2000px,dpi=240"
)
@Composable
private fun AddEditProductContentInvalidPricePreview() {
    BipSaleTheme {
        AddEditProductContent(
            editor = ProductContract.Editor(
                code = "CT-A-RoS",
                name = "Coturno cano alto rosa",
                priceInput = "abc"
            ),
            isEdit = true,
            onBack = {},
            onIntent = {}
        )
    }
}

@Preview(
    name = "AddEditProductContent · Editar Preenchido · Phone",
    showBackground = true,
    locale = "pt",
    device = "spec:width=1080px,height=2520px,dpi=420"
)
@Preview(
    name = "AddEditProductContent · Editar Preenchido · Tablet",
    showBackground = true,
    locale = "pt",
    device = "spec:width=1200px,height=2000px,dpi=240"
)
@Composable
private fun AddEditProductContentEditFilledPreview() {
    BipSaleTheme {
        AddEditProductContent(
            editor = previewEditorFilled,
            isEdit = true,
            onBack = {},
            onIntent = {}
        )
    }
}

// endregion
