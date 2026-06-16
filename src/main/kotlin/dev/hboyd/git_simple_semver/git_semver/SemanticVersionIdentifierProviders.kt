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

import dev.hboyd.git_simple_semver.semver.SemanticLabel.Companion.IDENTIFIER_REGEX
import java.time.Clock
import java.time.format.DateTimeFormatter

/**
 * Creates a [SemanticVersionIdentifierProvider] that returns the current date and time in the specified [formatter].
 * By default, the date and time are formatted in the format `yyyyMMddHHmmss`.
 */
fun dateTimeProvider(formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) =
    SemanticVersionIdentifierProvider {
        val formatterWithZone: DateTimeFormatter =
            if (formatter.zone == null) formatter.withZone(Clock.systemUTC().zone)
            else formatter

        val formatedDate: String = formatterWithZone.format(Clock.systemUTC().instant())
        require(formatedDate.matches(IDENTIFIER_REGEX)) {
            "Invalid date formater. Output produced \"$formatedDate\". Identifiers must match $IDENTIFIER_REGEX"
        }
        return@SemanticVersionIdentifierProvider formatedDate
    }

/**
 * Creates a [SemanticVersionIdentifierProvider] that always returns the given [text]
 */
fun textProvider(text: String) = SemanticVersionIdentifierProvider {
    require(text.matches(IDENTIFIER_REGEX)) {
        "Invalid text \"$text\". Identifiers must match $IDENTIFIER_REGEX"
    }
    text
}

/**
 * Creates a [SemanticVersionIdentifierProvider] that returns the number of commits since the last release version tag.
 */
fun commitsSinceReleaseProvider() = SemanticVersionIdentifierProvider { ctx ->
    ctx.commitsSinceLastVersionTag?.toString()
}

/**
 * Creates a [SemanticVersionIdentifierProvider] that returns the current branch name.
 */
fun branchProvider() = SemanticVersionIdentifierProvider(IdentifierProviderContext::branch)

/**
 * Creates a [SemanticVersionIdentifierProvider] that returns the current commit hash.
 */
fun currentCommitHashProvider(shortHash: Boolean = true) = SemanticVersionIdentifierProvider {
    if (it.commits.isNotEmpty())
        if (shortHash) it.commits[0].commit.name.substring(0, 7)
        else it.commits[0].commit.name
    else null
}
