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

import dev.hboyd.git_simple_semver.git_semver.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.CleanupMode
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.time.format.DateTimeFormatter

class SemanticVersionIdentifierProvidersTest {
    @field:TempDir(cleanup = CleanupMode.ON_SUCCESS)
    lateinit var testProjectDir: File

    @Test
    fun `date provider returns date time`() {
        val identity = dateTimeProvider().getIdentity(buildIdentifierProviderContext())

        assertNotNull(identity)
        assertDoesNotThrow { DateTimeFormatter.ofPattern("yyyyMMddHHmmss").parse(identity) }
    }

    @Test
    fun `date provider returns custom date time`() {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss")
        val identity = dateTimeProvider(formatter).getIdentity(buildIdentifierProviderContext())

        assertNotNull(identity)
        assertDoesNotThrow { formatter.parse(identity) }
    }

    @Test
    fun `text provider returns text`() {
        val identity = textProvider("SNAPSHOT").getIdentity(buildIdentifierProviderContext())

        assertEquals("SNAPSHOT", identity)
    }

    @Test
    fun `commits since release provider returns commits since last version tag`() {
        val identity = commitsSinceReleaseProvider().getIdentity(
            buildIdentifierProviderContext(commitsSinceLastVersionTag = 2)
        )

        assertEquals("2", identity)
    }

    @Test
    fun `commits since release provider returns null when commits since last version tag is unknown`() {
        val identity = commitsSinceReleaseProvider().getIdentity(
            buildIdentifierProviderContext(commitsSinceLastVersionTag = null)
        )

        assertNull(identity)
    }

    @Test
    fun `branch provider returns sanitized branch`() {
        val identity = branchProvider().getIdentity(
            buildIdentifierProviderContext(branch = "release/v2.0")
        )

        assertEquals("release-v20", identity)
    }

    @Test
    fun `current commit hash provider returns full current commit hash`() {
        val git = setupGitRepo(testProjectDir)
        val commit = commitRandom(git, "fix: fix bug")
        val identity = currentCommitHashProvider(shortHash = false).getIdentity(
            buildIdentifierProviderContext(commits = listOf(commit))
        )

        assertEquals(commit.commit.name, identity)
    }

    @Test
    fun `current commit hash provider returns null when there are no commits`() {
        val identity = currentCommitHashProvider().getIdentity(
            buildIdentifierProviderContext(commits = listOf())
        )

        assertNull(identity)
    }

    @Test
    fun `current commit short hash provider returns first seven characters of current commit hash`() {
        val git = setupGitRepo(testProjectDir)
        val commit = commitRandom(git, "fix: fix bug")
        val identity = currentCommitHashProvider().getIdentity(
            buildIdentifierProviderContext(commits = listOf(commit))
        )

        assertEquals(commit.commit.name.substring(0, 7), identity)
    }

    @Test
    fun `current commit short hash provider returns null when there are no commits`() {
        val identity = currentCommitHashProvider().getIdentity(
            buildIdentifierProviderContext(commits = listOf())
        )

        assertNull(identity)
    }
}
