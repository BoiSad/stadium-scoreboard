// Moduł wspólny: obsługa błędów i typy bazowe. Na razie pusty, kod pojawi się w kolejnych etapach.
plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(libs.versions.java.get().toInt())
}
