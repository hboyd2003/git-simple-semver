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

import dev.hboyd.git_simple_semver.conventional_commit.ConventionalCommit
import dev.hboyd.git_simple_semver.conventional_commit.toConventionalCommit
import dev.hboyd.git_simple_semver.git_semver.BumpType
import dev.hboyd.git_simple_semver.git_semver.IdentifierProviderContext
import dev.hboyd.git_simple_semver.semver.SemanticVersion
import dev.hboyd.git_simple_semver.semver.SemanticVersionTag
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.PersonIdent
import java.io.File
import java.time.ZoneOffset
import java.util.*
import kotlin.time.Instant
import kotlin.time.toJavaInstant

fun setupGitRepo(dir: File, initialCommit: Boolean = true): Git {
    val git: Git = Git.init()
        .setDirectory(dir)
        .setGitDir(dir.resolve(".git"))
        .call()

    git.repository.config.setString("user", null, "name", "Test User")
    git.repository.config.setString("user", null, "email", "test@test.org")
    git.repository.config.save()

    if (initialCommit) {
        git.add().addFilepattern(".").call()
        git.commit().setMessage("chore: initial commit").call()
    }

    return git
}

fun commitRandom(git: Git, commitMessage: String, time: Instant? = null): ConventionalCommit {
    val randomFile = git.repository.directory.resolve(UUID.randomUUID().toString())
    randomFile.writeText("Content")
    git.add().addFilepattern(randomFile.toString()).call()

    val commitCommand = git.commit()
        .setMessage(commitMessage)

    if (time != null) {
        val personIdent = PersonIdent("Test", "TestUser@non.existent", time.toJavaInstant(), ZoneOffset.UTC)
        commitCommand.author = personIdent
        commitCommand.committer = personIdent
    }

    return commitCommand
        .call()
        .toConventionalCommit()
}

fun buildIdentifierProviderContext(
    version: SemanticVersion = SemanticVersion(1, 2, 3),
    bumpType: BumpType = BumpType.MINOR,
    dirty: Boolean = false,
    branch: String = "main",
    commits: List<ConventionalCommit> = listOf(),
    versionTags: List<SemanticVersionTag> = listOf(),
    commitsSinceLastVersionTag: Int? = 1,
    commitsSinceLastReleaseVersionTag: Int? = 1
): IdentifierProviderContext {
    return IdentifierProviderContext(
        version,
        bumpType,
        dirty,
        branch,
        commits,
        versionTags,
        commitsSinceLastVersionTag,
        commitsSinceLastReleaseVersionTag
    )
}
