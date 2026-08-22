package dev.diegoflassa.bipsale

import androidx.compose.runtime.Composable
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import dev.diegoflassa.bipsale.core.navigation.Screen
import dev.diegoflassa.bipsale.feature.history.HistoryScreen
import dev.diegoflassa.bipsale.feature.history.SaleDetailScreen
import dev.diegoflassa.bipsale.feature.products.AddEditProductScreen
import dev.diegoflassa.bipsale.feature.products.ProductListScreen
import dev.diegoflassa.bipsale.feature.sales.CustomerInfoScreen
import dev.diegoflassa.bipsale.feature.sales.SalesScreen
import dev.diegoflassa.bipsale.ui.backup.BackupScreen
import dev.diegoflassa.bipsale.ui.dashboard.DashboardScreen
import dev.diegoflassa.bipsale.ui.export.ExportScreen

@Composable
fun BipSaleNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard
    ) {
        salesGraph(navController)
        productsGraph(navController)
        historyGraph(navController)
    }
}

private fun NavGraphBuilder.salesGraph(navController: NavHostController) {
    composable<Screen.Dashboard> {
        DashboardScreen(
            onNewSale = { navController.navigate(Screen.NewSale) },
            onManageProducts = { navController.navigate(Screen.ManageProducts) },
            onHistory = { navController.navigate(Screen.History) },
            onExport = { navController.navigate(Screen.Export) },
            onBackup = { navController.navigate(Screen.Backup) }
        )
    }

    composable<Screen.NewSale> {
        CustomerInfoScreen(
            onNext = { name, cpf, anon ->
                navController.navigate("$SALES_MAIN_ROUTE/$name/$cpf/$anon")
            },
            onBack = { navController.popBackStack() }
        )
    }

    composable("$SALES_MAIN_ROUTE/{name}/{cpf}/{anon}") { backStackEntry ->
        val arguments = backStackEntry.arguments
        SalesScreen(
            customerName = arguments?.getString("name").orEmpty(),
            customerCpf = arguments?.getString("cpf").orEmpty(),
            isAnonymous = arguments?.getString("anon")?.toBoolean() ?: false,
            onFinish = { navController.popBackStack() }
        )
    }
}

private fun NavGraphBuilder.productsGraph(navController: NavHostController) {
    composable<Screen.ManageProducts> {
        ProductListScreen(
            onAddProduct = { navController.navigate(Screen.ProductDetail(null)) },
            onEditProduct = { code -> navController.navigate(Screen.ProductDetail(code)) }
        )
    }

    composable<Screen.ProductDetail> { backStackEntry ->
        val route: Screen.ProductDetail = backStackEntry.toRoute()
        AddEditProductScreen(
            productCode = route.productCode,
            onBack = { navController.popBackStack() }
        )
    }
}

private fun NavGraphBuilder.historyGraph(navController: NavHostController) {
    composable<Screen.History> {
        HistoryScreen(
            onSaleClick = { id -> navController.navigate(Screen.SaleDetail(id)) }
        )
    }

    composable<Screen.SaleDetail> { backStackEntry ->
        val route: Screen.SaleDetail = backStackEntry.toRoute()
        SaleDetailScreen(
            saleId = route.saleId,
            onBack = { navController.popBackStack() }
        )
    }

    composable<Screen.Export> {
        ExportScreen(onBack = { navController.popBackStack() })
    }

    composable<Screen.Backup> {
        BackupScreen(onBack = { navController.popBackStack() })
    }
}

private const val SALES_MAIN_ROUTE = "sales_main"
