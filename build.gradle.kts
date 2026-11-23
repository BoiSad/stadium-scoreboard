// Pluginy deklarujemy raz w głównym projekcie (bez stosowania),
// a moduły stosują je przez alias. Dzięki temu wszystkie moduły używają tych samych wersji.
plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.spring) apply false
    alias(libs.plugins.spring.boot) apply false
}
