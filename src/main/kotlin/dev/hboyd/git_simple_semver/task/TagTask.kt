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

package dev.hboyd.git_simple_semver.task

import org.eclipse.jgit.api.Git
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import javax.inject.Inject

@DisableCachingByDefault
abstract class TagTask @Inject constructor(
    private val nameSupplier: () -> String,
    private val messageSupplier: () -> String,
    description: String?,
    group: String? = "Other",
) : DefaultTask() {
    init {
        this.group = group
        this.description = description
    }

    @TaskAction
    fun tag() {
        Git.open(project.projectDir).use {
            it.tag()
            .setAnnotated(true)
            .setName(nameSupplier.invoke())
            .setMessage(messageSupplier.invoke())
            .call()
            .also {
                logger.info("Tagged ${it.peeledObjectId} with name ${it.name}")
            }
        }
    }
}