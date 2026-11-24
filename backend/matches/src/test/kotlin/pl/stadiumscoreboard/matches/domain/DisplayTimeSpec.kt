package pl.stadiumscoreboard.matches.domain

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class DisplayTimeSpec : FunSpec({

    /** Ekran w I połowie po [elapsed] od jej rozpoczęcia. */
    fun firstHalfAfter(elapsed: Duration, periodLength: Duration = 45.minutes): String {
        val time = MutableClock(KICKOFF)
        val match = aMatch().copy(periodLength = periodLength).startPeriod(time.instant())
        time.advanceBy(elapsed)
        return displayTime(match, time.instant())
    }

    /** Ekran w II połowie po [elapsed] od jej rozpoczęcia. */
    fun secondHalfAfter(elapsed: Duration, periodLength: Duration = 45.minutes): String {
        val time = MutableClock(KICKOFF)
        val match = aMatch().copy(periodLength = periodLength)
            .startPeriod(time.instant())
            .endPeriod(time.instant())
            .startPeriod(time.instant())
        time.advanceBy(elapsed)
        return displayTime(match, time.instant())
    }

    context("I połowa") {
        test("start połowy to 00:00") { firstHalfAfter(Duration.ZERO) shouldBe "00:00" }
        test("sekundy są pokazywane z zerem wiodącym") { firstHalfAfter(3.minutes + 7.seconds) shouldBe "03:07" }
        test("dokładnie 45:00 to jeszcze czas regulaminowy") { firstHalfAfter(45.minutes) shouldBe "45:00" }
        test("45:00 + 1 s to pierwsza doliczona minuta") { firstHalfAfter(45.minutes + 1.seconds) shouldBe "45+1'" }
        test("45:59 to wciąż pierwsza doliczona minuta") { firstHalfAfter(45.minutes + 59.seconds) shouldBe "45+1'" }
        test("46:00 to druga doliczona minuta") { firstHalfAfter(46.minutes) shouldBe "45+2'" }
    }

    context("II połowa") {
        test("zaczyna się od 45:00") { secondHalfAfter(Duration.ZERO) shouldBe "45:00" }
        test("po 30 minutach pokazuje 75:00") { secondHalfAfter(30.minutes) shouldBe "75:00" }
        test("dokładnie 90:00 to jeszcze czas regulaminowy") { secondHalfAfter(45.minutes) shouldBe "90:00" }
        test("90:00 + 1 s to pierwsza doliczona minuta") { secondHalfAfter(45.minutes + 1.seconds) shouldBe "90+1'" }
        test("kilka minut doliczonego czasu") { secondHalfAfter(49.minutes + 30.seconds) shouldBe "90+5'" }
    }

    context("połowy po 40 minut (juniorzy)") {
        test("I połowa przekracza czas po 40 minutach") { firstHalfAfter(40.minutes + 1.seconds, 40.minutes) shouldBe "40+1'" }
        test("II połowa zaczyna się od 40:00") { secondHalfAfter(Duration.ZERO, 40.minutes) shouldBe "40:00" }
        test("II połowa przekracza czas po 80 minutach") { secondHalfAfter(40.minutes + 1.seconds, 40.minutes) shouldBe "80+1'" }
    }

    context("poza grą") {
        test("przed meczem") { displayTime(aMatch(MatchStatus.SCHEDULED), KICKOFF) shouldBe "00:00" }
        test("w przerwie") { displayTime(aMatch(MatchStatus.HALF_TIME), KICKOFF) shouldBe "45:00" }
        test("po meczu") { displayTime(aMatch(MatchStatus.FINISHED), KICKOFF) shouldBe "90:00" }
    }

    test("pauza w doliczonym czasie zatrzymuje wyświetlaną minutę") {
        val time = MutableClock(KICKOFF)
        val match = aMatch().startPeriod(time.instant())
        time.advanceBy(46.minutes + 30.seconds)
        val paused = match.pauseClock(time.instant())

        time.advanceBy(5.minutes)

        displayTime(paused, time.instant()) shouldBe "45+2'"
    }

    test("korekta zegara zmienia wyświetlany czas, a zegar liczy dalej") {
        val time = MutableClock(KICKOFF)
        val match = aMatch().startPeriod(time.instant())
        time.advanceBy(12.minutes)

        val corrected = match.adjustClock(30.minutes, time.instant())
        time.advanceBy(1.minutes)

        displayTime(corrected, time.instant()) shouldBe "31:00"
    }

    test("korekta cofająca zegar z doliczonego czasu do regulaminowego") {
        val time = MutableClock(KICKOFF)
        val match = aMatch().startPeriod(time.instant())
        time.advanceBy(46.minutes)

        val corrected = match.adjustClock(44.minutes + 50.seconds, time.instant())

        displayTime(corrected, time.instant()) shouldBe "44:50"
    }
})
