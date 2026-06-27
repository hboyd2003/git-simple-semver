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

import dev.hboyd.git_simple_semver.task.SimplePrintTask
import dev.hboyd.git_simple_semver.task.TagTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication

abstract class GitSimpleSemver : Plugin<Project> {

    override fun apply(project: Project): Unit = with(project) {
        pluginManager.apply(JavaPlugin::class.java)
        val extension: GitSimpleSemverExtension =
            extensions.create("gitSimpleSemver", GitSimpleSemverExtension::class.java)

        // "Lazily" resolve the version to ensure it is resolved as early as possible.
        project.version = LazyVersion({ extension.version.toString() }, { project.state.executed })

        allprojects {
            it.afterEvaluate { project ->
                if (!extension.includeBuildIdentifierInPublishedVersion.get()
                    && project.plugins.hasPlugin("maven-publish")
                ) {
                    project.extensions.getByType(PublishingExtension::class.java).publications
                        .withType(MavenPublication::class.java).configureEach { publication ->
                            publication.version = extension.version.buildVersionString(
                                includePreReleaseLabel = true,
                                includeBuildMetadataLabel = false
                            )
                        }
                }
            }
        }

        afterEvaluate {
            logger.lifecycle("Resolved Version: ${project.version}")
        }

        project.tasks.register(
            "printVersion",
            SimplePrintTask::class.java,
            { extension.version },
            "Prints the current version",
            "versioning"
        )

        project.tasks.register(
            "printCoreVersion",
            SimplePrintTask::class.java,
            { extension.version.buildCoreVersionString() },
            "Prints the current version without any pre-release or build metadata identifiers",
            "versioning"
        )

        project.tasks.register(
            "printVersionWithoutBuildMetadata",
            SimplePrintTask::class.java,
            { extension.version.buildVersionString(includePreReleaseLabel = true, includeBuildMetadataLabel = false) },
            "Prints the current version without any build metadata identifiers",
            "versioning"
        )

        project.tasks.register(
            "printVersionContext",
            SimplePrintTask::class.java,
            { extension.versionContext },
            "Prints the context used to generate the version",
            "versioning"
        )

        project.tasks.register(
            "tagWithCoreVersion",
            TagTask::class.java,
            { extension.versionTagPrefix.get() + extension.coreVersion },
            { extension.versionTagPrefix.get() + extension.coreVersion },
            "Creates a new annotated tag with the current core version with the configured prefix",
            "versioning"
        )
    }
}
