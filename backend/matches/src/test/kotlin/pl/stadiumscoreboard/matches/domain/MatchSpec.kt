package pl.stadiumscoreboard.matches.domain

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import pl.stadiumscoreboard.matches.domain.MatchStatus.FINISHED
import pl.stadiumscoreboard.matches.domain.MatchStatus.FIRST_HALF
import pl.stadiumscoreboard.matches.domain.MatchStatus.HALF_TIME
import pl.stadiumscoreboard.matches.domain.MatchStatus.SCHEDULED
import pl.stadiumscoreboard.matches.domain.MatchStatus.SECOND_HALF
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

class MatchSpec : FunSpec({

    val now = KICKOFF

    context("tworzenie meczu") {
        test("nowy mecz jest zaplanowany, ma połowy po 45 minut i zatrzymany zegar") {
            val match = aMatch()

            match.status shouldBe SCHEDULED
            match.periodLength shouldBe 45.minutes
            match.clock shouldBe MatchClock.STOPPED
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

        test("zegar nie może chodzić poza grą") {
            shouldThrow<InvalidMatchException> { aMatch(HALF_TIME).copy(clock = MatchClock.startedAt(now)) }
        }
    }

    context("poprawny przebieg meczu") {
        test("SCHEDULED → FIRST_HALF → HALF_TIME → SECOND_HALF → FINISHED") {
            val scheduled = aMatch()
            val firstHalf = scheduled.startPeriod(now)
            val halfTime = firstHalf.endPeriod(now)
            val secondHalf = halfTime.startPeriod(now)
            val finished = secondHalf.endPeriod(now)

            listOf(scheduled, firstHalf, halfTime, secondHalf, finished).map { it.status } shouldBe
                listOf(SCHEDULED, FIRST_HALF, HALF_TIME, SECOND_HALF, FINISHED)
        }

        test("startFirstHalf i startSecondHalf działają jak startPeriod") {
            aMatch(SCHEDULED).startFirstHalf(now).status shouldBe FIRST_HALF
            aMatch(HALF_TIME).startSecondHalf(now).status shouldBe SECOND_HALF
        }

        test("przejście zwraca nową kopię, a oryginał się nie zmienia") {
            val scheduled = aMatch()

            val started = scheduled.startFirstHalf(now)

            scheduled.status shouldBe SCHEDULED
            started.status shouldBe FIRST_HALF
        }

        test("start połowy uruchamia zegar od zera") {
            val time = MutableClock(KICKOFF)
            val firstHalf = aMatch().startPeriod(time.instant())
            time.advanceBy(47.minutes)
            val halfTime = firstHalf.endPeriod(time.instant())
            time.advanceBy(15.minutes)

            val secondHalf = halfTime.startPeriod(time.instant())

            secondHalf.clock.running shouldBe true
            secondHalf.clock.elapsed(time.instant()) shouldBe Duration.ZERO
        }

        test("koniec połowy zatrzymuje zegar w chwili gwizdka") {
            val time = MutableClock(KICKOFF)
            val firstHalf = aMatch().startPeriod(time.instant())
            time.advanceBy(47.minutes)

            val halfTime = firstHalf.endPeriod(time.instant())
            time.advanceBy(10.minutes)

            halfTime.clock.running shouldBe false
            halfTime.clock.elapsed(time.instant()) shouldBe 47.minutes
        }

        test("połowę można zakończyć także przy zatrzymanym zegarze") {
            val paused = aMatch(FIRST_HALF).pauseClock(now)

            paused.endPeriod(now).status shouldBe HALF_TIME
        }
    }

    context("niedozwolone przejścia rzucają wyjątek domenowy") {
        val notAllowedToStartFirstHalf = MatchStatus.entries - SCHEDULED
        notAllowedToStartFirstHalf.forEach { status ->
            test("startFirstHalf w statusie $status") {
                val exception = shouldThrow<IllegalStatusTransitionException> { aMatch(status).startFirstHalf(now) }
                exception.currentStatus shouldBe status
            }
        }

        val notAllowedToStartSecondHalf = MatchStatus.entries - HALF_TIME
        notAllowedToStartSecondHalf.forEach { status ->
            test("startSecondHalf w statusie $status") {
                shouldThrow<IllegalStatusTransitionException> { aMatch(status).startSecondHalf(now) }
            }
        }

        listOf(FIRST_HALF, SECOND_HALF, FINISHED).forEach { status ->
            test("startPeriod w statusie $status") {
                shouldThrow<IllegalStatusTransitionException> { aMatch(status).startPeriod(now) }
            }
        }

        listOf(SCHEDULED, HALF_TIME, FINISHED).forEach { status ->
            test("endPeriod w statusie $status") {
                shouldThrow<IllegalStatusTransitionException> { aMatch(status).endPeriod(now) }
            }
        }
    }

    context("sterowanie zegarem w trakcie gry") {
        test("pauza i wznowienie w trakcie połowy") {
            val time = MutableClock(KICKOFF)
            val match = aMatch(FIRST_HALF)
            time.advanceBy(10.minutes)
            val paused = match.pauseClock(time.instant())
            time.advanceBy(2.minutes)
            val resumed = paused.resumeClock(time.instant())
            time.advanceBy(5.minutes)

            resumed.clock.elapsed(time.instant()) shouldBe 15.minutes
        }

        test("korekta zegara w trakcie połowy") {
            aMatch(SECOND_HALF).adjustClock(30.minutes, now).clock.elapsed(now) shouldBe 30.minutes
        }

        listOf(SCHEDULED, HALF_TIME, FINISHED).forEach { status ->
            test("poza grą nie da się pauzować, wznawiać ani korygować zegara ($status)") {
                val match = aMatch(status)

                shouldThrow<IllegalStatusTransitionException> { match.pauseClock(now) }
                shouldThrow<IllegalStatusTransitionException> { match.resumeClock(now) }
                shouldThrow<IllegalStatusTransitionException> { match.adjustClock(10.minutes, now) }
            }
        }
    }

    context("doliczony czas") {
        test("w trakcie I połowy można ogłosić doliczony czas I połowy") {
            aMatch(FIRST_HALF).announceAddedTime(Period.FIRST_HALF, 3).addedMinutes shouldBe
                mapOf(Period.FIRST_HALF to 3)
        }

        test("ponowne ogłoszenie nadpisuje poprzednią wartość") {
            val match = aMatch(FIRST_HALF)
                .announceAddedTime(Period.FIRST_HALF, 2)
                .announceAddedTime(Period.FIRST_HALF, 4)

            match.addedMinutes[Period.FIRST_HALF] shouldBe 4
        }

        test("doliczony czas I połowy zostaje zapamiętany po jej końcu") {
            val match = aMatch(FIRST_HALF).announceAddedTime(Period.FIRST_HALF, 2).endPeriod(now)

            match.addedMinutes[Period.FIRST_HALF] shouldBe 2
            match.addedMinutes[Period.SECOND_HALF].shouldBeNull()
        }

        test("zakres 0–15 minut jest akceptowany na brzegach") {
            aMatch(SECOND_HALF).announceAddedTime(Period.SECOND_HALF, 0).addedMinutes[Period.SECOND_HALF] shouldBe 0
            aMatch(SECOND_HALF).announceAddedTime(Period.SECOND_HALF, 15).addedMinutes[Period.SECOND_HALF] shouldBe 15
        }

        listOf(-1, 16).forEach { minutes ->
            test("$minutes min jest poza zakresem") {
                shouldThrow<InvalidAddedTimeException> { aMatch(FIRST_HALF).announceAddedTime(Period.FIRST_HALF, minutes) }
            }
        }

        test("nie da się ogłosić doliczonego czasu innej połowy niż trwająca") {
            shouldThrow<IllegalStatusTransitionException> {
                aMatch(SECOND_HALF).announceAddedTime(Period.FIRST_HALF, 2)
            }
        }

        listOf(SCHEDULED, HALF_TIME, FINISHED).forEach { status ->
            test("poza grą nie da się ogłosić doliczonego czasu ($status)") {
                shouldThrow<IllegalStatusTransitionException> { aMatch(status).announceAddedTime(Period.FIRST_HALF, 2) }
            }
        }
    }

    test("gra toczy się tylko w trakcie połów") {
        MatchStatus.entries.filter { it.isInPlay } shouldBe listOf(FIRST_HALF, SECOND_HALF)
    }
})
