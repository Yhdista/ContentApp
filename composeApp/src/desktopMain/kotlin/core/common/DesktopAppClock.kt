package core.common

import java.util.TimeZone

class DesktopAppClock : AppClock {
    override fun nowMillis(): Long = System.currentTimeMillis()
    override fun timeZoneOffsetMillis(): Long =
        TimeZone.getDefault().getOffset(System.currentTimeMillis()).toLong()
}
