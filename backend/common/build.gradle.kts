// Moduł wspólny: obsługa błędów i typy bazowe.
plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(libs.versions.java.get().toInt())
}

dependencies {
    // BOM tylko wyrównuje wersje JUnit w testach; kod modułu nie zależy od Springa.
    testImplementation(platform(libs.spring.boot.bom))
    testImplementation(libs.bundles.kotest)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
}
