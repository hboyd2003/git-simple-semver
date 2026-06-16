/*
 * git-simple-semver
 * Copyright (c) 2026 Harrison Boyd
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package dev.hboyd.git_simple_semver

import org.eclipse.jgit.api.Git
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.CleanupMode
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.writeText

class GitSimpleSemverExtensionTest {
    @field:TempDir(cleanup = CleanupMode.ON_SUCCESS)
    lateinit var testProjectDir: Path

    private val baseGradleBuild: String =
        """
        import dev.hboyd.git_simple_semver.git_semver.SemanticVersionIdentifierProvider
        import dev.hboyd.git_simple_semver.git_semver.IdentifierProviderContext
        import dev.hboyd.git_simple_semver.git_semver.branchProvider
        import dev.hboyd.git_simple_semver.git_semver.commitsSinceReleaseProvider
        import dev.hboyd.git_simple_semver.git_semver.currentCommitHashProvider
        import dev.hboyd.git_simple_semver.git_semver.dateTimeProvider
        import dev.hboyd.git_simple_semver.git_semver.textProvider
            
        plugins {
            id("dev.hboyd.git-simple-semver")
        }
        
        gitSimpleSemver {
            %s
        }
    """.trimIndent()

    @Test
    fun `print version task prints the current version`() {
        val git = generateGradleProject("")
        git.tag().setName("v1.2.3").call()
        commitRandom(git, "misc: misc commit")

        executeGradleRun("printVersion").assertPrintedVersion("1.2.3-SNAPSHOT+1")
    }

    @Test
    fun `print core version task prints only the core version`() {
        val git = generateGradleProject("")
        git.tag().setName("v1.2.3").call()
        commitRandom(git, "misc: misc commit")

        executeGradleRun("printCoreVersion").assertPrintedVersion("1.2.3", "printCoreVersion")
    }

    @Test
    fun `major change selections control major version bumps`() {
        val git = generateGradleProject(
            """
            majorChangeSelections.set(listOf(changeSpec("api")))
            minorChangeSelections.set(emptyList())
            patchChangeSelections.set(emptyList())
            """.trimIndent()
        )
        git.tag().setName("v1.2.3").call()
        commitRandom(git, "api: change public contract")

        executeGradleRun("printCoreVersion").assertPrintedVersion("2.2.3", "printCoreVersion")
    }

    @Test
    fun `minor change selections control minor version bumps`() {
        val git = generateGradleProject(
            """
            minorChangeSelections.set(listOf(changeSpec("deps")))
            patchChangeSelections.set(emptyList())
            """.trimIndent()
        )
        git.tag().setName("v1.2.3").call()
        commitRandom(git, "deps: update dependency")

        executeGradleRun("printCoreVersion").assertPrintedVersion("1.3.3", "printCoreVersion")
    }

    @Test
    fun `patch change selections control patch version bumps`() {
        val git = generateGradleProject(
            """
            minorChangeSelections.set(emptyList())
            patchChangeSelections.set(listOf(changeSpec("chore")))
            """.trimIndent()
        )
        git.tag().setName("v1.2.3").call()
        commitRandom(git, "chore: update build")

        executeGradleRun("printCoreVersion").assertPrintedVersion("1.2.4", "printCoreVersion")
    }

    @Test
    fun `consider major changes as minor when no release controls first breaking change bump`() {
        val git = generateGradleProject(
            """
            considerMajorChangesAsMinorWhenNoRelease.set(false)
            """.trimIndent()
        )
        commitRandom(git, "feat!: replace api")

        executeGradleRun("printVersion").assertPrintedVersion("1.0.0")
    }

    @Test
    fun `ignored commit regex excludes matching commits from version bumps`() {
        val git = generateGradleProject(
            """
            ignoredCommitRegex.set("^skip:.*")
            """.trimIndent()
        )
        git.tag().setName("v1.2.3").call()
        commitRandom(git, "skip: feat: ignored feature")
        commitRandom(git, "fix: real bug fix")

        executeGradleRun("printCoreVersion").assertPrintedVersion("1.2.4", "printCoreVersion")
    }

    @Test
    fun `version tag prefix controls which tags are considered releases`() {
        val git = generateGradleProject(
            """
            versionTagPrefix.set("release-")
            """.trimIndent()
        )
        git.tag().setName("v9.9.9").call()
        git.tag().setName("release-1.2.3").call()
        commitRandom(git, "fix: real bug fix")

        executeGradleRun("printCoreVersion").assertPrintedVersion("1.2.4", "printCoreVersion")
    }

    @Test
    fun `pre release identifier providers append pre release identifiers`() {
        val git = generateGradleProject(
            """
            preReleaseIdentifierProviders.set(listOf(SemanticVersionIdentifierProvider { "beta" }))
            buildIdentifierProviders.set(listOf())
            """.trimIndent()
        )
        git.tag().setName("v1.2.3").call()
        commitRandom(git, "fix: real bug fix")

        executeGradleRun("printVersion").assertPrintedVersion("1.2.4-beta")
    }

    @Test
    fun `pre release and build-metadata identifiers are not included in core version`() {
        val git = generateGradleProject(
            """
            preReleaseIdentifierProviders.set(listOf(SemanticVersionIdentifierProvider { "beta" }))
            buildIdentifierProviders.set(listOf(SemanticVersionIdentifierProvider { "build-123" }))            
            """.trimIndent()
        )
        git.tag().setName("v1.2.3").call()
        commitRandom(git, "fix: real bug fix")

        executeGradleRun("printCoreVersion").assertPrintedVersion("1.2.4", "printCoreVersion")
    }

    @Test
    fun `build identifier providers append build metadata identifiers`() {
        val git = generateGradleProject(
            """
            buildIdentifierProviders.set(listOf(SemanticVersionIdentifierProvider { "build-123" }))
            preReleaseIdentifierProviders.set(listOf())
            """.trimIndent()
        )
        git.tag().setName("v1.2.3").call()
        commitRandom(git, "fix: real bug fix")

        executeGradleRun("printVersion").assertPrintedVersion("1.2.4+build-123")
    }

    private fun generateGradleProject(pluginConfig: String): Git {
        testProjectDir.resolve("settings.gradle.kts").writeText(
            """
            rootProject.name = "git-simple-semver-test"
            """.trimIndent()
        )
        testProjectDir.resolve("build.gradle.kts").writeText(String.format(baseGradleBuild, pluginConfig))
        return setupGitRepo(testProjectDir.toFile())
    }

    private fun executeGradleRun(task: String): BuildResult =
        GradleRunner
            .create()
            .withProjectDir(testProjectDir.toFile())
            .withArguments(task)
            .withPluginClasspath()
            .withDebug(true)
            .forwardOutput()
            .build()

    private fun BuildResult.assertPrintedVersion(expectedVersion: String, taskName: String = "printVersion") {
        val lines: List<String> = output.lines()
        val printVersionTaskOutputHeader = "> Task :$taskName"

        Assertions.assertEquals(expectedVersion, lines[lines.indexOf(printVersionTaskOutputHeader) + 1])
    }
}