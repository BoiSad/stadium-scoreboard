rootProject.name = "stadium-scoreboard"

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
    // Repozytoria deklarujemy tylko tutaj, nie w modułach.
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
}

include(
    ":backend:common",
    ":backend:matches",
    ":backend:sponsors",
    ":backend:app",
)
