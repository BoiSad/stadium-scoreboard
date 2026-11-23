package pl.stadiumscoreboard.matches.domain

import java.time.Instant
import java.util.UUID

val MATCH_ID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000001")
private val KICKOFF: Instant = Instant.parse("2026-12-06T18:00:00Z")

fun aMatch(status: MatchStatus = MatchStatus.SCHEDULED): Match = Match(
    id = MATCH_ID,
    homeTeam = "Lech Poznań",
    awayTeam = "Legia Warszawa",
    kickoffAt = KICKOFF,
    status = status,
)

fun goal(team: TeamSide, id: UUID = UUID.randomUUID()): Goal = Goal(
    id = id,
    matchId = MATCH_ID,
    team = team,
    occurredAt = KICKOFF,
    matchMinute = "10'",
)

fun reversalOf(goal: Goal, team: TeamSide = goal.team): GoalReversal = reversal(goal.id, team)

fun reversal(reversedGoalId: UUID, team: TeamSide): GoalReversal = GoalReversal(
    id = UUID.randomUUID(),
    matchId = MATCH_ID,
    team = team,
    occurredAt = KICKOFF,
    matchMinute = "12'",
    reversedGoalId = reversedGoalId,
)
