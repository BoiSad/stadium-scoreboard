package pl.stadiumscoreboard.matches.domain

import java.time.Instant
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/**
 * Mecz jest niemutowalny: każda operacja zwraca nową kopię, a oryginał się nie zmienia.
 * Walidacja w `init` działa też dla kopii tworzonych przez `copy`.
 */
data class Match(
    val id: UUID,
    val homeTeam: String,
    val awayTeam: String,
    val kickoffAt: Instant,
    val periodLength: Duration = DEFAULT_PERIOD_LENGTH,
    val status: MatchStatus = MatchStatus.SCHEDULED,
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
    }

    fun startFirstHalf(): Match = when (status) {
        MatchStatus.SCHEDULED -> copy(status = MatchStatus.FIRST_HALF)
        else -> throw IllegalStatusTransitionException(status, "rozpoczęcie I połowy")
    }

    fun startSecondHalf(): Match = when (status) {
        MatchStatus.HALF_TIME -> copy(status = MatchStatus.SECOND_HALF)
        else -> throw IllegalStatusTransitionException(status, "rozpoczęcie II połowy")
    }

    /** Rozpoczyna kolejną połowę: pierwszą przed meczem, drugą po przerwie. */
    fun startPeriod(): Match = when (status) {
        MatchStatus.SCHEDULED -> startFirstHalf()
        MatchStatus.HALF_TIME -> startSecondHalf()
        MatchStatus.FIRST_HALF,
        MatchStatus.SECOND_HALF,
        MatchStatus.FINISHED -> throw IllegalStatusTransitionException(status, "rozpoczęcie połowy")
    }

    fun endPeriod(): Match = when (status) {
        MatchStatus.FIRST_HALF -> copy(status = MatchStatus.HALF_TIME)
        MatchStatus.SECOND_HALF -> copy(status = MatchStatus.FINISHED)
        MatchStatus.SCHEDULED,
        MatchStatus.HALF_TIME,
        MatchStatus.FINISHED -> throw IllegalStatusTransitionException(status, "zakończenie połowy")
    }

    companion object {
        val DEFAULT_PERIOD_LENGTH: Duration = 45.minutes
    }
}
