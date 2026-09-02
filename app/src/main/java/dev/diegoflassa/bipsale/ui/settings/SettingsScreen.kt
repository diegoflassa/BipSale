package dev.diegoflassa.bipsale.ui.settings

import dev.diegoflassa.bipsale.core.domain.settings.AppSettings
import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.diegoflassa.bipsale.R
import dev.diegoflassa.bipsale.core.ui.components.BipSaleTopAppBar
import dev.diegoflassa.bipsale.core.ui.theme.BipSaleTheme
import dev.diegoflassa.bipsale.feature.sales.components.ItemDiscountKind

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(viewModel, context) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is SettingsContract.Effect.ShowSnackbar ->
                    snackbarHostState.showSnackbar(effect.message.asString(context))

                is SettingsContract.Effect.Saved -> Unit
            }
        }
    }

    SettingsScreenContent(
        state = uiState,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onIntent = viewModel::onIntent
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsScreenContent(
    state: SettingsContract.State,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onIntent: (SettingsContract.Intent) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.testTag(SettingsScreenTestTags.ROOT),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            BipSaleTopAppBar(
                title = stringResource(R.string.settings_title),
                onBack = onBack
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SectionHeader(
                title = stringResource(R.string.settings_discount_section),
                explainer = stringResource(R.string.settings_discount_explainer)
            )

            DiscountKindSelector(
                kind = state.discountKind,
                onKindChange = { onIntent(SettingsContract.Intent.DiscountKindChanged(it)) }
            )

            OutlinedTextField(
                value = state.discountInput,
                onValueChange = { onIntent(SettingsContract.Intent.DiscountValueChanged(it)) },
                label = { Text(stringResource(state.discountKind.labelRes())) },
                supportingText = {
                    Text(
                        stringResource(R.string.settings_discount_blank_hint) + " " +
                            stringResource(state.discountKind.hintRes())
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(SettingsScreenTestTags.DISCOUNT_FIELD)
            )

            SectionHeader(
                title = stringResource(R.string.settings_sale_section),
                explainer = stringResource(R.string.settings_ask_customer_hint)
            )

            AskCustomerRow(
                enabled = state.askCustomerInfo,
                onChange = { onIntent(SettingsContract.Intent.AskCustomerInfoChanged(it)) }
            )

            SectionHeader(
                title = stringResource(R.string.settings_qr_columns_section),
                explainer = stringResource(R.string.settings_qr_columns_explainer)
            )

            QrColumnsSelector(
                columns = state.qrLabelColumns,
                onChange = { onIntent(SettingsContract.Intent.QrColumnsChanged(it)) }
            )

            QrLabelTextSizeSection(
                nameSizePt = state.qrLabelNameTextSizePt,
                priceSizePt = state.qrLabelPriceTextSizePt,
                onIntent = onIntent
            )

            Button(
                onClick = { onIntent(SettingsContract.Intent.Save) },
                enabled = state.canSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(SettingsScreenTestTags.SAVE_BUTTON)
            ) {
                Text(stringResource(R.string.settings_save))
            }
        }
    }
}



@Composable
private fun AskCustomerRow(
    enabled: Boolean,
    onChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(SettingsScreenTestTags.ASK_CUSTOMER_SWITCH),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = stringResource(R.string.settings_ask_customer_label),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        Switch(checked = enabled, onCheckedChange = onChange)
    }
}

@Composable
private fun SectionHeader(title: String, explainer: String) {
    Text(text = title, style = MaterialTheme.typography.titleMedium)
    Text(
        text = explainer,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DiscountKindSelector(
    kind: ItemDiscountKind,
    onKindChange: (ItemDiscountKind) -> Unit
) {
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        ItemDiscountKind.entries.forEachIndexed { index, entry ->
            SegmentedButton(
                selected = kind == entry,
                onClick = { onKindChange(entry) },
                shape = SegmentedButtonDefaults.itemShape(
                    index = index,
                    count = ItemDiscountKind.entries.size
                )
            ) {
                Text(stringResource(entry.labelRes()))
            }
        }
    }
}

private fun ItemDiscountKind.hintRes(): Int = when (this) {
    ItemDiscountKind.PERCENTAGE -> R.string.settings_discount_percentage_hint
    ItemDiscountKind.AMOUNT -> R.string.settings_discount_amount_hint
}

private fun ItemDiscountKind.labelRes(): Int = when (this) {
    ItemDiscountKind.PERCENTAGE -> R.string.settings_discount_percentage
    ItemDiscountKind.AMOUNT -> R.string.settings_discount_amount
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QrColumnsSelector(
    columns: Int,
    onChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.settings_qr_columns_label),
            style = MaterialTheme.typography.bodyLarge
        )
        SingleChoiceSegmentedButtonRow(modifier = Modifier.weight(1f)) {
            val range = AppSettings.MIN_QR_LABEL_COLUMNS..AppSettings.MAX_QR_LABEL_COLUMNS
            range.forEachIndexed { index, option ->
                SegmentedButton(
                    selected = columns == option,
                    onClick = { onChange(option) },
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = range.count()
                    )
                ) {
                    Text(option.toString())
                }
            }
        }
    }
}

@Composable
private fun QrLabelTextSizeSection(
    nameSizePt: Int,
    priceSizePt: Int,
    onIntent: (SettingsContract.Intent) -> Unit
) {
    SectionHeader(
        title = stringResource(R.string.settings_qr_text_size_section),
        explainer = stringResource(R.string.settings_qr_text_size_explainer)
    )

    TextSizeStepper(
        label = stringResource(R.string.settings_qr_name_text_size_label),
        sizePt = nameSizePt,
        onChange = { onIntent(SettingsContract.Intent.QrLabelNameTextSizeChanged(it)) },
        valueTag = SettingsScreenTestTags.NAME_TEXT_SIZE_VALUE,
        decreaseTag = SettingsScreenTestTags.NAME_TEXT_SIZE_DECREASE,
        increaseTag = SettingsScreenTestTags.NAME_TEXT_SIZE_INCREASE
    )

    TextSizeStepper(
        label = stringResource(R.string.settings_qr_price_text_size_label),
        sizePt = priceSizePt,
        onChange = { onIntent(SettingsContract.Intent.QrLabelPriceTextSizeChanged(it)) },
        valueTag = SettingsScreenTestTags.PRICE_TEXT_SIZE_VALUE,
        decreaseTag = SettingsScreenTestTags.PRICE_TEXT_SIZE_DECREASE,
        increaseTag = SettingsScreenTestTags.PRICE_TEXT_SIZE_INCREASE
    )
}

/**
 * A stepper rather than a segmented row: the point range is far too wide to lay out as buttons, and
 * a slider makes it fiddly to land on an exact point size.
 */
@Composable
private fun TextSizeStepper(
    label: String,
    sizePt: Int,
    onChange: (Int) -> Unit,
    valueTag: String,
    decreaseTag: String,
    increaseTag: String
) {
    val range = AppSettings.MIN_QR_LABEL_TEXT_SIZE_PT..AppSettings.MAX_QR_LABEL_TEXT_SIZE_PT
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        IconButton(
            onClick = { onChange(sizePt - 1) },
            enabled = sizePt > range.first,
            modifier = Modifier.testTag(decreaseTag)
        ) {
            Icon(
                Icons.Default.Remove,
                contentDescription = stringResource(R.string.settings_qr_text_size_decrease)
            )
        }
        Text(
            text = stringResource(R.string.settings_qr_text_size_points, sizePt),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.testTag(valueTag)
        )
        IconButton(
            onClick = { onChange(sizePt + 1) },
            enabled = sizePt < range.last,
            modifier = Modifier.testTag(increaseTag)
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = stringResource(R.string.settings_qr_text_size_increase)
            )
        }
    }
}

// region Previews

@Preview(name = "SettingsScreenContent · Vazio · Phone", showBackground = true, locale = "pt", device = "spec:width=1080px,height=2520px,dpi=420")
@Preview(name = "SettingsScreenContent · Vazio · Tablet", showBackground = true, locale = "pt", device = "spec:width=1200px,height=2000px,dpi=240")
@Composable
private fun SettingsScreenContentEmptyPreview() {
    BipSaleTheme {
        SettingsScreenContent(
            state = SettingsContract.State(isLoading = false),
            snackbarHostState = SnackbarHostState(),
            onBack = {},
            onIntent = {}
        )
    }
}

@Preview(name = "SettingsScreenContent · Configurado · Phone", showBackground = true, locale = "pt", device = "spec:width=1080px,height=2520px,dpi=420")
@Preview(name = "SettingsScreenContent · Configurado · Phone · Dark", showBackground = true, locale = "pt", device = "spec:width=1080px,height=2520px,dpi=420", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SettingsScreenContentFilledPreview() {
    BipSaleTheme {
        SettingsScreenContent(
            state = SettingsContract.State(
                discountKind = ItemDiscountKind.PERCENTAGE,
                discountInput = "5",
                askCustomerInfo = false,
                qrLabelColumns = 3,
                isLoading = false
            ),
            snackbarHostState = SnackbarHostState(),
            onBack = {},
            onIntent = {}
        )
    }
}

@Preview(name = "QrColumnsSelector")
@Composable
private fun QrColumnsSelectorPreview() {
    BipSaleTheme {
        QrColumnsSelector(columns = 4, onChange = {})
    }
}

// endregion
