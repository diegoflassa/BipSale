package dev.diegoflassa.bipsale.core.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import dev.diegoflassa.bipsale.core.ui.R
import dev.diegoflassa.bipsale.core.ui.theme.BipSaleTheme

/**
 * The app's one header. Every screen uses it so the back affordance sits in the same place with
 * the same content description, which is what a TalkBack user navigates by.
 *
 * [navigationIcon] wins over [onBack] — a screen in selection mode replaces the arrow with a
 * "clear selection" control rather than growing a second one.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BipSaleTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        modifier = modifier,
        title = {
            Text(text = title, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        navigationIcon = {
            when {
                navigationIcon != null -> navigationIcon()
                onBack != null -> IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.common_back)
                    )
                }
            }
        },
        actions = actions
    )
}

// region Previews

private const val PREVIEW_TITLE = "Histórico de Vendas"
private const val PREVIEW_LONG_TITLE =
    "Padaria e Confeitaria Gourmet do Centro — Relatórios Consolidados"

@Preview(name = "BipSaleTopAppBar · Com voltar · Phone", showBackground = true, locale = "pt", device = "spec:width=1080px,height=2520px,dpi=420")
@Preview(name = "BipSaleTopAppBar · Com voltar · Tablet", showBackground = true, locale = "pt", device = "spec:width=1200px,height=2000px,dpi=240")
@Composable
private fun BipSaleTopAppBarWithBackPreview() {
    BipSaleTheme {
        BipSaleTopAppBar(title = PREVIEW_TITLE, onBack = {})
    }
}

@Preview(name = "BipSaleTopAppBar · Sem voltar · Phone", showBackground = true, locale = "pt", device = "spec:width=1080px,height=2520px,dpi=420")
@Preview(name = "BipSaleTopAppBar · Sem voltar · Tablet", showBackground = true, locale = "pt", device = "spec:width=1200px,height=2000px,dpi=240")
@Composable
private fun BipSaleTopAppBarRootPreview() {
    BipSaleTheme {
        BipSaleTopAppBar(title = "BipSale")
    }
}

@Preview(name = "BipSaleTopAppBar · Seleção · Phone", showBackground = true, locale = "pt", device = "spec:width=1080px,height=2520px,dpi=420")
@Preview(name = "BipSaleTopAppBar · Seleção · Tablet", showBackground = true, locale = "pt", device = "spec:width=1200px,height=2000px,dpi=240")
@Composable
private fun BipSaleTopAppBarSelectionPreview() {
    BipSaleTheme {
        BipSaleTopAppBar(
            title = "3 selecionados",
            onBack = {},
            navigationIcon = {
                IconButton(onClick = {}) {
                    Icon(Icons.Default.Close, contentDescription = "Limpar seleção")
                }
            }
        )
    }
}

@Preview(name = "BipSaleTopAppBar · Título longo · Phone", showBackground = true, locale = "pt", device = "spec:width=1080px,height=2520px,dpi=420")
@Preview(name = "BipSaleTopAppBar · Título longo · Tablet", showBackground = true, locale = "pt", device = "spec:width=1200px,height=2000px,dpi=240")
@Composable
private fun BipSaleTopAppBarLongTitlePreview() {
    BipSaleTheme {
        BipSaleTopAppBar(title = PREVIEW_LONG_TITLE, onBack = {})
    }
}

@Preview(name = "BipSaleTopAppBar · Com voltar · Phone · Dark", showBackground = true, locale = "pt", device = "spec:width=1080px,height=2520px,dpi=420", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun BipSaleTopAppBarWithBackDarkPreview() {
    BipSaleTheme {
        BipSaleTopAppBar(title = PREVIEW_TITLE, onBack = {})
    }
}

// endregion
