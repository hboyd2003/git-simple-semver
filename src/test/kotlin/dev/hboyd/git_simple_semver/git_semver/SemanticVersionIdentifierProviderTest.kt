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

package dev.hboyd.git_simple_semver.git_semver

import dev.hboyd.git_simple_semver.buildIdentifierProviderContext
import dev.hboyd.git_simple_semver.commitRandom
import dev.hboyd.git_simple_semver.setupGitRepo
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.CleanupMode
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.io.File

class SemanticVersionIdentifierProviderTest {
    @field:TempDir(cleanup = CleanupMode.ON_SUCCESS)
    lateinit var testProjectDir: File

    val expectedStaticProviderString: String = "providedIdentifier"

    val staticProvider: SemanticVersionIdentifierProvider = SemanticVersionIdentifierProvider {
        expectedStaticProviderString
    }

    @Test
    fun `test provider provides expected string`() {
        assertEquals(expectedStaticProviderString, staticProvider.getIdentity(buildIdentifierProviderContext()))
    }

    @Test
    fun `only if not release modifier creates provider which produces null when there have been no commits since release and repo is not dirty`() {
        val git = setupGitRepo(testProjectDir)
        val commit = commitRandom(git, "fix: fix bug")

        assertNull(
            staticProvider.onlyIfNotRelease().getIdentity(
                buildIdentifierProviderContext(
                    commits = listOf(commit),
                    commitsSinceLastVersionTag = 0,
                    commitsSinceLastReleaseVersionTag = 0
                )
            )
        )
    }

    @ParameterizedTest
    @CsvSource(value = ["true,0", "true,1", "false,1"])
    fun `only if not release modifier creates provider which produces an identifier when version is not a release`(
        dirtyBranch: Boolean,
        commitsSinceRelease: Int
    ) {
        val context = buildIdentifierProviderContext(
            dirty = dirtyBranch,
            commitsSinceLastVersionTag = commitsSinceRelease,
            commitsSinceLastReleaseVersionTag = commitsSinceRelease
        )

        assertEquals(expectedStaticProviderString, staticProvider.onlyIfNotRelease().getIdentity(context))
    }
}
