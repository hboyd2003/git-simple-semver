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

/**
 * Represents a git tag whose tag name is a semantic version possibly pre-fixed with text which does not contain
 * any digits or newlines.
 */
abstract class SemanticVersionTag(
    val prefix: String = "",
    major: Int,
    minor: Int,
    patch: Int,
    preReleaseLabel: SemanticLabel.PreRelease = SemanticLabel.PreRelease(),
    buildMetadataLabel: SemanticLabel.BuildMetadata = SemanticLabel.BuildMetadata(),
) : SemanticVersion(major, minor, patch, preReleaseLabel, buildMetadataLabel) {
    abstract val commitId: ObjectId

    constructor(semanticVersion: SemanticVersion, prefix: String) : this(
        prefix,
        semanticVersion.major,
        semanticVersion.minor,
        semanticVersion.patch,
        semanticVersion.preReleaseLabel,
        semanticVersion.buildMetadataLabel,
    )

    companion object {
        /**
         * Regex that splits has two groups:
         * 1. The text before the first digit, `\n` or `\r` (if any)
         * 2. The rest of the text
         */
        val SEMANTIC_VERSION_TAG_REGEX: Regex =
            Regex("^([^0-9\\n\\r]*)(.*)$")
    }

    override fun toString(): String = prefix + super.toString()
}
