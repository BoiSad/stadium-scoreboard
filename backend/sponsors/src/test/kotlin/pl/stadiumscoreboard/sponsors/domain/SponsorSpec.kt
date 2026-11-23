package pl.stadiumscoreboard.sponsors.domain

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import java.util.UUID

private fun aSponsor(name: String, position: Int = 0, active: Boolean = true) = Sponsor(
    id = UUID.randomUUID(),
    name = name,
    imagePath = "sponsors/${name.lowercase()}.png",
    displaySeconds = 10,
    position = position,
    active = active,
)

class SponsorSpec : FunSpec({

    context("walidacja sponsora") {
        test("pusta nazwa jest odrzucana") {
            shouldThrow<InvalidSponsorException> { aSponsor(name = " ") }
        }

        test("czas wyświetlania musi być dodatni") {
            shouldThrow<InvalidSponsorException> { aSponsor(name = "Orlen").copy(displaySeconds = 0) }
        }

        test("pozycja nie może być ujemna") {
            shouldThrow<InvalidSponsorException> { aSponsor(name = "Orlen", position = -1) }
        }
    }

    context("aktywni sponsorzy w kolejności wyświetlania") {
        test("pusta lista daje pustą listę") {
            activeSponsorsInDisplayOrder(emptyList()).shouldBeEmpty()
        }

        test("nieaktywni sponsorzy są pomijani") {
            val sponsors = listOf(
                aSponsor(name = "Orlen"),
                aSponsor(name = "PZU", active = false),
            )

            activeSponsorsInDisplayOrder(sponsors).map { it.name } shouldBe listOf("Orlen")
        }

        test("sponsorzy są posortowani po pozycji") {
            val sponsors = listOf(
                aSponsor(name = "A", position = 3),
                aSponsor(name = "B", position = 1),
                aSponsor(name = "C", position = 2),
            )

            activeSponsorsInDisplayOrder(sponsors).map { it.name } shouldBe listOf("B", "C", "A")
        }

        test("przy tej samej pozycji decyduje nazwa") {
            val sponsors = listOf(
                aSponsor(name = "Żabka", position = 1),
                aSponsor(name = "Allegro", position = 1),
                aSponsor(name = "Orlen", position = 0),
            )

            activeSponsorsInDisplayOrder(sponsors).map { it.name } shouldBe listOf("Orlen", "Allegro", "Żabka")
        }

        test("oryginalna lista się nie zmienia") {
            val sponsors = listOf(aSponsor(name = "B", position = 2), aSponsor(name = "A", position = 1))

            activeSponsorsInDisplayOrder(sponsors)

            sponsors.map { it.name } shouldBe listOf("B", "A")
        }
    }
})
