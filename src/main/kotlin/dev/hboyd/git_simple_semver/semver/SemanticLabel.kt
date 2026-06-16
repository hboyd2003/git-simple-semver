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


/**
 * Represents a semantic label.
 *
 * @see <a href="https://semver.org/spec/v2.0.0.html#spec-item-9">Semantic Versioning 2.0.0 - pre-release specification</a>
 * @see <a href="https://semver.org/spec/v2.0.0.html#spec-item-10">Semantic Versioning 2.0.0 - build metadata specification</a>
 */
sealed class SemanticLabel(val prefix: String) : ArrayList<String>() {

    companion object {
        /**
         * The separator used to separate identifiers in a semantic label.
         */
        const val IDENTIFIER_SEPARATOR: Char = '.'

        /**
         * A regex that matches a semantic label identifier.
         */
        val IDENTIFIER_REGEX: Regex = Regex("^[a-zA-Z0-9\\-]*$")
    }

    init {
        require(all { it.matches(IDENTIFIER_REGEX) }) {
            "Semantic label identifiers must match $IDENTIFIER_REGEX"
        }
    }

    /**
     * Returns the semantic label as a string without the prefix (e.g. '-' or '+').
     */
    fun buildLabelWithoutPrefix(): String = joinToString(IDENTIFIER_SEPARATOR.toString())

    /**
     * Returns the semantic label as a string.
     */
    fun buildLabel(): String {
        if (isEmpty()) return ""

        return prefix + buildLabelWithoutPrefix()
    }

    override fun toString(): String = buildLabel()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as SemanticLabel

        return prefix == other.prefix
    }

    override fun hashCode(): Int {
        return 31 * super.hashCode() + prefix.hashCode()
    }

    /**
     * Represents a pre-release semantic label.
     *
     * @see <a href="https://semver.org/spec/v2.0.0.html#spec-item-9">Semantic Versioning 2.0.0 - pre-release specification</a>
     */
    class PreRelease() : SemanticLabel("-"),
        Comparable<PreRelease> {
        constructor(identifiers: Collection<String>) : this() {
            addAll(identifiers)
        }

        constructor(identifiers: String) : this() {
            if (identifiers.isNotBlank()) addAll(identifiers.split(IDENTIFIER_SEPARATOR))
        }

        /**
         * Compares to another [PreRelease] semantic label based on their identifiers in the following order:
         *
         * 1. Identifiers consisting of only digits are compared numerically.
         * 2. Identifiers with letters or hyphens are compared lexically in ASCII sort order.
         * 3. Numeric identifiers always have lower precedence than non-numeric identifiers.
         * 4. A larger set of pre-release fields has a higher precedence than a smaller set, if all of the preceding identifiers are equal.
         *
         * @see <a href="https://semver.org/spec/v2.0.0.html#spec-item-11">Semantic Versioning 2.0.0 - pre-release precedence specification</a>
         */
        override fun compareTo(other: PreRelease): Int {
            this.zip(other).forEach { (thisIdentifier, otherIdentifier) ->
                val compareResult: Int =
                    if (thisIdentifier.matches(Regex("\\d+")) && otherIdentifier.matches(Regex("\\d+")))
                        thisIdentifier.toInt().compareTo(otherIdentifier.toInt())
                    else
                        thisIdentifier.compareTo(otherIdentifier)

                if (compareResult != 0) return compareResult
            }

            return this.size.compareTo(other.size)
        }
    }

    /**
     * Represents a build metadata semantic label.
     *
     * @see <a href="https://semver.org/spec/v2.0.0.html#spec-item-10">Semantic Versioning 2.0.0 - build metadata specification</a>
     */
    class BuildMetadata() : SemanticLabel("+") {
        constructor(identifiers: Collection<String>) : this() {
            addAll(identifiers)
        }

        constructor(identifiers: String) : this() {
            if (identifiers.isNotBlank()) addAll(identifiers.split(IDENTIFIER_SEPARATOR))
        }
    }
}

fun String.toPrereleaseSemanticLabel(): SemanticLabel.PreRelease =
    SemanticLabel.PreRelease(this)

fun String.toBuildMetadataSemanticLabel(): SemanticLabel.BuildMetadata =
    SemanticLabel.BuildMetadata(this)
