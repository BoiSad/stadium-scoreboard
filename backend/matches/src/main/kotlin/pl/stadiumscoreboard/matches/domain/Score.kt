package pl.stadiumscoreboard.matches.domain

data class Score(val home: Int, val away: Int) {

    override fun toString(): String = "$home:$away"

    companion object {
        val ZERO = Score(home = 0, away = 0)
    }
}

/**
 * Liczy wynik z listy zdarzeń meczu. Gol cofnięty przez [GoalReversal] się nie liczy.
 *
 * Zdarzenia trafiają do listy dopiero po walidacji, więc niespójna lista (cofnięcie
 * nieistniejącego gola, podwójne cofnięcie, cofnięcie z inną drużyną) oznacza błąd w systemie.
 * Wtedy rzucamy wyjątek, zamiast po cichu pokazać na telebimie zły wynik.
 */
fun score(events: List<MatchEvent>): Score {
    val goalsById = events.filterIsInstance<Goal>().associateBy { it.id }
    val reversals = events.filterIsInstance<GoalReversal>()

    reversals.forEach { reversal -> checkReversalMatchesGoal(reversal, goalsById[reversal.reversedGoalId]) }
    checkNoGoalReversedTwice(reversals)

    val reversedGoalIds = reversals.map { it.reversedGoalId }.toSet()
    val countedGoals = goalsById.values.filterNot { it.id in reversedGoalIds }

    return Score(
        home = countedGoals.count { it.team == TeamSide.HOME },
        away = countedGoals.count { it.team == TeamSide.AWAY },
    )
}

private fun checkReversalMatchesGoal(reversal: GoalReversal, reversedGoal: Goal?) {
    if (reversedGoal == null) {
        throw InconsistentMatchEventsException(
            "Cofnięcie ${reversal.id} wskazuje na gola ${reversal.reversedGoalId}, którego nie ma w meczu",
        )
    }
    if (reversedGoal.team != reversal.team) {
        throw InconsistentMatchEventsException(
            "Cofnięcie ${reversal.id} dotyczy drużyny ${reversal.team}, a gol ${reversedGoal.id} padł dla ${reversedGoal.team}",
        )
    }
}

private fun checkNoGoalReversedTwice(reversals: List<GoalReversal>) {
    val goalIdsReversedTwice = reversals
        .groupingBy { it.reversedGoalId }
        .eachCount()
        .filterValues { it > 1 }
        .keys

    if (goalIdsReversedTwice.isNotEmpty()) {
        throw InconsistentMatchEventsException("Gol cofnięty więcej niż raz: ${goalIdsReversedTwice.joinToString()}")
    }
}
