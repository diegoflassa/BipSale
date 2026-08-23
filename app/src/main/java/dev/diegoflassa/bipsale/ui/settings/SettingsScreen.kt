package dev.diegoflassa.bipsale.ui.settings

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
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
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
                title = stringResource(R.string.settings_pix_section),
                explainer = stringResource(R.string.settings_pix_explainer)
            )

            PixFields(state = state, onIntent = onIntent)

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
private fun PixFields(
    state: SettingsContract.State,
    onIntent: (SettingsContract.Intent) -> Unit
) {
    OutlinedTextField(
        value = state.pixKey,
        onValueChange = { onIntent(SettingsContract.Intent.PixKeyChanged(it)) },
        label = { Text(stringResource(R.string.settings_pix_key_label)) },
        supportingText = { Text(stringResource(R.string.settings_pix_key_hint)) },
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(SettingsScreenTestTags.PIX_KEY_FIELD)
    )

    OutlinedTextField(
        value = state.merchantName,
        onValueChange = { onIntent(SettingsContract.Intent.MerchantNameChanged(it)) },
        label = { Text(stringResource(R.string.settings_merchant_name_label)) },
        supportingText = { Text(stringResource(R.string.settings_merchant_name_hint)) },
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(SettingsScreenTestTags.MERCHANT_NAME_FIELD)
    )

    OutlinedTextField(
        value = state.merchantCity,
        onValueChange = { onIntent(SettingsContract.Intent.MerchantCityChanged(it)) },
        label = { Text(stringResource(R.string.settings_merchant_city_label)) },
        supportingText = { Text(stringResource(R.string.settings_merchant_city_hint)) },
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(SettingsScreenTestTags.MERCHANT_CITY_FIELD)
    )
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
                pixKey = "123.456.789-00",
                merchantName = "Padaria do Centro",
                merchantCity = "Sao Paulo",
                discountKind = ItemDiscountKind.PERCENTAGE,
                discountInput = "5",
                isLoading = false
            ),
            snackbarHostState = SnackbarHostState(),
            onBack = {},
            onIntent = {}
        )
    }
}

// endregion
