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
import org.eclipse.jgit.api.Git
import java.io.File
import java.util.*

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

fun commitRandom(git: Git, commitMessage: String): ConventionalCommit {
    val randomFile = git.repository.directory.resolve(UUID.randomUUID().toString())
    randomFile.writeText("Content")
    git.add().addFilepattern(randomFile.toString()).call()
    return git.commit().setMessage(commitMessage).call().toConventionalCommit()
}