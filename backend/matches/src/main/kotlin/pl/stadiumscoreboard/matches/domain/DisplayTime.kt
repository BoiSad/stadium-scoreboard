package pl.stadiumscoreboard.matches.domain

import java.time.Instant
import kotlin.time.Duration

/**
 * Czas meczu do pokazania na ekranie.
 *
 * - W regulaminowym czasie (włącznie z równo 45:00): `mm:ss` od początku meczu.
 *   II połowa zaczyna się od długości połowy, np. `45:00`.
 * - Po przekroczeniu: trwająca minuta doliczonego czasu. Tak jak w piłce nożnej, zegar 45:00–45:59
 *   to `45+1'`, 46:00–46:59 to `45+2'` itd.
 * - Poza grą: początek meczu, koniec I połowy albo koniec meczu.
 */
fun displayTime(match: Match, now: Instant): String {
    val periodLength = match.periodLength
    return when (match.status) {
        MatchStatus.SCHEDULED -> formatClock(Duration.ZERO)
        MatchStatus.FIRST_HALF -> periodTime(Duration.ZERO, periodLength, match.clock.elapsed(now))
        MatchStatus.HALF_TIME -> formatClock(periodLength)
        MatchStatus.SECOND_HALF -> periodTime(periodLength, periodLength, match.clock.elapsed(now))
        MatchStatus.FINISHED -> formatClock(periodLength * 2)
    }
}

private fun periodTime(periodStart: Duration, periodLength: Duration, elapsed: Duration): String {
    if (elapsed <= periodLength) {
        return formatClock(periodStart + elapsed)
    }
    val regulationEndMinute = (periodStart + periodLength).inWholeMinutes
    val addedMinute = (elapsed - periodLength).inWholeMinutes + 1
    return "$regulationEndMinute+$addedMinute'"
}

private fun formatClock(time: Duration): String {
    val minutes = time.inWholeMinutes.toString().padStart(2, '0')
    val seconds = (time.inWholeSeconds % 60).toString().padStart(2, '0')
    return "$minutes:$seconds"
}
