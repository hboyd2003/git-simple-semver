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

import org.gradle.api.Project
import java.io.Serializable

class LazyVersion(
    @Transient
    val versionProvider: (() -> String)? = null,
    @Transient
    val projectConfigurationStateProvider: (() -> Boolean)? = null
) : Serializable {
    private var _version: String? = null

    private val version: String
        get() {
            if (_version != null) return _version.toString()

            if ((projectConfigurationStateProvider?.invoke() ?: false) && versionProvider != null) {
                _version = versionProvider.invoke()
                return _version.toString()
            }
            return Project.DEFAULT_VERSION
        }

    private fun writeReplace(): Any {
        version
        return this
    }

    override fun toString(): String {
        return version
    }

}