package core.feedback

interface PlatformFeedback {
    fun vibrateTimerComplete()
    fun playTimerSound()
}

class NoOpPlatformFeedback : PlatformFeedback {
    override fun vibrateTimerComplete() {}
    override fun playTimerSound() {}
}
