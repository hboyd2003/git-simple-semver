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

package dev.hboyd.git_simple_semver

import dev.hboyd.git_simple_semver.conventional_commit.ConventionalCommitMatcher
import dev.hboyd.git_simple_semver.git_semver.*
import dev.hboyd.git_simple_semver.semver.SemanticVersion
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.lib.RepositoryBuilder
import org.eclipse.jgit.util.FS
import org.gradle.api.file.ProjectLayout
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Nested
import javax.inject.Inject

abstract class GitSimpleSemverExtension @Inject constructor(
    private val layout: ProjectLayout,
    private val objects: ObjectFactory
) {
    /**
     * The selectors for the types of changes that will trigger a major version bump.
     * This is in addition to the '!' suffix and the 'BREAKING CHANGE' footer.
     */
    @get:Nested
    val majorChangeSelections: ListProperty<TypeScopeSelectionSpec> =
        objects.listProperty(TypeScopeSelectionSpec::class.java)
            .convention(emptyList())

    /**
     * The selectors for the types of changes that will trigger a minor version bump.
     */
    @get:Nested
    val minorChangeSelections: ListProperty<TypeScopeSelectionSpec> =
        objects.listProperty(TypeScopeSelectionSpec::class.java).convention(
            listOf(
                changeSpec("perf"),
                changeSpec("feat"),
                changeSpec("refactor")
            )
        )

    /**
     * The selectors for the types of changes that will trigger a patch version bump.
     */
    @get:Nested
    val patchChangeSelections: ListProperty<TypeScopeSelectionSpec> =
        objects.listProperty(TypeScopeSelectionSpec::class.java).convention(
            listOf(
                changeSpec("fix"),
                changeSpec("style"),
                changeSpec("docs"),
                changeSpec("test"),
                changeSpec("chore"),
                changeSpec("build")
                )
        )

    /**
     * The minimum bump to be applied if there are commits since the last release tag and none of those commits
     * are matched with the change selectors.
     */
    val minimumVersionBump: Property<BumpType> = objects.property(BumpType::class.java)
        .convention(BumpType.PATCH)

    /**
     * Include the build identifier in the Maven publication version.
     */
    val includeBuildIdentifierInPublishedVersion: Property<Boolean> = objects.property(Boolean::class.java)
        .convention(false)

    /**
     * If true, major changes will be considered minor changes when no release has been made yet.
     */
    val considerMajorChangesAsMinorWhenNoRelease: Property<Boolean> = objects.property(Boolean::class.java)
        .convention(true)

    /**
     * Commits that will be ignored when calculating the version.
     */
    val ignoredCommitRegex: Property<String> = objects.property(String::class.java)
        .convention("^(?:FIXUP|AMEND|MERGE|Merge).*")

    /**
     * The prefix given to version tags.
     */
    val versionTagPrefix: Property<String> = objects.property(String::class.java)
        .convention("v")

    /**
     * Providers to use for generating pre-release identifiers.
     */
    val preReleaseIdentifierProviders: ListProperty<SemanticVersionIdentifierProvider> =
        objects.listProperty(SemanticVersionIdentifierProvider::class.java)
            .convention(listOf(
                textProvider("SNAPSHOT").onlyIfChanges(),
                branchProvider().onlyIfBranch(Regex("^(?!main|master|trunk|release.*)$")),
            ))

    /**
     * Providers to use for generating build metadata identifiers.
     */
    val buildIdentifierProviders: ListProperty<SemanticVersionIdentifierProvider> =
        objects.listProperty(SemanticVersionIdentifierProvider::class.java)
            .convention(listOf(
                commitsSinceReleaseProvider().onlyIfChanges(),
                dateTimeProvider().onlyIfDirty()))

    /**
     * The generator used to calculate the version.
     * Once resolved, this will never change.
     */
    val gitVersionGenerator by lazy {
        GitVersionGenerator(
            majorChangeSelections.get().map { it.asConventionCommitMatcher() },
            minorChangeSelections.get().map { it.asConventionCommitMatcher() },
            patchChangeSelections.get().map { it.asConventionCommitMatcher() },
            considerMajorChangesAsMinorWhenNoRelease.get(),
            ignoredCommitRegex.get().toRegex(),
            versionTagPrefix.get(),
            preReleaseIdentifierProviders.get(),
            buildIdentifierProviders.get(),
            minimumVersionBump.get()
        )
    }

    /**
     * The repository used to calculate the version.
     * Once resolved, this will never change.
     */
    private val repository: Repository by lazy {
        RepositoryBuilder()
            .setFS(FS.DETECTED)
            .setMustExist(true)
            .findGitDir(layout.projectDirectory.asFile)
            .build()
    }

    /**
     * The context used to calculate the version.
     * Once resolved, this will never change.
     */
    val versionContext: VersionProviderContext by lazy {
        GitVersionGenerator.createVersionProviderContext(
            repository,
            versionTagPrefix.get(),
            ignoredCommitRegex.get().toRegex()
        )
    }

    /**
     * The current version of the project, excluding any pre-release or build metadata identifiers.
     */
    val coreVersion: SemanticVersion by lazy { SemanticVersion(version.major, version.minor, version.patch) }

    /**
     * The current version of the project.
     */
    val version: SemanticVersion by lazy { gitVersionGenerator.generateVersion(repository, versionContext) }

    abstract class TypeScopeSelectionSpec {
        abstract val typeRegex: Property<String>
        abstract val scopeRegex: Property<String>

        fun asConventionCommitMatcher(): ConventionalCommitMatcher =
            ConventionalCommitMatcher(typeRegex.get().toRegex(), scopeRegex.get().toRegex())
    }

    /**
     * Creates a [TypeScopeSelectionSpec] with the specified [typeRegex] and [scopeRegex].
     */
    fun changeSpec(typeRegex: String, scopeRegex: String = ".*"): TypeScopeSelectionSpec {
        val typeScopeSelectionSpec: TypeScopeSelectionSpec = objects.newInstance(TypeScopeSelectionSpec::class.java)
        typeScopeSelectionSpec.typeRegex.set(typeRegex)
        typeScopeSelectionSpec.scopeRegex.set(scopeRegex)
        return typeScopeSelectionSpec
    }
}