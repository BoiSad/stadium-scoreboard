package pl.stadiumscoreboard.matches.domain

import java.time.Instant
import java.util.UUID

/**
 * Zdarzenie w meczu. Zdarzeń nigdy nie usuwamy: pomyłkę naprawia nowe zdarzenie (cofnięcie).
 * `id` nadaje klient, więc ponowne wysłanie tego samego zdarzenia nie zdubluje gola.
 */
sealed interface MatchEvent {
    val id: UUID
    val matchId: UUID
    val team: TeamSide
    val occurredAt: Instant
    val matchMinute: String
}

data class Goal(
    override val id: UUID,
    override val matchId: UUID,
    override val team: TeamSide,
    override val occurredAt: Instant,
    override val matchMinute: String,
) : MatchEvent

data class GoalReversal(
    override val id: UUID,
    override val matchId: UUID,
    override val team: TeamSide,
    override val occurredAt: Instant,
    override val matchMinute: String,
    val reversedGoalId: UUID,
) : MatchEvent
