package pl.stadiumscoreboard.matches.domain

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.time.Clock
import java.time.ZoneOffset
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

class MatchClockSpec : FunSpec({

    test("nowy zegar stoi i pokazuje zero") {
        MatchClock.STOPPED.running shouldBe false
        MatchClock.STOPPED.elapsed(KICKOFF) shouldBe Duration.ZERO
    }

    test("Clock.fixed: zegar uruchomiony 10 minut temu pokazuje 10 minut") {
        val tenMinutesLater = Clock.fixed(KICKOFF.plus(10.minutes.toJavaDuration()), ZoneOffset.UTC)

        MatchClock.startedAt(KICKOFF).elapsed(tenMinutesLater.instant()) shouldBe 10.minutes
    }

    test("start → 10 min → pauza → 2 min → wznowienie → 5 min daje 15 minut") {
        val time = MutableClock(KICKOFF)
        var clock = MatchClock.STOPPED.start(time.instant())

        time.advanceBy(10.minutes)
        clock = clock.pause(time.instant())
        time.advanceBy(2.minutes)
        clock = clock.start(time.instant())
        time.advanceBy(5.minutes)

        clock.elapsed(time.instant()) shouldBe 15.minutes
    }

    test("zatrzymany zegar nie liczy czasu") {
        val time = MutableClock(KICKOFF)
        val running = MatchClock.startedAt(time.instant())
        time.advanceBy(3.minutes)
        val paused = running.pause(time.instant())

        time.advanceBy(30.minutes)

        paused.elapsed(time.instant()) shouldBe 3.minutes
    }

    test("czas sprzed startu (rozjechane zegary) nie daje ujemnego wyniku") {
        val clock = MatchClock(startedAt = KICKOFF, elapsedBeforeStart = 5.minutes)

        clock.elapsed(KICKOFF.minusSeconds(30)) shouldBe 5.minutes
    }

    context("korekta zegara") {
        test("korekta chodzącego zegara ustawia czas i liczy dalej od chwili korekty") {
            val time = MutableClock(KICKOFF)
            val clock = MatchClock.startedAt(time.instant())
            time.advanceBy(12.minutes)

            val adjusted = clock.adjust(20.minutes, time.instant())
            time.advanceBy(1.minutes)

            adjusted.running shouldBe true
            adjusted.elapsed(time.instant()) shouldBe 21.minutes
        }

        test("korekta zatrzymanego zegara nie uruchamia go") {
            val adjusted = MatchClock.STOPPED.adjust(30.minutes + 15.seconds, KICKOFF)

            adjusted.running shouldBe false
            adjusted.elapsed(KICKOFF.plusSeconds(600)) shouldBe 30.minutes + 15.seconds
        }

        test("ujemny czas jest odrzucany") {
            shouldThrow<IllegalClockOperationException> { MatchClock.STOPPED.adjust((-1).seconds, KICKOFF) }
        }
    }

    context("niedozwolone operacje") {
        test("nie da się uruchomić chodzącego zegara") {
            shouldThrow<IllegalClockOperationException> { MatchClock.startedAt(KICKOFF).start(KICKOFF) }
        }

        test("nie da się zatrzymać zatrzymanego zegara") {
            shouldThrow<IllegalClockOperationException> { MatchClock.STOPPED.pause(KICKOFF) }
        }

        test("stop działa także na zatrzymanym zegarze") {
            MatchClock.STOPPED.stop(KICKOFF) shouldBe MatchClock.STOPPED
        }
    }
})
