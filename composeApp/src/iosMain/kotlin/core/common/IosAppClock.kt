package core.common

import platform.Foundation.NSDate
import platform.Foundation.NSTimeZone
import platform.Foundation.localTimeZone
import platform.Foundation.timeIntervalSince1970

class IosAppClock : AppClock {
    override fun nowMillis(): Long = (NSDate().timeIntervalSince1970 * 1000.0).toLong()
    override fun timeZoneOffsetMillis(): Long =
        (NSTimeZone.localTimeZone.secondsFromGMT * 1000).toLong()
}
