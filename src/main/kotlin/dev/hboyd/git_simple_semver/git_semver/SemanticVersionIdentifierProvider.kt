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

/**
 * Provider for a semantic version identifier based on the given [IdentifierProviderContext].
 */
fun interface SemanticVersionIdentifierProvider {
    /**
     * Returns a semantic version identifier based on the given [context].
     */
    fun getIdentity(context: IdentifierProviderContext): String?

    /**
     * Returns a new [SemanticVersionIdentifierProvider] that only provides this provider if the repo has any changes
     * beyond that of the last release version tag including uncommitted changes.
     */
    fun onlyIfChanges(): SemanticVersionIdentifierProvider = SemanticVersionIdentifierProvider { ctx ->
        if (ctx.dirty || ctx.commitsSinceLastReleaseVersionTag?.let { it > 0 } == true)
            this.getIdentity(ctx)
        else null
    }

    /**
     * Returns a new [SemanticVersionIdentifierProvider] that only provides this provider if the repo is dirty
     * (i.e., has uncommitted changes) otherwise returns `null`.
     */
    fun onlyIfDirty(): SemanticVersionIdentifierProvider = SemanticVersionIdentifierProvider {
        if (it.dirty) this.getIdentity(it) else null
    }

    /**
     * Returns a new [SemanticVersionIdentifierProvider] that only provides this provider if the repo has commits
     * beyond that of the last release version tag.
     */
    fun onlyIfUnreleasedChanges(): SemanticVersionIdentifierProvider = SemanticVersionIdentifierProvider { ctx ->
        ctx.commitsSinceLastReleaseVersionTag?.let { if (it > 0) this.getIdentity(ctx) else null }
    }

    /**
     * Returns a new [SemanticVersionIdentifierProvider] that only provides this provider if the current branch matches
     * the given [branchRegex].
     */
    fun onlyIfBranch(branchRegex: Regex): SemanticVersionIdentifierProvider = SemanticVersionIdentifierProvider { ctx ->
        ctx.branch.let { if (it.matches(branchRegex)) this.getIdentity(ctx) else null }
    }
}
