# Git Simple Semver

Git Simple Semver is a Gradle plugin that generates the current version of the project based on git tags and
conventional commits. It is based on the idea the version is metadata and should not be committed with the code. It's
primarily intended for easy customization of the version and to make it easier to track the version of the project,
 especially in CI environments and during local development.

## Usage

### Repository

For releases:
```kotlin
// Release builds are published to Maven Central
mavenCentral()
maven {
    name = "hboyd-dev-repo-releases"
    url = uri("https://repo.hboyd.dev/releases/")
}
```

For snapshots:
```kotlin
maven {
    name = "hboyd-dev-repo-snapshots"
    url = uri("https://repo.hboyd.dev/snapshots/")
}
```

### Configuration
```kotlin
import dev.hboyd.git_simple_semver.GitSimpleSemverExtension.TypeScopeSelectionSpec
import dev.hboyd.git_simple_semver.git_semver.SemanticVersionIdentifierProvider
import dev.hboyd.git_simple_semver.git_semver.SemanticVersionIdentifierProviders
    
plugins {
    id("dev.hboyd.git-simple-semver")
}

gitSimpleSemver {
    majorChangeSelections.set(listOf(changeSpec("feat", "api"))) // This is in addition to the standard `!` suffix and "BREAKING CHANGE" footer
    minorChangeSelections.set(listOf(changeSpec("feat"), changeSpec("refactor")))
    patchChangeSelections.set(listOf(changeSpec("chore"), changeSpec("fix")))
    versionTagPrefix.set("release-") // Prefix for version tags
    
    considerMajorChangesAsMinorWhenNoRelease.set(false)
    ignoredCommitRegex.set("^SKIPME.*")
    
    preReleaseIdentifierProviders.set(listOf(SemanticVersionIdentifierProviders.buildNumberProvider))
    buildIdentifierProviders.set(listOf(SemanticVersionIdentifierProvider { "build-123" }))
}
```
For more information on the configuration options, see the source code.

## Versioning

Versions follow the [SemVer 2.0.0](http://semver.org/) versioning standard. For the versions available, see the
 [tags on this repository](https://github.com/your/project/tags).

## Authors

* **Harrison Boyd** – *Initial work* - [Hboyd2003](https://github.com/hboyd2003)

See also the list of [contributors](https://github.com/hboyd2003/git-simple-semver/contributors) who participated in this project.

## License

This project is licensed under the LGPLv3 License – see the [LICENSE.md](LICENSE.md) file for details