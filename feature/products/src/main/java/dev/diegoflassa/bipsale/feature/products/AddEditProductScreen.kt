package dev.diegoflassa.bipsale.feature.products

import android.content.res.Configuration
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material3.Surface
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.diegoflassa.bipsale.core.qrcode.LabelData
import dev.diegoflassa.bipsale.core.qrcode.QrGenerator
import dev.diegoflassa.bipsale.core.qrcode.QrLabelSheetLayout
import dev.diegoflassa.bipsale.core.qrcode.QrLabelSheetRenderer
import dev.diegoflassa.bipsale.core.ui.components.BipSaleTopAppBar
import dev.diegoflassa.bipsale.core.ui.theme.BipSaleTheme
import dev.diegoflassa.bipsale.feature.products.components.ProductImagePicker
import dev.diegoflassa.bipsale.feature.products.print.QrLabelPrinter
import kotlin.math.roundToInt

@Composable
fun AddEditProductScreen(
    productCode: String?,
    onBack: () -> Unit,
    viewModel: ProductViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val printer = remember { QrLabelPrinter(QrLabelSheetRenderer(QrGenerator())) }

    LaunchedEffect(viewModel, context) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is ProductContract.Effect.NavigationBack -> onBack()

                is ProductContract.Effect.ShowSnackbar ->
                    snackbarHostState.showSnackbar(effect.message.asString(context))

                is ProductContract.Effect.PrintLabels ->
                    printer.print(
                        context,
                        context.getString(R.string.products_qr_labels_title),
                        effect.labels,
                        effect.requestedColumns
                    )

                // Both belong to the list screen's import flow; this screen never raises them.
                is ProductContract.Effect.PickTemplateDestination,
                is ProductContract.Effect.PickImportSource -> Unit
            }
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
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onIntent = viewModel::onIntent
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddEditProductContent(
    editor: ProductContract.Editor,
    isEdit: Boolean,
    snackbarHostState: SnackbarHostState,
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            BipSaleTopAppBar(
                title = stringResource(
                    if (isEdit) R.string.products_edit_product
                    else R.string.products_new_product
                ),
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
                    onExpand = { onIntent(ProductContract.Intent.ShowLabelPreview) },
                    onPrint = { onIntent(ProductContract.Intent.PrintEditorLabel) }
                )
            }
        }

        if (editor.isLabelPreviewVisible && labelBitmap != null) {
            RealSizeLabelDialog(
                labelBitmap = labelBitmap,
                onDismiss = { onIntent(ProductContract.Intent.HideLabelPreview) }
            )
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
        supportingText = { Text(stringResource(R.string.products_name_hint)) },
        singleLine = true,
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
            if (priceRejected) {
                Text(stringResource(R.string.products_price_invalid))
            } else {
                Text(stringResource(R.string.products_price_hint))
            }
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
    )
    OutlinedTextField(
        value = editor.quantityInput,
        onValueChange = { onIntent(ProductContract.Intent.QuantityChanged(it)) },
        label = { Text(stringResource(R.string.products_quantity_label)) },
        supportingText = { Text(stringResource(R.string.products_quantity_hint)) },
        modifier = Modifier
            .fillMaxWidth()
            .testTag(AddEditProductScreenTestTags.QUANTITY_FIELD),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
    )
}

@Composable
private fun LabelPrintPreview(
    labelBitmap: Bitmap,
    onExpand: () -> Unit,
    onPrint: () -> Unit
) {
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
            .clickable(
                onClickLabel = stringResource(R.string.products_label_real_size_title),
                onClick = onExpand
            )
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

/**
 * Shows the label at the size it physically prints.
 *
 * The conversion goes through the panel's real pixel pitch (`xdpi`/`ydpi`) rather than the density
 * bucket: a bucket is rounded to the nearest standard density, so a ruler held to the screen would
 * disagree with the printout by a few millimetres.
 */
@Composable
private fun RealSizeLabelDialog(labelBitmap: Bitmap, onDismiss: () -> Unit) {
    val layout = remember { QrLabelSheetLayout.a4() }
    val metrics = LocalContext.current.resources.displayMetrics
    val density = LocalDensity.current

    val widthDp = with(density) {
        ((layout.cellWidthPt / POINTS_PER_INCH) * metrics.xdpi).toDp()
    }
    val heightDp = with(density) {
        ((layout.cellHeightPt / POINTS_PER_INCH) * metrics.ydpi).toDp()
    }
    val widthMm = QrLabelSheetLayout.pointsToMillimetres(layout.cellWidthPt).roundToInt()
    val heightMm = QrLabelSheetLayout.pointsToMillimetres(layout.cellHeightPt).roundToInt()

    Dialog(
        onDismissRequest = onDismiss,
        // Spelled out rather than left to defaults: back and an outside tap are the two ways out
        // an operator will reach for first, and both must close this.
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
    ) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier.testTag(AddEditProductScreenTestTags.REAL_SIZE_DIALOG)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.products_label_real_size_title),
                    style = MaterialTheme.typography.titleMedium
                )
                Image(
                    bitmap = labelBitmap.asImageBitmap(),
                    contentDescription = stringResource(R.string.products_qr_label_preview),
                    modifier = Modifier.size(width = widthDp, height = heightDp)
                )
                Text(
                    text = stringResource(
                        R.string.products_label_real_size_caption,
                        widthMm.toString(),
                        heightMm.toString()
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(AddEditProductScreenTestTags.REAL_SIZE_CLOSE)
                ) {
                    Text(stringResource(R.string.products_close))
                }
            }
        }
    }
}

private const val POINTS_PER_INCH = 72f
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
            snackbarHostState = SnackbarHostState(),
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
            snackbarHostState = SnackbarHostState(),
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
            snackbarHostState = SnackbarHostState(),
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
            snackbarHostState = SnackbarHostState(),
            onBack = {},
            onIntent = {}
        )
    }
}

@Preview(
    name = "ProductEditorFields · Preenchido · Phone",
    showBackground = true,
    locale = "pt",
    device = "spec:width=1080px,height=2520px,dpi=420"
)
@Preview(
    name = "ProductEditorFields · Preenchido · Tablet",
    showBackground = true,
    locale = "pt",
    device = "spec:width=1200px,height=2000px,dpi=240"
)
@Composable
private fun ProductEditorFieldsFilledPreview() {
    BipSaleTheme {
        Column { ProductEditorFields(editor = previewEditorFilled, isEdit = true, onIntent = {}) }
    }
}

@Preview(
    name = "ProductEditorFields · Preco Invalido · Phone",
    showBackground = true,
    locale = "pt",
    device = "spec:width=1080px,height=2520px,dpi=420"
)
@Preview(
    name = "ProductEditorFields · Preco Invalido · Tablet",
    showBackground = true,
    locale = "pt",
    device = "spec:width=1200px,height=2000px,dpi=240"
)
@Composable
private fun ProductEditorFieldsInvalidPricePreview() {
    BipSaleTheme {
        Column {
            ProductEditorFields(
                editor = ProductContract.Editor(
                    code = "CT-A-RoS",
                    name = "Coturno cano alto rosa",
                    priceInput = "abc"
                ),
                isEdit = true,
                onIntent = {}
            )
        }
    }
}

@Preview(
    name = "LabelPrintPreview · Padrao · Phone",
    showBackground = true,
    locale = "pt",
    device = "spec:width=1080px,height=2520px,dpi=420"
)
@Preview(
    name = "LabelPrintPreview · Padrao · Tablet",
    showBackground = true,
    locale = "pt",
    device = "spec:width=1200px,height=2000px,dpi=240"
)
@Composable
private fun LabelPrintPreviewDefaultPreview() {
    BipSaleTheme {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            LabelPrintPreview(
                labelBitmap = previewLabelBitmap(),
                onExpand = {},
                onPrint = {}
            )
        }
    }
}

@Preview(
    name = "RealSizeLabelDialog · Padrao · Phone",
    showBackground = true,
    locale = "pt",
    device = "spec:width=1080px,height=2520px,dpi=420"
)
@Preview(
    name = "RealSizeLabelDialog · Padrao · Tablet",
    showBackground = true,
    locale = "pt",
    device = "spec:width=1200px,height=2000px,dpi=240"
)
@Composable
private fun RealSizeLabelDialogPreview() {
    BipSaleTheme {
        RealSizeLabelDialog(labelBitmap = previewLabelBitmap(), onDismiss = {})
    }
}

/** Rendered through the real renderer so the preview shows the geometry that actually prints. */
@Composable
private fun previewLabelBitmap(): Bitmap {
    val renderer = remember { QrLabelSheetRenderer(QrGenerator()) }
    return remember { renderer.renderLabelPreview(previewEditorFilled.label!!, LABEL_PREVIEW_PX) }
}

// endregion
