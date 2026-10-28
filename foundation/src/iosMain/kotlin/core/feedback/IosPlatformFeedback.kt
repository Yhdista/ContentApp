package core.feedback

import platform.AudioToolbox.AudioServicesPlaySystemSound
import platform.AudioToolbox.kSystemSoundID_Vibrate

class IosPlatformFeedback : PlatformFeedback {

    override fun vibrateTimerComplete() {
        AudioServicesPlaySystemSound(kSystemSoundID_Vibrate)
    }

    override fun playTimerSound() {
        AudioServicesPlaySystemSound(1005u)
    }
}
