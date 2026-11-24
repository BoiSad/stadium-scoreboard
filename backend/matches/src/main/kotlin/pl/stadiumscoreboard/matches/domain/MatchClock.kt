package pl.stadiumscoreboard.matches.domain

import java.time.Instant
import kotlin.time.Duration
import kotlin.time.toKotlinDuration

/**
 * Zegar połowy meczu. Nie trzyma „aktualnej minuty”, tylko moment ostatniego startu
 * i czas zebrany wcześniej. Dzięki temu każdy (serwer, ekran) policzy czas sam z `now`.
 *
 * Domena nigdy nie pyta systemu o aktualny czas: `now` zawsze przychodzi z zewnątrz.
 */
data class MatchClock(
    val startedAt: Instant?,
    val elapsedBeforeStart: Duration,
) {
    init {
        if (elapsedBeforeStart.isNegative()) {
            throw IllegalClockOperationException("Czas zegara nie może być ujemny: $elapsedBeforeStart")
        }
    }

    val running: Boolean
        get() = startedAt != null

    fun start(now: Instant): MatchClock {
        if (running) throw IllegalClockOperationException("Zegar już chodzi")
        return copy(startedAt = now)
    }

    fun pause(now: Instant): MatchClock {
        if (!running) throw IllegalClockOperationException("Zegar już jest zatrzymany")
        return MatchClock(startedAt = null, elapsedBeforeStart = elapsed(now))
    }

    /** Zatrzymuje zegar niezależnie od tego, czy chodził (np. na koniec połowy). */
    fun stop(now: Instant): MatchClock = if (running) pause(now) else this

    fun elapsed(now: Instant): Duration {
        val start = startedAt ?: return elapsedBeforeStart
        val runningFor = java.time.Duration.between(start, now).toKotlinDuration()
        // Gdy `now` jest wcześniej niż start (np. rozjechane zegary maszyn), nie cofamy czasu.
        return elapsedBeforeStart + runningFor.coerceAtLeast(Duration.ZERO)
    }

    /** Ręczna korekta: ustawia czas na [newElapsed]. Chodzący zegar liczy dalej od `now`. */
    fun adjust(newElapsed: Duration, now: Instant): MatchClock {
        if (newElapsed.isNegative()) {
            throw IllegalClockOperationException("Czas zegara nie może być ujemny: $newElapsed")
        }
        return MatchClock(startedAt = if (running) now else null, elapsedBeforeStart = newElapsed)
    }

    companion object {
        val STOPPED = MatchClock(startedAt = null, elapsedBeforeStart = Duration.ZERO)

        fun startedAt(now: Instant) = MatchClock(startedAt = now, elapsedBeforeStart = Duration.ZERO)
    }
}
