package dev.diegoflassa.bipsale.ui.dashboard

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.diegoflassa.bipsale.R
import dev.diegoflassa.bipsale.core.ui.components.BipSaleTopAppBar
import dev.diegoflassa.bipsale.core.ui.theme.BipSaleTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNewSale: () -> Unit,
    onManageProducts: () -> Unit,
    onHistory: () -> Unit,
    onExport: () -> Unit,
    onBackup: () -> Unit,
    onSettings: () -> Unit
) {
    Scaffold(
        topBar = {
            // The dashboard is the start destination, so there is nowhere to go back to.
            BipSaleTopAppBar(title = stringResource(R.string.dashboard_title))
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                // Six entries overflow a phone in landscape, and a card the operator cannot
                // reach is a feature that does not exist.
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            DashboardCard(
                stringResource(R.string.dashboard_new_sale_title),
                stringResource(R.string.dashboard_new_sale_description),
                Icons.Default.ShoppingCart,
                onNewSale
            )
            DashboardCard(
                stringResource(R.string.dashboard_products_title),
                stringResource(R.string.dashboard_products_description),
                Icons.Default.Inventory,
                onManageProducts
            )
            DashboardCard(
                stringResource(R.string.dashboard_history_title),
                stringResource(R.string.dashboard_history_description),
                Icons.Default.History,
                onHistory
            )
            DashboardCard(
                stringResource(R.string.dashboard_reports_title),
                stringResource(R.string.dashboard_reports_description),
                Icons.Default.Description,
                onExport
            )
            DashboardCard(
                stringResource(R.string.dashboard_backup_title),
                stringResource(R.string.dashboard_backup_description),
                Icons.Default.Backup,
                onBackup
            )
            DashboardCard(
                stringResource(R.string.dashboard_settings),
                stringResource(R.string.dashboard_settings_description),
                Icons.Default.Settings,
                onSettings
            )
        }
    }
}

@Composable
fun DashboardCard(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(100.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleLarge)
                Text(description, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

// region Previews

@Preview(name = "DashboardScreen · Default · Phone", showBackground = true, locale = "pt", device = "spec:width=1080px,height=2520px,dpi=420")
@Preview(name = "DashboardScreen · Default · Tablet", showBackground = true, locale = "pt", device = "spec:width=1200px,height=2000px,dpi=240")
@Composable
private fun DashboardScreenPreview() {
    BipSaleTheme {
        DashboardScreen(
            onNewSale = {},
            onManageProducts = {},
            onHistory = {},
            onExport = {},
            onBackup = {},
            onSettings = {}
        )
    }
}

@Preview(name = "DashboardScreen · Default · Phone · Dark", showBackground = true, locale = "pt", device = "spec:width=1080px,height=2520px,dpi=420", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DashboardScreenDarkPreview() {
    BipSaleTheme {
        DashboardScreen(
            onNewSale = {},
            onManageProducts = {},
            onHistory = {},
            onExport = {},
            onBackup = {},
            onSettings = {}
        )
    }
}

@Preview(name = "DashboardCard · Default · Phone", showBackground = true, locale = "pt", device = "spec:width=1080px,height=2520px,dpi=420")
@Preview(name = "DashboardCard · Default · Tablet", showBackground = true, locale = "pt", device = "spec:width=1200px,height=2000px,dpi=240")
@Composable
private fun DashboardCardPreview() {
    BipSaleTheme {
        DashboardCard(
            title = "Nova Venda",
            description = "Inicie uma venda via QR Code",
            icon = Icons.Default.ShoppingCart,
            onClick = {},
        )
    }
}

@Preview(name = "DashboardCard · Long Text · Phone", showBackground = true, locale = "pt", device = "spec:width=1080px,height=2520px,dpi=420")
@Composable
private fun DashboardCardLongTextPreview() {
    BipSaleTheme {
        DashboardCard(
            title = "Relatórios Financeiros Consolidados",
            description = "Exportar todos os dados de vendas e produtos para uma planilha Excel detalhada",
            icon = Icons.Default.Description,
            onClick = {},
        )
    }
}

// endregion
