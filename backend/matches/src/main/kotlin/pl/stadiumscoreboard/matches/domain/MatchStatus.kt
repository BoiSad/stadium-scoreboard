package pl.stadiumscoreboard.matches.domain

enum class MatchStatus {
    SCHEDULED,
    FIRST_HALF,
    HALF_TIME,
    SECOND_HALF,
    FINISHED;

    val isInPlay: Boolean
        get() = this == FIRST_HALF || this == SECOND_HALF
}
