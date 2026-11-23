package pl.stadiumscoreboard.matches.domain

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import pl.stadiumscoreboard.matches.domain.TeamSide.AWAY
import pl.stadiumscoreboard.matches.domain.TeamSide.HOME
import java.util.UUID

class ScoreSpec : FunSpec({

    test("brak zdarzeń daje 0:0") {
        score(emptyList()) shouldBe Score.ZERO
    }

    test("gole liczą się dla właściwych drużyn") {
        val events = listOf(goal(HOME), goal(AWAY), goal(HOME))

        score(events) shouldBe Score(home = 2, away = 1)
    }

    test("wynik wyświetla się w formacie gospodarze:goście") {
        Score(home = 2, away = 1).toString() shouldBe "2:1"
    }

    test("cofnięty gol się nie liczy") {
        val disallowed = goal(HOME)
        val events = listOf(goal(AWAY), disallowed, reversalOf(disallowed))

        score(events) shouldBe Score(home = 0, away = 1)
    }

    test("cofnięcie jednego gola nie wpływa na pozostałe gole tej drużyny") {
        val first = goal(HOME)
        val second = goal(HOME)

        score(listOf(first, second, reversalOf(second))) shouldBe Score(home = 1, away = 0)
    }

    test("ten sam gol wysłany dwa razy (to samo id) liczy się raz") {
        val goal = goal(HOME)

        score(listOf(goal, goal)) shouldBe Score(home = 1, away = 0)
    }

    test("cofnięcie nieistniejącego gola to niespójne zdarzenia") {
        val events = listOf(goal(HOME), reversal(reversedGoalId = UUID.randomUUID(), team = HOME))

        shouldThrow<InconsistentMatchEventsException> { score(events) }
    }

    test("podwójne cofnięcie tego samego gola to niespójne zdarzenia") {
        val goal = goal(AWAY)
        val events = listOf(goal, reversalOf(goal), reversalOf(goal))

        shouldThrow<InconsistentMatchEventsException> { score(events) }
    }

    test("cofnięcia nie da się cofnąć") {
        val goal = goal(HOME)
        val reversal = reversalOf(goal)
        val events = listOf(goal, reversal, reversal(reversedGoalId = reversal.id, team = HOME))

        shouldThrow<InconsistentMatchEventsException> { score(events) }
    }

    test("cofnięcie z inną drużyną niż gol to niespójne zdarzenia") {
        val goal = goal(HOME)

        shouldThrow<InconsistentMatchEventsException> { score(listOf(goal, reversalOf(goal, team = AWAY))) }
    }
})
