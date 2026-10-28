package core.ui

import core.result.DataError
import kotlin.test.Test
import kotlin.test.assertTrue

class UiTextTest {
    @Test fun storageAndNetworkFailuresHaveReadableMessages() {
        (DataError.Local.entries + DataError.Network.entries).forEach { assertTrue(it.toUiText().asString().isNotBlank()) }
    }
}
