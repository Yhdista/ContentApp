package core.platform

import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

class DesktopPlatformClipboard : PlatformClipboard {
    override fun copyToClipboard(label: String, text: String) {
        runCatching {
            val selection = StringSelection(text)
            Toolkit.getDefaultToolkit().systemClipboard.setContents(selection, selection)
        }
    }
}
