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

import dev.hboyd.git_simple_semver.git_semver.BumpType

/**
 * Represents a version 2.0.0 semantic version.
 *
 * @see <a href="https://semver.org/spec/v2.0.0.html">Semantic Versioning 2.0.0</a>
 */
open class SemanticVersion(
    var major: Int,
    var minor: Int,
    var patch: Int,
    val preReleaseLabel: SemanticLabel.PreRelease = SemanticLabel.PreRelease(),
    val buildMetadataLabel: SemanticLabel.BuildMetadata = SemanticLabel.BuildMetadata(),
) : Comparable<SemanticVersion> {
    companion object {
        /**
         * A regex that matches an entire semantic version string.
         * With groups for major, minor, patch, pre-release label, and build metadata label.
         */
        val SEMANTIC_VERSION_REGEX: Regex =
            Regex("^(\\d+)\\.(\\d+)\\.(\\d+)(?:-?((?:[A-z0-9-]+\\.?)+))?(?:\\+((?:[A-z0-9-]+\\.?)+))?$")

        /**
         * Creates a new [SemanticVersion] with the same values as the given [version].
         */
        fun copyOf(version: SemanticVersion): SemanticVersion = SemanticVersion(
            version.major,
            version.minor,
            version.patch,
            version.preReleaseLabel,
            version.buildMetadataLabel,
        )
    }

    /**
     * Returns the version as a [String] excluding any pre-release or build metadata labels.
     */
    fun buildCoreVersionString(): String = "$major.$minor.$patch"

    /**
     * Returns the full version as a [String] including the pre-release and build metadata labels if enabled by
     * [includePreReleaseLabel] and [includeBuildMetadataLabel] respectively.
     *
     * @param includePreReleaseLabel Whether to include the pre-release label.
     * @param includeBuildMetadataLabel Whether to include the build metadata label.
     */
    fun buildVersionString(includePreReleaseLabel: Boolean = true, includeBuildMetadataLabel: Boolean = true): String {
        val versionBuilder: StringBuilder = StringBuilder()

        versionBuilder.append(buildCoreVersionString())
        if (includePreReleaseLabel) versionBuilder.append(preReleaseLabel)
        if (includeBuildMetadataLabel) versionBuilder.append(buildMetadataLabel)

        return versionBuilder.toString()
    }

    /**
     * Returns a new [SemanticVersion] with its version bumped by the specified [bumpType].
     */
    fun bump(bumpType: BumpType): SemanticVersion = when (bumpType) {
        BumpType.MAJOR -> SemanticVersion(major + 1, minor, patch, preReleaseLabel, buildMetadataLabel)
        BumpType.MINOR -> SemanticVersion(major, minor + 1, patch, preReleaseLabel, buildMetadataLabel)
        BumpType.PATCH -> SemanticVersion(major, minor, patch + 1, preReleaseLabel, buildMetadataLabel)
        BumpType.NONE -> this
    }

    override fun compareTo(other: SemanticVersion): Int = compareBy<SemanticVersion> { it.major }
        .thenBy { it.minor }
        .thenBy { it.patch }
        .thenBy { it.preReleaseLabel }
        .compare(this, other)

    override fun toString(): String = buildVersionString()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SemanticVersion

        return major == other.major
                && minor == other.minor
                && patch == other.patch
                && preReleaseLabel == other.preReleaseLabel
                && buildMetadataLabel == other.buildMetadataLabel
    }

    override fun hashCode(): Int {
        var result = major
        result = 31 * result + minor
        result = 31 * result + patch
        result = 31 * result + preReleaseLabel.hashCode()
        result = 31 * result + buildMetadataLabel.hashCode()
        return result
    }
}

fun String.toSemanticVersion(): SemanticVersion {
    val matchResult = SemanticVersion.SEMANTIC_VERSION_REGEX.find(this)
        ?: throw IllegalArgumentException("Version string $this is not a valid semantic version")

    return SemanticVersion(
        matchResult.groupValues[1].toInt(),
        matchResult.groupValues[2].toInt(),
        matchResult.groupValues[3].toInt(),
        matchResult.groupValues[4].toPrereleaseSemanticLabel(),
        matchResult.groupValues[5].toBuildMetadataSemanticLabel()
    )
}
