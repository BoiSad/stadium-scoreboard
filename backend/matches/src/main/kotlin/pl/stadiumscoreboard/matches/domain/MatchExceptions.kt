package pl.stadiumscoreboard.matches.domain

import pl.stadiumscoreboard.common.domain.DomainException

class InvalidMatchException(message: String) : DomainException(message)

class IllegalStatusTransitionException(
    val currentStatus: MatchStatus,
    val action: String,
) : DomainException("Niedozwolona operacja „$action” w statusie $currentStatus")

class InconsistentMatchEventsException(message: String) : DomainException(message)

class IllegalClockOperationException(message: String) : DomainException(message)

class InvalidAddedTimeException(message: String) : DomainException(message)
