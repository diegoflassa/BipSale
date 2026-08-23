package dev.diegoflassa.bipsale.feature.sales.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.diegoflassa.bipsale.core.domain.model.PaymentMethod
import dev.diegoflassa.bipsale.feature.sales.R
import dev.diegoflassa.bipsale.core.ui.theme.BipSaleTheme
import dev.diegoflassa.bipsale.feature.sales.SalesScreenTestTags

@Composable
fun SaleBottomBar(
    subtotal: Double,
    itemDiscountAmount: Double,
    discountPercentage: Double,
    total: Double,
    paymentMethod: PaymentMethod?,
    canFinalize: Boolean,
    onDiscountClick: () -> Unit,
    onPaymentMethodChange: (PaymentMethod) -> Unit,
    onFinalize: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(tonalElevation = 8.dp, modifier = modifier) {
        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            LabelledRow(stringResource(R.string.subtotal_label)) {
                Text(
                    modifier = Modifier.testTag(SalesScreenTestTags.SUBTOTAL_VALUE),
                    text = stringResource(R.string.currency_format, subtotal),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // Only shown once something has actually been discounted per line, so an operator
            // reading the total back can point at where the difference came from.
            if (itemDiscountAmount > 0.0) {
                LabelledRow(stringResource(R.string.sales_item_discounts_label)) {
                    Text(
                        text = stringResource(R.string.currency_format, itemDiscountAmount),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            LabelledRow(stringResource(R.string.discount_label)) {
                TextButton(
                    modifier = Modifier.testTag(SalesScreenTestTags.DISCOUNT_BUTTON),
                    onClick = onDiscountClick
                ) {
                    Text(stringResource(R.string.discount_percentage, discountPercentage.toInt()))
                }
            }

            LabelledRow(stringResource(R.string.payment_method_label)) {
                PaymentMethodDropdown(
                    modifier = Modifier.testTag(SalesScreenTestTags.PAYMENT_METHOD_DROPDOWN),
                    selectedMethod = paymentMethod,
                    onMethodSelected = onPaymentMethodChange
                )
            }

            LabelledRow(
                label = stringResource(R.string.total_label),
                labelStyle = MaterialTheme.typography.titleLarge
            ) {
                Text(
                    modifier = Modifier.testTag(SalesScreenTestTags.TOTAL_VALUE),
                    text = stringResource(R.string.currency_format, total),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(SalesScreenTestTags.FINALIZE_BUTTON),
                enabled = canFinalize,
                onClick = onFinalize
            ) {
                Text(stringResource(R.string.finalize_sale_button))
            }
        }
    }
}

@Composable
private fun LabelledRow(
    label: String,
    labelStyle: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyMedium,
    value: @Composable () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = labelStyle,
            color = if (labelStyle == MaterialTheme.typography.titleLarge) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        )
        value()
    }
}

// region Previews

@Preview(name = "SaleBottomBar · Vazio · Phone", showBackground = true, locale = "pt", device = "spec:width=1080px,height=2520px,dpi=420")
@Preview(name = "SaleBottomBar · Vazio · Tablet", showBackground = true, locale = "pt", device = "spec:width=1200px,height=2000px,dpi=240")
@Composable
private fun SaleBottomBarEmptyPreview() {
    BipSaleTheme {
        SaleBottomBar(
            subtotal = 0.0,
            itemDiscountAmount = 0.0,
            discountPercentage = 0.0,
            total = 0.0,
            paymentMethod = null,
            canFinalize = false,
            onDiscountClick = {},
            onPaymentMethodChange = {},
            onFinalize = {}
        )
    }
}

@Preview(name = "SaleBottomBar · Com desconto · Phone", showBackground = true, locale = "pt", device = "spec:width=1080px,height=2520px,dpi=420")
@Preview(name = "SaleBottomBar · Com desconto · Tablet", showBackground = true, locale = "pt", device = "spec:width=1200px,height=2000px,dpi=240")
@Composable
private fun SaleBottomBarDiscountedPreview() {
    BipSaleTheme {
        SaleBottomBar(
            subtotal = 89.90,
            itemDiscountAmount = 9.90,
            discountPercentage = 10.0,
            total = 72.0,
            paymentMethod = PaymentMethod.CREDIT_CARD,
            canFinalize = true,
            onDiscountClick = {},
            onPaymentMethodChange = {},
            onFinalize = {}
        )
    }
}

@Preview(name = "SaleBottomBar · Com desconto · Phone · Dark", showBackground = true, locale = "pt", device = "spec:width=1080px,height=2520px,dpi=420", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SaleBottomBarDiscountedDarkPreview() {
    BipSaleTheme {
        SaleBottomBar(
            subtotal = 89.90,
            itemDiscountAmount = 9.90,
            discountPercentage = 10.0,
            total = 72.0,
            paymentMethod = PaymentMethod.CREDIT_CARD,
            canFinalize = true,
            onDiscountClick = {},
            onPaymentMethodChange = {},
            onFinalize = {}
        )
    }
}

// endregion
