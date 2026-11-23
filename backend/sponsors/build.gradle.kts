plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(libs.versions.java.get().toInt())
}

dependencies {
    api(project(":backend:common"))

    // BOM tylko wyrównuje wersje JUnit w testach; kod modułu nie zależy od Springa.
    testImplementation(platform(libs.spring.boot.bom))
    testImplementation(libs.bundles.kotest)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
}
