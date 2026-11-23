package pl.stadiumscoreboard.common.domain

/**
 * Naruszenie reguły biznesowej, np. niedozwolone przejście statusu meczu.
 * Warstwa web zamieni takie wyjątki na odpowiedzi 4xx, a nie 500.
 */
abstract class DomainException(message: String) : RuntimeException(message)
