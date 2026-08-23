package dev.diegoflassa.bipsale.feature.history.components

import android.content.res.Configuration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.diegoflassa.bipsale.core.domain.model.PaymentMethod
import dev.diegoflassa.bipsale.core.domain.model.Sale
import dev.diegoflassa.bipsale.core.ui.theme.BipSaleTheme
import dev.diegoflassa.bipsale.feature.history.HistoryScreenTestTags
import dev.diegoflassa.bipsale.feature.history.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SaleHistoryItem(
    sale: Sale,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    /** Present only for a PIX sale, where showing the code again is something an operator needs. */
    onShowPix: (() -> Unit)? = null
) {
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        colors = if (isSelected) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        } else {
            CardDefaults.cardColors()
        }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = sale.customerName.ifEmpty {
                        stringResource(R.string.history_anonymous_customer)
                    },
                    style = MaterialTheme.typography.titleMedium
                )
                if (isSelected) {
                    Checkbox(checked = true, onCheckedChange = { onClick() })
                } else {
                    Text(
                        text = dateFormat.format(Date(sale.date)),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            Text(
                text = stringResource(
                    R.string.history_cpf,
                    sale.customerCpf.ifEmpty { stringResource(R.string.history_cpf_empty) }
                ),
                style = MaterialTheme.typography.bodySmall
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.history_total_label),
                    style = MaterialTheme.typography.bodyMedium
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.history_currency, sale.finalAmount),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (onShowPix != null && !isSelected) {
                        IconButton(
                            onClick = onShowPix,
                            modifier = Modifier.testTag(
                                HistoryScreenTestTags.pixButton(sale.id)
                            )
                        ) {
                            Icon(
                                Icons.Default.QrCode2,
                                contentDescription = stringResource(R.string.history_pix_show)
                            )
                        }
                    }
                }
            }
        }
    }
}

// region Previews

private val previewSaleNamed = Sale(
    id = "1",
    customerName = "Maria Aparecida Souza",
    customerCpf = "123.456.789-00",
    totalAmount = 89.90,
    discountPercentage = 0.0,
    finalAmount = 89.90,
    paymentMethod = PaymentMethod.PIX,
    date = 1752000000000L,
)

private val previewSaleAnonymous = Sale(
    id = "2",
    customerName = "",
    customerCpf = "",
    totalAmount = 45.00,
    discountPercentage = 10.0,
    finalAmount = 40.50,
    paymentMethod = PaymentMethod.CASH,
    date = 1752003600000L,
)

@Preview(name = "SaleHistoryItem · Nomeado · Phone", showBackground = true, locale = "pt", device = "spec:width=1080px,height=2520px,dpi=420")
@Preview(name = "SaleHistoryItem · Nomeado · Tablet", showBackground = true, locale = "pt", device = "spec:width=1200px,height=2000px,dpi=240")
@Composable
private fun SaleHistoryItemNamedPreview() {
    BipSaleTheme {
        SaleHistoryItem(sale = previewSaleNamed, isSelected = false, onClick = {}, onLongClick = {})
    }
}

@Preview(name = "SaleHistoryItem · Nomeado · Phone · Dark", showBackground = true, locale = "pt", device = "spec:width=1080px,height=2520px,dpi=420", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SaleHistoryItemNamedDarkPreview() {
    BipSaleTheme {
        SaleHistoryItem(sale = previewSaleNamed, isSelected = false, onClick = {}, onLongClick = {})
    }
}

@Preview(name = "SaleHistoryItem · Anônimo · Phone", showBackground = true, locale = "pt", device = "spec:width=1080px,height=2520px,dpi=420")
@Preview(name = "SaleHistoryItem · Anônimo · Tablet", showBackground = true, locale = "pt", device = "spec:width=1200px,height=2000px,dpi=240")
@Composable
private fun SaleHistoryItemAnonymousPreview() {
    BipSaleTheme {
        SaleHistoryItem(sale = previewSaleAnonymous, isSelected = false, onClick = {}, onLongClick = {})
    }
}

@Preview(name = "SaleHistoryItem · Selecionado · Phone", showBackground = true, locale = "pt", device = "spec:width=1080px,height=2520px,dpi=420")
@Preview(name = "SaleHistoryItem · Selecionado · Tablet", showBackground = true, locale = "pt", device = "spec:width=1200px,height=2000px,dpi=240")
@Composable
private fun SaleHistoryItemSelectedPreview() {
    BipSaleTheme {
        SaleHistoryItem(sale = previewSaleNamed, isSelected = true, onClick = {}, onLongClick = {})
    }
}

// endregion
