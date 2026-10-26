package core.feedback

import java.awt.Toolkit

class DesktopPlatformFeedback : PlatformFeedback {

    override fun vibrateTimerComplete() {
        try {
            Toolkit.getDefaultToolkit().beep()
        } catch (_: Throwable) {
            // Silently ignore
        }
    }

    override fun playTimerSound() {
        try {
            Toolkit.getDefaultToolkit().beep()
        } catch (_: Throwable) {
            // Silently ignore
        }
    }
}
