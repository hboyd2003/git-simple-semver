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
import dev.hboyd.git_simple_semver.conventional_commit.ConventionalCommitMatcher
import dev.hboyd.git_simple_semver.conventional_commit.toConventionalCommit
import dev.hboyd.git_simple_semver.semver.LightweightSemanticVersionTag
import dev.hboyd.git_simple_semver.semver.SemanticVersion
import dev.hboyd.git_simple_semver.semver.SemanticVersionTag
import dev.hboyd.git_simple_semver.semver.toSemanticVersionGitTag
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.*
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.util.FS
import java.io.File

/**
 * Generates a semantic version based on the conventional commits in a git repository.
 */
class GitVersionGenerator(
    val majorChangeMatchers: List<ConventionalCommitMatcher>,
    val minorChangeMatchers: List<ConventionalCommitMatcher>,
    val patchChangeMatchers: List<ConventionalCommitMatcher>,
    val considerMajorChangesAsMinorWhenNoRelease: Boolean,
    val ignoredCommitRegex: Regex,
    val versionTagPrefix: String,
    val preReleaseIdentifierProviders: List<SemanticVersionIdentifierProvider>,
    val buildIdentifierProviders: List<SemanticVersionIdentifierProvider>,
    val minimumVersionBump: BumpType = BumpType.NONE,
) {
    /**
     * Generates a version for the given [repositoryDir].
     */
    fun generateVersion(
        repositoryDir: File
    ): SemanticVersion {
        val repository: Repository = RepositoryBuilder()
            .setFS(FS.DETECTED)
            .setMustExist(true)
            .findGitDir(repositoryDir)
            .build()

        repository.use {
            return generateVersion(repository)
        }
    }

    /**
     * Generates a version for the given [repository].
     * Optionally accepts an existing [versionProviderContext] to use for version generation.
     */
    fun generateVersion(
        repository: Repository?,
        versionProviderContext: VersionProviderContext =
            if (repository != null) createVersionProviderContext(
                repository,
                versionTagPrefix,
                ignoredCommitRegex
            )
            else VersionProviderContext.EMPTY
    ): SemanticVersion {
        val lastReleaseTag: SemanticVersionTag? =
            versionProviderContext.versionTags.firstOrNull { it.preReleaseLabel.isEmpty() }
        val currentReleaseVersion: SemanticVersion =
            lastReleaseTag?.let { SemanticVersion.copyOf(it) } ?: SemanticVersion(0, 0, 0)
        val startCommit: ConventionalCommit? =
            if (lastReleaseTag == null) runCatching { versionProviderContext.commits.last() }.getOrNull()
            else versionProviderContext.commits.firstOrNull { it.commit == lastReleaseTag.commitId }

        var bump: BumpType = BumpType.NONE
        if (startCommit != null) {
            bump = versionProviderContext.commits.subList(0, versionProviderContext.commits.indexOf(startCommit))
                .maxOfOrNull { commit ->
                    when {
                        majorChangeMatchers.any { it.matches(commit) } || commit.breakingChange -> BumpType.MAJOR
                        minorChangeMatchers.any { it.matches(commit) } -> BumpType.MINOR
                        patchChangeMatchers.any { it.matches(commit) } -> BumpType.PATCH
                        else -> BumpType.NONE
                    }
                } ?: BumpType.NONE
        }

        versionProviderContext.commitsSinceLastReleaseVersionTag?.let {
            if (bump == BumpType.NONE && it > 0) bump = minimumVersionBump
        }

        if (bump == BumpType.MAJOR && considerMajorChangesAsMinorWhenNoRelease && lastReleaseTag == null)
            bump = BumpType.MINOR
        val currentVersion: SemanticVersion = currentReleaseVersion.bump(bump)

        val identifierProviderContext =
            IdentifierProviderContext(versionProviderContext, currentVersion, bump)

        preReleaseIdentifierProviders.map { it.getIdentity(identifierProviderContext) }
            .filter { !it.isNullOrBlank() }
            .map { it as String }
            .forEach { currentVersion.preReleaseLabel.add(it) }

        buildIdentifierProviders.map { it.getIdentity(identifierProviderContext) }
            .filter { !it.isNullOrBlank() }
            .map { it as String }
            .forEach { currentVersion.buildMetadataLabel.add(it) }

        return currentVersion
    }

    companion object {
        /**
         * Creates a [VersionProviderContext] for the specified [repository].
         * Only tags that start with [versionTagPrefix] and are in the current branch are considered.
         * Commits that match [ignoredCommitRegex] are ignored.
         */
        fun createVersionProviderContext(
            repository: Repository,
            versionTagPrefix: String = "v",
            ignoredCommitRegex: Regex = "^FIXUP.*".toRegex()
        ): VersionProviderContext {
            check(!(repository.isBare)) { "Repository must be a non-bare repository" }

            val branchRef: Ref? = repository.findRef(repository.branch) ?: repository.exactRef(Constants.HEAD)

            val commits: Map<ObjectId, RevCommit>
            val versionTags: List<SemanticVersionTag>
            if (branchRef != null && branchRef.objectId != null) {
                RevWalk(repository).use { walker ->
                    walker.markStart(walker.parseCommit(branchRef.objectId))

                    commits = walker.associateBy { it.id }.filterKeys { !ignoredCommitRegex.matches(it.name) }
                    versionTags = repository.refDatabase.getRefsByPrefix(Constants.R_TAGS + versionTagPrefix)
                        .map { repository.refDatabase.peel(it) }
                        .mapNotNull { tagRef ->
                            runCatching {
                                walker.parseTag(tagRef.objectId)?.toSemanticVersionGitTag()
                            }.getOrElse {
                                runCatching {
                                    LightweightSemanticVersionTag.fromLightweightTag(
                                        tagRef,
                                        walker.parseCommit(tagRef.objectId)
                                    )
                                }.getOrNull()
                            }
                        }.filter { it.commitId in commits }
                        .sortedWith { tagA, tagB ->
                            commits.values.indexOf(tagA.commitId).compareTo(commits.values.indexOf(tagB.commitId))
                        }
                }
            } else {
                commits = emptyMap()
                versionTags = emptyList()
            }

            val commitsSinceLastVersionTag: Int =
                runCatching { versionTags[0].commitId.let { commits.keys.indexOf(it) } }.getOrElse { commits.size }

            val lastReleaseVersionTaggedCommit: ObjectId? =
                versionTags.find { it.preReleaseLabel.isEmpty() }?.commitId
            val commitsSinceLastReleaseVersionTag: Int? =
                lastReleaseVersionTaggedCommit?.let { commits.keys.indexOf(it) }

            return VersionProviderContext(
                Git.wrap(repository).status().call().hasUncommittedChanges(),
                repository.branch,
                commits.values.mapNotNull { runCatching { it.toConventionalCommit() }.getOrNull() }.toList(),
                versionTags,
                commitsSinceLastVersionTag,
                commitsSinceLastReleaseVersionTag
            )
        }
    }
}
