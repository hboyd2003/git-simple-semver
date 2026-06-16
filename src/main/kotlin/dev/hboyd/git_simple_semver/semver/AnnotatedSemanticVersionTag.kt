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

import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.revwalk.RevTag

/**
 * A [SemanticVersionTag] that is backed by an annotated tag ([RevTag]).
 */
class AnnotatedSemanticVersionTag(
    prefix: String = "",
    major: Int,
    minor: Int,
    patch: Int,
    preReleaseLabel: SemanticLabel.PreRelease = SemanticLabel.PreRelease(),
    buildMetadataLabel: SemanticLabel.BuildMetadata = SemanticLabel.BuildMetadata(),
    val annotatedTag: RevTag,
) : SemanticVersionTag(prefix, major, minor, patch, preReleaseLabel, buildMetadataLabel) {
    override val commitId: ObjectId
        get() = annotatedTag.`object`.id

    constructor(semanticVersion: SemanticVersion, prefix: String, annotatedTag: RevTag) : this(
        prefix,
        semanticVersion.major,
        semanticVersion.minor,
        semanticVersion.patch,
        semanticVersion.preReleaseLabel,
        semanticVersion.buildMetadataLabel,
        annotatedTag
    )
}

/**
 * Converts a [RevTag] to an [AnnotatedSemanticVersionTag].
 */
fun RevTag.toSemanticVersionGitTag(): AnnotatedSemanticVersionTag {
    val tagValues: List<String> = SemanticVersionTag.SEMANTIC_VERSION_TAG_REGEX.find(tagName)?.groupValues
        ?: throw IllegalArgumentException("Tag name \"$name\" is not a valid semantic version tag")
    val versionValues: List<String> =
        SemanticVersion.SEMANTIC_VERSION_REGEX.find(tagValues[2])?.groupValues
            ?: throw IllegalArgumentException("Tag name \"$name\" is not a valid semantic version tag")

    return AnnotatedSemanticVersionTag(
        tagValues[1],
        versionValues[1].toInt(),
        versionValues[2].toInt(),
        versionValues[3].toInt(),
        versionValues[4].toPrereleaseSemanticLabel(),
        versionValues[5].toBuildMetadataSemanticLabel(),
        this
    )
}
