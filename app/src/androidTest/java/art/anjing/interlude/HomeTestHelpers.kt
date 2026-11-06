package art.anjing.interlude

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.onNodeWithContentDescription

internal fun SemanticsNodeInteractionsProvider.appTile(label: String): SemanticsNodeInteraction =
    onNodeWithContentDescription("Open $label")
