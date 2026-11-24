package pl.stadiumscoreboard.matches.domain

import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.time.Duration
import kotlin.time.toJavaDuration

/** Zegar testowy, który stoi w miejscu, dopóki test sam go nie przesunie. */
class MutableClock(
    private var current: Instant,
    private val zoneId: ZoneId = ZoneOffset.UTC,
) : Clock() {

    override fun instant(): Instant = current

    override fun getZone(): ZoneId = zoneId

    override fun withZone(zone: ZoneId): Clock = MutableClock(current, zone)

    fun advanceBy(duration: Duration) {
        current = current.plus(duration.toJavaDuration())
    }
}
