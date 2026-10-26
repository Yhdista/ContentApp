package core.platform

interface PlatformClipboard {
    fun copyToClipboard(label: String, text: String)
}

class NoOpPlatformClipboard : PlatformClipboard {
    override fun copyToClipboard(label: String, text: String) {}
}
