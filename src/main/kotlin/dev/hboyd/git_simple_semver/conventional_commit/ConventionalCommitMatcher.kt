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

package dev.hboyd.git_simple_semver.conventional_commit

/**
 * A matcher for conventional commits.
 */
class ConventionalCommitMatcher(
    val typeRegex: Regex = Regex(".*"),
    val scopeRegex: Regex = Regex(".*"),
    val descriptionRegex: Regex = Regex(".*"),
    val bodyRegex: Regex = Regex(".*"),
    val footerKeyRegex: Regex = Regex(".*"),
    val footerValueRegex: Regex = Regex(".*"),
    val breakingChange: Boolean? = null,
) {
    constructor(
        type: String = ".*",
        scope: String = ".*",
        description: String = ".*",
        body: String = "(.|\n)*",
        footerKey: String = ".*",
        footerValue: String = ".*",
        breakingChange: Boolean? = null
    ) : this(
        Regex(type),
        Regex(scope),
        Regex(description),
        Regex(body),
        Regex(footerKey),
        Regex(footerValue),
        breakingChange
    )

    /**
     * Returns true if the given [commit] matches this matcher.
     */
    fun matches(commit: ConventionalCommit): Boolean {
        return commit.type.matches(typeRegex)
                && (commit.scope?.matches(scopeRegex) ?: true)
                && commit.description.matches(descriptionRegex)
                && commit.body.matches(bodyRegex)
                && commit.commit.footerLines.all { it.key.matches(footerKeyRegex) && it.value.matches(footerValueRegex) }
                && if (breakingChange != null) commit.breakingChange == breakingChange else true
    }
}