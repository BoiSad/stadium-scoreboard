package pl.stadiumscoreboard.sponsors.domain

import pl.stadiumscoreboard.common.domain.DomainException
import java.util.UUID

data class Sponsor(
    val id: UUID,
    val name: String,
    val imagePath: String,
    val displaySeconds: Int,
    val position: Int,
    val active: Boolean,
) {
    init {
        if (name.isBlank()) {
            throw InvalidSponsorException("Nazwa sponsora nie może być pusta")
        }
        if (displaySeconds <= 0) {
            throw InvalidSponsorException("Czas wyświetlania musi być dodatni, podano: $displaySeconds s")
        }
        if (position < 0) {
            throw InvalidSponsorException("Pozycja nie może być ujemna, podano: $position")
        }
    }
}

class InvalidSponsorException(message: String) : DomainException(message)

/** Sponsorzy do rotacji na ekranie: tylko aktywni, po pozycji, a przy tej samej pozycji po nazwie. */
fun activeSponsorsInDisplayOrder(sponsors: List<Sponsor>): List<Sponsor> =
    sponsors
        .filter { it.active }
        .sortedWith(compareBy(Sponsor::position, Sponsor::name))
