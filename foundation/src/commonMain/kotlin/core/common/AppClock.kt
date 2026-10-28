package core.common

interface AppClock {
    fun nowMillis(): Long
    fun timeZoneOffsetMillis(): Long = 0L
}

@OptIn(kotlin.time.ExperimentalTime::class)
class SystemAppClock(
    private val timeProvider: () -> Long = { kotlin.time.Clock.System.now().toEpochMilliseconds() },
    private val offsetProvider: () -> Long = { 0L }
) : AppClock {
    override fun nowMillis(): Long = timeProvider()
    override fun timeZoneOffsetMillis(): Long = offsetProvider()
}
