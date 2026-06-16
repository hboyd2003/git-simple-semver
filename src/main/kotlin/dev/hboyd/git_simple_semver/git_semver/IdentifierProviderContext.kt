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

import dev.hboyd.git_simple_semver.conventional_commit.ConventionalCommit
import dev.hboyd.git_simple_semver.semver.SemanticVersion
import dev.hboyd.git_simple_semver.semver.SemanticVersionTag

/**
 * Context used by semantic version identifier providers to generate identifiers.
 */
class IdentifierProviderContext(
    val version: SemanticVersion,
    val bumpType: BumpType,
    dirty: Boolean,
    branch: String,
    commits: List<ConventionalCommit>,
    versionTags: List<SemanticVersionTag>,
    commitsSinceLastVersionTag: Int?,
    commitsSinceLastReleaseVersionTag: Int?
) : VersionProviderContext(
    dirty,
    branch,
    commits,
    versionTags,
    commitsSinceLastVersionTag,
    commitsSinceLastReleaseVersionTag
) {
    constructor(
        versionProviderContext: VersionProviderContext,
        version: SemanticVersion,
        bumpType: BumpType
    ) : this(
        version,
        bumpType,
        versionProviderContext.dirty,
        versionProviderContext.branch,
        versionProviderContext.commits,
        versionProviderContext.versionTags,
        versionProviderContext.commitsSinceLastVersionTag,
        versionProviderContext.commitsSinceLastReleaseVersionTag
    )
}
