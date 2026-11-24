package pl.stadiumscoreboard.common.domain

import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Strefa czasowa stadionu. W systemie trzymamy czas jako [Instant] (UTC, jednoznaczny),
 * a na czas lokalny zamieniamy dopiero przy wyświetlaniu.
 */
val STADIUM_ZONE: ZoneId = ZoneId.of("Europe/Warsaw")

fun Instant.atStadium(): ZonedDateTime = atZone(STADIUM_ZONE)
