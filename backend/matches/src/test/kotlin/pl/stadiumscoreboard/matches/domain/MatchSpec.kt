package pl.stadiumscoreboard.matches.domain

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import pl.stadiumscoreboard.matches.domain.MatchStatus.FINISHED
import pl.stadiumscoreboard.matches.domain.MatchStatus.FIRST_HALF
import pl.stadiumscoreboard.matches.domain.MatchStatus.HALF_TIME
import pl.stadiumscoreboard.matches.domain.MatchStatus.SCHEDULED
import pl.stadiumscoreboard.matches.domain.MatchStatus.SECOND_HALF
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

class MatchSpec : FunSpec({

    context("tworzenie meczu") {
        test("nowy mecz jest zaplanowany i ma połowy po 45 minut") {
            val match = aMatch()

            match.status shouldBe SCHEDULED
            match.periodLength shouldBe 45.minutes
        }

        test("można ustawić krótsze połowy, np. dla juniorów") {
            aMatch().copy(periodLength = 40.minutes).periodLength shouldBe 40.minutes
        }

        test("pusta nazwa drużyny jest odrzucana") {
            shouldThrow<InvalidMatchException> { aMatch().copy(homeTeam = "  ") }
        }

        test("drużyna nie może grać sama ze sobą, bez względu na wielkość liter i spacje") {
            shouldThrow<InvalidMatchException> { aMatch().copy(homeTeam = "Lech", awayTeam = " lech ") }
        }

        test("długość połowy musi być dodatnia") {
            shouldThrow<InvalidMatchException> { aMatch().copy(periodLength = Duration.ZERO) }
        }
    }

    context("poprawny przebieg meczu") {
        test("SCHEDULED → FIRST_HALF → HALF_TIME → SECOND_HALF → FINISHED") {
            val scheduled = aMatch()
            val firstHalf = scheduled.startPeriod()
            val halfTime = firstHalf.endPeriod()
            val secondHalf = halfTime.startPeriod()
            val finished = secondHalf.endPeriod()

            listOf(scheduled, firstHalf, halfTime, secondHalf, finished).map { it.status } shouldBe
                listOf(SCHEDULED, FIRST_HALF, HALF_TIME, SECOND_HALF, FINISHED)
        }

        test("startFirstHalf i startSecondHalf działają jak startPeriod") {
            aMatch(SCHEDULED).startFirstHalf().status shouldBe FIRST_HALF
            aMatch(HALF_TIME).startSecondHalf().status shouldBe SECOND_HALF
        }

        test("przejście zwraca nową kopię, a oryginał się nie zmienia") {
            val scheduled = aMatch()

            val started = scheduled.startFirstHalf()

            scheduled.status shouldBe SCHEDULED
            started.status shouldBe FIRST_HALF
            started.copy(status = SCHEDULED) shouldBe scheduled
        }
    }

    context("niedozwolone przejścia rzucają wyjątek domenowy") {
        val notAllowedToStartFirstHalf = MatchStatus.entries - SCHEDULED
        notAllowedToStartFirstHalf.forEach { status ->
            test("startFirstHalf w statusie $status") {
                val exception = shouldThrow<IllegalStatusTransitionException> { aMatch(status).startFirstHalf() }
                exception.currentStatus shouldBe status
            }
        }

        val notAllowedToStartSecondHalf = MatchStatus.entries - HALF_TIME
        notAllowedToStartSecondHalf.forEach { status ->
            test("startSecondHalf w statusie $status") {
                shouldThrow<IllegalStatusTransitionException> { aMatch(status).startSecondHalf() }
            }
        }

        listOf(FIRST_HALF, SECOND_HALF, FINISHED).forEach { status ->
            test("startPeriod w statusie $status") {
                shouldThrow<IllegalStatusTransitionException> { aMatch(status).startPeriod() }
            }
        }

        listOf(SCHEDULED, HALF_TIME, FINISHED).forEach { status ->
            test("endPeriod w statusie $status") {
                shouldThrow<IllegalStatusTransitionException> { aMatch(status).endPeriod() }
            }
        }
    }

    test("gra toczy się tylko w trakcie połów") {
        MatchStatus.entries.filter { it.isInPlay } shouldBe listOf(FIRST_HALF, SECOND_HALF)
    }
})
