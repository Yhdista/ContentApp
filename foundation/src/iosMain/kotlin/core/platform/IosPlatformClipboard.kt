package core.platform

import platform.UIKit.UIPasteboard

class IosPlatformClipboard : PlatformClipboard {
    override fun copyToClipboard(label: String, text: String) {
        UIPasteboard.generalPasteboard.string = text
    }
}
