package core.platform

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context

class AndroidPlatformClipboard(
    private val context: Context
) : PlatformClipboard {
    override fun copyToClipboard(label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard?.setPrimaryClip(clip)
    }
}
