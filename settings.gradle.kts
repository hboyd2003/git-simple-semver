rootProject.name = "git-simple-semver"

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenLocal()
        mavenCentral()
        maven(url = "https://repo.papermc.io/repository/maven-public/") {
            name = "papermc-repo-releases"
            mavenContent { releasesOnly() }
        }
        maven(url = "https://repo.papermc.io/repository/maven-snapshots/") {
            name = "papermc-repo-snapshots"
            mavenContent { snapshotsOnly() }
        }
        maven(url = "https://repo.hboyd.dev/releases/") {
            name = "hboyd-dev-repo-releases"
            mavenContent { releasesOnly() }
        }
        maven(url = "https://repo.hboyd.dev/snapshots/") {
            name = "hboyd-dev-repo-snapshots"
            mavenContent { snapshotsOnly() }
        }
    }
}

pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
        mavenCentral()
        maven(url = "https://repo.papermc.io/repository/maven-public/") {
            name = "papermc-repo-releases"
            mavenContent { releasesOnly() }
        }
        maven(url = "https://repo.papermc.io/repository/maven-snapshots/") {
            name = "papermc-repo-snapshots"
            mavenContent { snapshotsOnly() }
        }
        maven(url = "https://repo.hboyd.dev/releases/") {
            name = "hboyd-dev-repo-releases"
            mavenContent { releasesOnly() }
        }
        maven(url = "https://repo.hboyd.dev/snapshots/") {
            name = "hboyd-dev-repo-snapshots"
            mavenContent { snapshotsOnly() }
        }
    }
}
