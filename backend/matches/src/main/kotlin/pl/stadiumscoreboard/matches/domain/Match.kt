package pl.stadiumscoreboard.matches.domain

import java.time.Instant
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/**
 * Mecz jest niemutowalny: każda operacja zwraca nową kopię, a oryginał się nie zmienia.
 * Walidacja w `init` działa też dla kopii tworzonych przez `copy`.
 *
 * Operacje zależne od czasu dostają `now` jako parametr: domena sama nie pyta systemu o aktualny czas.
 */
data class Match(
    val id: UUID,
    val homeTeam: String,
    val awayTeam: String,
    val kickoffAt: Instant,
    val periodLength: Duration = DEFAULT_PERIOD_LENGTH,
    val status: MatchStatus = MatchStatus.SCHEDULED,
    val clock: MatchClock = MatchClock.STOPPED,
    val addedMinutes: Map<Period, Int> = emptyMap(),
) {
    init {
        if (homeTeam.isBlank() || awayTeam.isBlank()) {
            throw InvalidMatchException("Nazwy drużyn nie mogą być puste")
        }
        if (homeTeam.trim().equals(awayTeam.trim(), ignoreCase = true)) {
            throw InvalidMatchException("Drużyna nie może grać sama ze sobą: $homeTeam")
        }
        if (!periodLength.isPositive()) {
            throw InvalidMatchException("Długość połowy musi być dodatnia, podano: $periodLength")
        }
        if (clock.running && !status.isInPlay) {
            throw InvalidMatchException("Zegar nie może chodzić w statusie $status")
        }
        addedMinutes.values.forEach { checkAddedMinutesInRange(it) }
    }

    val currentPeriod: Period?
        get() = when (status) {
            MatchStatus.FIRST_HALF -> Period.FIRST_HALF
            MatchStatus.SECOND_HALF -> Period.SECOND_HALF
            MatchStatus.SCHEDULED,
            MatchStatus.HALF_TIME,
            MatchStatus.FINISHED -> null
        }

    fun startFirstHalf(now: Instant): Match = when (status) {
        MatchStatus.SCHEDULED -> copy(status = MatchStatus.FIRST_HALF, clock = MatchClock.startedAt(now))
        else -> throw IllegalStatusTransitionException(status, "rozpoczęcie I połowy")
    }

    fun startSecondHalf(now: Instant): Match = when (status) {
        MatchStatus.HALF_TIME -> copy(status = MatchStatus.SECOND_HALF, clock = MatchClock.startedAt(now))
        else -> throw IllegalStatusTransitionException(status, "rozpoczęcie II połowy")
    }

    /** Rozpoczyna kolejną połowę: pierwszą przed meczem, drugą po przerwie. Zegar połowy startuje od zera. */
    fun startPeriod(now: Instant): Match = when (status) {
        MatchStatus.SCHEDULED -> startFirstHalf(now)
        MatchStatus.HALF_TIME -> startSecondHalf(now)
        MatchStatus.FIRST_HALF,
        MatchStatus.SECOND_HALF,
        MatchStatus.FINISHED -> throw IllegalStatusTransitionException(status, "rozpoczęcie połowy")
    }

    /** Kończy połowę i zatrzymuje zegar. Zegar zachowuje czas, w którym sędzia zakończył połowę. */
    fun endPeriod(now: Instant): Match = when (status) {
        MatchStatus.FIRST_HALF -> copy(status = MatchStatus.HALF_TIME, clock = clock.stop(now))
        MatchStatus.SECOND_HALF -> copy(status = MatchStatus.FINISHED, clock = clock.stop(now))
        MatchStatus.SCHEDULED,
        MatchStatus.HALF_TIME,
        MatchStatus.FINISHED -> throw IllegalStatusTransitionException(status, "zakończenie połowy")
    }

    fun pauseClock(now: Instant): Match {
        checkInPlay("pauza zegara")
        return copy(clock = clock.pause(now))
    }

    fun resumeClock(now: Instant): Match {
        checkInPlay("wznowienie zegara")
        return copy(clock = clock.start(now))
    }

    fun adjustClock(newElapsed: Duration, now: Instant): Match {
        checkInPlay("korekta zegara")
        return copy(clock = clock.adjust(newElapsed, now))
    }

    /** Ogłasza doliczony czas połowy. Ponowne ogłoszenie nadpisuje poprzednią wartość. */
    fun announceAddedTime(period: Period, minutes: Int): Match {
        if (currentPeriod != period) {
            throw IllegalStatusTransitionException(status, "ogłoszenie doliczonego czasu dla $period")
        }
        checkAddedMinutesInRange(minutes)
        return copy(addedMinutes = addedMinutes + (period to minutes))
    }

    private fun checkInPlay(action: String) {
        if (!status.isInPlay) throw IllegalStatusTransitionException(status, action)
    }

    companion object {
        val DEFAULT_PERIOD_LENGTH: Duration = 45.minutes
        val ADDED_MINUTES_RANGE: IntRange = 0..15

        private fun checkAddedMinutesInRange(minutes: Int) {
            if (minutes !in ADDED_MINUTES_RANGE) {
                throw InvalidAddedTimeException("Doliczony czas musi mieścić się w zakresie $ADDED_MINUTES_RANGE min, podano: $minutes")
            }
        }
    }
}
