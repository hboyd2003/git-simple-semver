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

package dev.hboyd.git_simple_semver.semver

import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.Ref

/**
 * A [SemanticVersionTag] that is backed by a lightweight tag.
 */
class LightweightSemanticVersionTag(
    prefix: String = "",
    major: Int,
    minor: Int,
    patch: Int,
    preReleaseLabel: SemanticLabel.PreRelease = SemanticLabel.PreRelease(),
    buildMetadataLabel: SemanticLabel.BuildMetadata = SemanticLabel.BuildMetadata(),
    override val commitId: ObjectId
) : SemanticVersionTag(prefix, major, minor, patch, preReleaseLabel, buildMetadataLabel) {

    companion object {
        /**
         * Creates a [LightweightSemanticVersionTag] from the given [ref] and [commitId].
         */
        fun fromLightweightTag(ref: Ref, commitId: ObjectId): LightweightSemanticVersionTag {
            val tagValues: List<String> =
                SEMANTIC_VERSION_TAG_REGEX.find(ref.name.substringAfter(Constants.R_TAGS))?.groupValues
                    ?: throw IllegalArgumentException("Tag name $ref is not a valid semantic version tag")
            val versionValues: List<String> =
                SEMANTIC_VERSION_REGEX.find(tagValues[2])?.groupValues
                    ?: throw IllegalArgumentException("Tag name $ref is not a valid semantic version tag")

            return LightweightSemanticVersionTag(
                tagValues[1],
                versionValues[1].toInt(),
                versionValues[2].toInt(),
                versionValues[3].toInt(),
                versionValues[4].toPrereleaseSemanticLabel(),
                versionValues[5].toBuildMetadataSemanticLabel(),
                commitId
            )
        }
    }

    constructor(prefix: String, semanticVersion: SemanticVersion, commitId: ObjectId) : this(
        prefix,
        semanticVersion.major,
        semanticVersion.minor,
        semanticVersion.patch,
        semanticVersion.preReleaseLabel,
        semanticVersion.buildMetadataLabel,
        commitId
    )
}