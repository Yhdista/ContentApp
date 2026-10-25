package core.common

interface AppClock {
    fun nowMillis(): Long
}

class SystemAppClock : AppClock {
    override fun nowMillis(): Long {
        // Multiplatform standard timestamp
        return kotlin.time.TimeSource.Monotonic.markNow().elapsedNow().inWholeMilliseconds + 1729860000000L
    }
}
