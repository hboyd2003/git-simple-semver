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

import dev.hboyd.git_simple_semver.conventional_commit.ConventionalCommitMatcher
import dev.hboyd.git_simple_semver.git_semver.BumpType
import dev.hboyd.git_simple_semver.git_semver.GitVersionGenerator
import dev.hboyd.git_simple_semver.semver.SemanticVersion
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.MergeCommand
import org.eclipse.jgit.lib.PersonIdent
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.io.CleanupMode
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.io.File
import java.time.ZoneOffset
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toJavaInstant

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class GitVersionGeneratorTest {

    @field:TempDir(cleanup = CleanupMode.ON_SUCCESS)
    lateinit var testProjectDir: File

    @Test
    fun `version is generated without repo`() {
        val version = GitVersionGenerator(
            listOf(),
            listOf(),
            listOf(),
            considerMajorChangesAsMinorWhenNoRelease = true,
            considerMajorChangesAsMinorWhenMajorZero = false,
            "".toRegex(),
            "v",
            listOf(),
            listOf()
        ).generateVersion(null)

        Assertions.assertEquals("0.0.0", version.toString())
    }

    @Test
    fun `version is generated without any commits`() {
        val git: Git = setupGitRepo(testProjectDir, initialCommit = false)
        val version = GitVersionGenerator(
            listOf(),
            listOf(),
            listOf(),
            considerMajorChangesAsMinorWhenNoRelease = true,
            considerMajorChangesAsMinorWhenMajorZero = false,
            "".toRegex(),
            "v",
            listOf(),
            listOf()
        ).generateVersion(git.repository)

        Assertions.assertEquals("0.0.0", version.toString())
    }

    @Test
    fun `version is generated without any version tags`() {
        val git: Git = setupGitRepo(testProjectDir, initialCommit = false)
        commitRandom(git, "fix: fix bug")
        commitRandom(git, "feat: add feature")

        val version = GitVersionGenerator(
            listOf(),
            listOf(ConventionalCommitMatcher("feat")),
            listOf(ConventionalCommitMatcher("fix")),
            considerMajorChangesAsMinorWhenNoRelease = true,
            considerMajorChangesAsMinorWhenMajorZero = false,
            "".toRegex(),
            "v",
            listOf(),
            listOf()
        ).generateVersion(git.repository)

        Assertions.assertEquals("0.1.0", version.toString())
    }

    @Test
    fun `version is generated while in a detached head`() {
        val git: Git = setupGitRepo(testProjectDir, initialCommit = false)
        commitRandom(git, "fix: fix bug")
        git.tag().setName("v1.0.0").call()
        git.checkout().setName("v1.0.0").call()

        val version = GitVersionGenerator(
            listOf(),
            listOf(ConventionalCommitMatcher("feat")),
            listOf(ConventionalCommitMatcher("fix")),
            considerMajorChangesAsMinorWhenNoRelease = true,
            considerMajorChangesAsMinorWhenMajorZero = false,
            "".toRegex(),
            "v",
            listOf(),
            listOf()
        ).generateVersion(git.repository)

        Assertions.assertEquals("1.0.0", version.toString())
    }

    @Test
    fun `generated version bumps patch version once`() {
        val git: Git = setupGitRepo(testProjectDir)
        git.tag().setName("v1.0.0").call()
        commitRandom(git, "fix: fix bug")
        commitRandom(git, "fix: fix another bug")

        val version = GitVersionGenerator(
            listOf(),
            listOf(ConventionalCommitMatcher("feat")),
            listOf(ConventionalCommitMatcher("fix")),
            considerMajorChangesAsMinorWhenNoRelease = true,
            considerMajorChangesAsMinorWhenMajorZero = false,
            "".toRegex(),
            "v",
            listOf(),
            listOf()
        ).generateVersion(git.repository)

        Assertions.assertEquals("1.0.1", version.toString())
    }

    @Test
    fun `generated version bumps minor version once`() {
        val git: Git = setupGitRepo(testProjectDir)
        git.tag().setName("v1.0.0").call()
        commitRandom(git, "fix: fix bug")
        commitRandom(git, "feat: new feature")
        commitRandom(git, "fix: fix another bug")

        val version = GitVersionGenerator(
            listOf(),
            listOf(ConventionalCommitMatcher("feat")),
            listOf(ConventionalCommitMatcher("fix")),
            considerMajorChangesAsMinorWhenNoRelease = true,
            considerMajorChangesAsMinorWhenMajorZero = false,
            "".toRegex(),
            "v",
            listOf(),
            listOf()
        ).generateVersion(git.repository)

        Assertions.assertEquals("1.1.0", version.toString())
    }

    @Test
    fun `generated version bumps major version once`() {
        val git: Git = setupGitRepo(testProjectDir)
        git.tag().setName("v1.0.0").call()
        commitRandom(git, "fix: fix bug")
        commitRandom(git, "feat: new feature")
        commitRandom(git, "fix: fix another bug")
        commitRandom(git, "feat: add feature")
        commitRandom(git, "feat!: breaking change")


        val version = GitVersionGenerator(
            listOf(),
            listOf(ConventionalCommitMatcher("feat")),
            listOf(ConventionalCommitMatcher("fix")),
            considerMajorChangesAsMinorWhenNoRelease = true,
            considerMajorChangesAsMinorWhenMajorZero = false,
            "".toRegex(),
            "v",
            listOf(),
            listOf()
        ).generateVersion(git.repository)

        Assertions.assertEquals("2.0.0", version.toString())
    }

    @Test
    fun `generated version bumps major version once when breaking change footer exists`() {
        val git: Git = setupGitRepo(testProjectDir)
        val initialVersion = SemanticVersion(1, 0, 0)
        git.tag().setName("v$initialVersion").call()
        commitRandom(git, "fix: fix bug")
        commitRandom(git, """
            feat: a new feature
            
            another-footer: a footer
            BREAKING CHANGE: a breaking change!
            last-footer: the last footer""".trimIndent())

        val version = GitVersionGenerator(
            listOf(),
            listOf(ConventionalCommitMatcher("feat")),
            listOf(ConventionalCommitMatcher("fix")),
            considerMajorChangesAsMinorWhenNoRelease = true,
            considerMajorChangesAsMinorWhenMajorZero = false,
            "".toRegex(),
            "v",
            listOf(),
            listOf()
        ).generateVersion(git.repository)

        Assertions.assertEquals("2.0.0", version.toString())
    }

    @ParameterizedTest
    @EnumSource(value = BumpType::class)
    fun `generated version bumps with minimum bump when commits after release exist but non match any bump`(bumpType: BumpType) {
        val git: Git = setupGitRepo(testProjectDir)
        val initialVersion = SemanticVersion(1, 0, 0)
        git.tag().setName("v$initialVersion").call()
        commitRandom(git, "nonematch: fix bug")

        val version = GitVersionGenerator(
            listOf(),
            listOf(ConventionalCommitMatcher("feat")),
            listOf(ConventionalCommitMatcher("fix")),
            considerMajorChangesAsMinorWhenNoRelease = true,
            considerMajorChangesAsMinorWhenMajorZero = false,
            "".toRegex(),
            "v",
            listOf(),
            listOf(),
            bumpType
        ).generateVersion(git.repository)


        Assertions.assertEquals(initialVersion.bump(bumpType), version)
    }

    @Test
    fun `generated version does not bump based on the minimum bump when bumping commits after release exist`() {
        val git: Git = setupGitRepo(testProjectDir)
        git.tag().setName("v1.0.0").call()
        commitRandom(git, "fix: fix bug")

        val version = GitVersionGenerator(
            listOf(),
            listOf(ConventionalCommitMatcher("feat")),
            listOf(ConventionalCommitMatcher("fix")),
            considerMajorChangesAsMinorWhenNoRelease = true,
            considerMajorChangesAsMinorWhenMajorZero = false,
            "".toRegex(),
            "v",
            listOf(),
            listOf(),
            BumpType.MAJOR
        ).generateVersion(git.repository)


        Assertions.assertEquals("1.0.1", version.toString())
    }

    @Test
    fun `generated version bumps version as major for breaking change when major version 0 and major changes are not considered minor when major version 0`() {
        val git: Git = setupGitRepo(testProjectDir)
        git.tag().setName("v0.1.0").call()
        commitRandom(git, "feat!: breaking feature")

        val version = GitVersionGenerator(
            listOf(),
            listOf(ConventionalCommitMatcher("feat")),
            listOf(ConventionalCommitMatcher("fix")),
            considerMajorChangesAsMinorWhenNoRelease = false,
            considerMajorChangesAsMinorWhenMajorZero = false,
            "".toRegex(),
            "v",
            listOf(),
            listOf(),
            BumpType.MAJOR,
        ).generateVersion(git.repository)


        Assertions.assertEquals("1.0.0", version.toString())
    }

    @Test
    fun `generated version bumps version as minor for breaking change when major version 0 and major changes are considered minor when major version 0`() {
        val git: Git = setupGitRepo(testProjectDir)
        git.tag().setName("v0.1.0").call()
        commitRandom(git, "feat!: breaking feature")

        val version = GitVersionGenerator(
            listOf(),
            listOf(ConventionalCommitMatcher("feat")),
            listOf(ConventionalCommitMatcher("fix")),
            considerMajorChangesAsMinorWhenNoRelease = false,
            considerMajorChangesAsMinorWhenMajorZero = true,
            "".toRegex(),
            "v",
            listOf(),
            listOf(),
            BumpType.MAJOR
        ).generateVersion(git.repository)

        Assertions.assertEquals("0.2.0", version.toString())
    }

    @Test
    fun `generated version bumps version as minor for breaking change when no release and major changes are considered minor when major version 0`() {
        val git: Git = setupGitRepo(testProjectDir)
        commitRandom(git, "feat!: breaking feature")

        val version = GitVersionGenerator(
            listOf(),
            listOf(ConventionalCommitMatcher("feat")),
            listOf(ConventionalCommitMatcher("fix")),
            considerMajorChangesAsMinorWhenNoRelease = false,
            considerMajorChangesAsMinorWhenMajorZero = true,
            "".toRegex(),
            "v",
            listOf(),
            listOf(),
            BumpType.MAJOR
        ).generateVersion(git.repository)

        Assertions.assertEquals("0.1.0", version.toString())
    }

    @Test
    fun `generated version bumps version as major for breaking change when major version not 0 and major changes are considered minor when major version 0`() {
        val git: Git = setupGitRepo(testProjectDir)
        git.tag().setName("v1.0.0").call()
        commitRandom(git, "feat!: breaking feature")

        val version = GitVersionGenerator(
            listOf(),
            listOf(ConventionalCommitMatcher("feat")),
            listOf(ConventionalCommitMatcher("fix")),
            considerMajorChangesAsMinorWhenNoRelease = false,
            considerMajorChangesAsMinorWhenMajorZero = true,
            "".toRegex(),
            "v",
            listOf(),
            listOf(),
            BumpType.MAJOR
            ).generateVersion(git.repository)


        Assertions.assertEquals("2.0.0", version.toString())
    }

    @Test
    fun `context ignores commits on other branches`() {
        val git: Git = setupGitRepo(testProjectDir)
        commitRandom(git, "fix: fix bug")
        commitRandom(git, "feat: add feature")
        git.branchCreate().setName("branch2").call()
        commitRandom(git, "chore: update dependencies")
        commitRandom(git, "feat!: breaking change")
        git.checkout().setName("branch2").call()

        val context = GitVersionGenerator.createVersionProviderContext(git.repository)
        Assertions.assertEquals(3, context.commits.size)
        Assertions.assertEquals(
            listOf("feat: add feature", "fix: fix bug", "chore: initial commit"),
            context.commits.map { it.commit.shortMessage })
    }

    @Test
    fun `context ignores tags on other branches`() {
        val git: Git = setupGitRepo(testProjectDir)
        git.tag().setName("v1.0.0").call()
        git.branchCreate().setName("branch2").call()
        commitRandom(git, "fix: fix bug")
        git.tag().setName("v2.0.0").call()
        git.checkout().setName("branch2").call()
        commitRandom(git, "feat: add feature")
        git.tag().setName("v1.1.0").call()

        val context = GitVersionGenerator.createVersionProviderContext(git.repository)
        Assertions.assertEquals(2, context.versionTags.size)
        Assertions.assertEquals(listOf("v1.1.0", "v1.0.0"), context.versionTags.map { it.toString() })
    }

    @Test
    fun `context correctly counts number of commits since version tags`() {
        val git: Git = setupGitRepo(testProjectDir)
        git.tag().setName("v1.0.0").call()
        commitRandom(git, "feat: add feature")
        git.tag().setName("v1.1.0-SNAPSHOT").call()
        commitRandom(git, "fix: fix bug")
        commitRandom(git, "chore: update dependencies")

        val context = GitVersionGenerator.createVersionProviderContext(git.repository)
        Assertions.assertEquals(2, context.commitsSinceLastVersionTag)
        Assertions.assertEquals(3, context.commitsSinceLastReleaseVersionTag)
    }

    @Test
    fun `context maintains tag order`() {
        val git: Git = setupGitRepo(testProjectDir)
        git.tag().setName("v1.0.0").call()
        commitRandom(git, "feat: add feature")
        git.tag().setName("v2.1.0").call()
        commitRandom(git, "chore: update dependencies")
        git.tag().setName("v1.1.0").call()

        val context = GitVersionGenerator.createVersionProviderContext(git.repository)
        Assertions.assertEquals(3, context.versionTags.size)
        Assertions.assertEquals(listOf("v1.1.0", "v2.1.0", "v1.0.0"), context.versionTags.map { it.toString() })
    }

    @Test
    fun `generated version considers older commits merged later`() {
        val instant = Clock.System.now()
        val git: Git = setupGitRepo(testProjectDir)
        commitRandom(git, "feat: a new feature", instant.plus(1.minutes))
        git.tag().setName("v1.0.0").call()

        git.branchCreate()
            .setName("feature")
            .call()
        git.checkout()
            .setName("feature")
            .call()
        commitRandom(git, "feat: a feature in the feature branch", instant.plus(2.minutes))
        commitRandom(git, "feat: another feature in the feature branch", instant.plus(3.minutes))
        val featureBranchRef = git.repository.fullBranch

        git.checkout()
            .setName("master")
            .call()
        commitRandom(git, "fix: a fix", instant.plus(4.minutes))

        // Release without merging feature branch
        git.tag().setName("v1.0.1")
            .setTagger(PersonIdent("Test", "TestUser@non.existent", instant.plus(5.minutes).toJavaInstant(), ZoneOffset.UTC))
            .call()

        git.merge().setFastForward(MergeCommand.FastForwardMode.NO_FF)
            .include(git.repository.exactRef(featureBranchRef))
            .setConflictStyle(MergeCommand.ConflictStyle.MERGE)
            .call()

        val version = GitVersionGenerator(
            listOf(),
            listOf(ConventionalCommitMatcher("feat")),
            listOf(ConventionalCommitMatcher("fix")),
            considerMajorChangesAsMinorWhenNoRelease = false,
            considerMajorChangesAsMinorWhenMajorZero = true,
            "".toRegex(),
            "v",
            listOf(),
            listOf(),
            BumpType.PATCH
        ).generateVersion(git.repository)

        Assertions.assertEquals("1.1.0", version.toString())
    }
}
