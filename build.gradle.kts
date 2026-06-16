plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.pluginPublish)
    alias(libs.plugins.indra)
    alias(libs.plugins.indraPluginPublishing)
    alias(libs.plugins.indraLicenserSpotless)
    alias(libs.plugins.gitSimpleSemver)
}

dependencies {
    implementation(gradleApi())
    implementation(libs.jgit)

    testImplementation(platform(libs.junitBom))
    testImplementation(libs.junitJupiter)
    testRuntimeOnly(libs.junitPlatformLauncher)
}

indra {
    javaVersions {
        target(17)
    }

    github("hboyd2003", "git-simple-semver") {
        ci(true)
        publishing(false)
    }

    publishReleasesTo("hboydDev", "https://repo.hboyd.dev/releases")
    publishSnapshotsTo("hboydDev", "https://repo.hboyd.dev/snapshots")

    lgpl3OrLaterLicense()

    signWithKeyFromPrefixedProperties("hboyd")

    configurePublications {
        pom {
            developers {
                developer {
                    id = "hboyd"
                    timezone = "America/New_York"
                }
            }
        }
    }
}

indraSpotlessLicenser {
    licenseHeaderFile(rootProject.file(".spotless/license_header_template.txt"))
    newLine(true)
}

indraPluginPublishing {
    plugin(
        project.name,
        "dev.hboyd.git_simple_semver.GitSimpleSemver",
        "Git Simple Semver",
        "Automatically determines the semantic version based on conventional commits and git tags",
        listOf("git", "semver", "version", "semantic")
    )
    website("https://github.com/hboyd2003/git-simple-semver")
}

tasks {
    test {
        useJUnitPlatform()
    }
}