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

import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.util.RawParseUtils
import org.eclipse.jgit.util.RawParseUtils.guessEncoding
import java.nio.charset.Charset

/**
 * Represents a conventional commit v1.0.0.
 *
 * @see <a href="https://www.conventionalcommits.org/en/v1.0.0/">Conventional Commits</a>
 */
class ConventionalCommit(
    val commit: RevCommit,
) {
    init {
        CONVENTIONAL_COMMIT_SHORT_MESSAGE_REGEX.find(commit.shortMessage)?.groupValues
            ?: throw IllegalArgumentException("Commit \"${commit.shortMessage}\" (${commit.id.name}) is not a valid conventional commit")
    }

    companion object {
        val CONVENTIONAL_COMMIT_SHORT_MESSAGE_REGEX: Regex = Regex("^([^\\t\\n\\r:()!]+)(?:\\((.+)\\))?(!)?: ?(.+)$")
    }

    /**
     * The type of the commit.
     */
    val type: String
        get() = conventionalCommitShortMessageValues()[1]

    /**
     * The scope of the commit.
     */
    val scope: String?
        get() = conventionalCommitShortMessageValues()[2].ifEmpty { null }

    /**
     * Whether the commit is a breaking change.
     */
    val breakingChange: Boolean
        get() = conventionalCommitShortMessageValues()[3].isNotEmpty() || commit.footerLines.any { it.key == "BREAKING CHANGE" }

    /**
     * The description of the commit.
     */
    val description: String
        get() = conventionalCommitShortMessageValues()[4]

    /**
     * The body of the commit.
     */
    val body: String
        get() {
            val enc: Charset? = guessEncoding(commit.rawBuffer)

            // The first non-header line is never part of the body.
            val startOfBody = RawParseUtils.nextLfSkippingSplitLines(
                commit.rawBuffer,
                if (RawParseUtils.hasAnyKnownHeaders(commit.rawBuffer))
                    RawParseUtils.commitMessage(
                        commit.rawBuffer,
                        0
                    ) else 0
            )

            // Search for the beginning of footer
            var endOfBody = commit.rawBuffer.size
            while (endOfBody > startOfBody) {
                if (commit.rawBuffer[endOfBody - 1] == '\n'.code.toByte()
                    && commit.rawBuffer[endOfBody - 2] == '\n'.code.toByte()
                    && RawParseUtils.endOfFooterLineKey(commit.rawBuffer, endOfBody) != -1
                )
                    break

                --endOfBody
            }
            if (endOfBody <= startOfBody)
                endOfBody = commit.rawBuffer.size

            return RawParseUtils.decode(enc, commit.rawBuffer, startOfBody, endOfBody)
                .trim { char -> (char == '\n' || char == '\r') }
        }

    private fun conventionalCommitShortMessageValues(): List<String> {
        return CONVENTIONAL_COMMIT_SHORT_MESSAGE_REGEX.find(commit.shortMessage)?.groupValues
            ?: throw IllegalStateException("Failed to parse conventional commit message")
    }

    override fun toString(): String {
        return "ConventionalCommit(commit=${commit.name}, type='$type', scope=$scope, breakingChange=$breakingChange, description='$description', body='$body')"
    }
}

/**
 * Returns a new [ConventionalCommit] created from itself.
 */
fun RevCommit.toConventionalCommit(): ConventionalCommit = ConventionalCommit(this)