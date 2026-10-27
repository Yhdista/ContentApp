package core.common

interface AppClock {
    fun nowMillis(): Long
    fun timeZoneOffsetMillis(): Long = 0L
}

class SystemAppClock(
    private val timeProvider: () -> Long = { kotlin.time.TimeSource.Monotonic.markNow().elapsedNow().inWholeMilliseconds + 1729860000000L },
    private val offsetProvider: () -> Long = { 0L }
) : AppClock {
    override fun nowMillis(): Long = timeProvider()
    override fun timeZoneOffsetMillis(): Long = offsetProvider()
}
