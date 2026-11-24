package pl.stadiumscoreboard.common.domain

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.time.Instant
import java.time.LocalDateTime

class StadiumTimeSpec : FunSpec({

    test("zimą Polska jest o 1 godzinę przed UTC") {
        Instant.parse("2026-12-06T17:00:00Z").atStadium().toLocalDateTime() shouldBe
            LocalDateTime.parse("2026-12-06T18:00:00")
    }

    test("latem Polska jest o 2 godziny przed UTC") {
        Instant.parse("2026-07-04T16:00:00Z").atStadium().toLocalDateTime() shouldBe
            LocalDateTime.parse("2026-07-04T18:00:00")
    }

    test("w nocy zmiany czasu na zimowy godzina 02:30 występuje dwa razy") {
        // 25.10.2026 o 03:00 czasu letniego zegary cofają się na 02:00.
        val firstTwoThirty = Instant.parse("2026-10-25T00:30:00Z").atStadium()
        val secondTwoThirty = Instant.parse("2026-10-25T01:30:00Z").atStadium()

        firstTwoThirty.toLocalDateTime() shouldBe secondTwoThirty.toLocalDateTime()
        firstTwoThirty.toInstant() shouldNotBe secondTwoThirty.toInstant()
    }

    test("w nocy zmiany czasu na letni godziny 02:00–02:59 nie istnieją") {
        // 29.03.2026 o 02:00 czasu zimowego zegary przesuwają się na 03:00.
        val nonExistent = LocalDateTime.parse("2026-03-29T02:30:00")

        nonExistent.atZone(STADIUM_ZONE).toLocalDateTime() shouldBe LocalDateTime.parse("2026-03-29T03:30:00")
    }
})
