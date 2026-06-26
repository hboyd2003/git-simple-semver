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

import dev.hboyd.git_simple_semver.semver.SemanticVersion
import org.gradle.api.Project
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

class LazyVersionTest {

    @Test
    fun `lazy version returns default version before project is configured`() {
        val lazyVersion = LazyVersion(
            { SemanticVersion(1, 0, 0).toString() },
            { false }
        )
        Assertions.assertEquals(Project.DEFAULT_VERSION, lazyVersion.toString())
    }

    @Test
    fun `lazy version retrieves version after project is configured`() {
        val version = "1.0.0"
        val lazyVersion = LazyVersion(
            { version },
            { true }
        )
        Assertions.assertEquals(version, lazyVersion.toString())
    }

    @Test
    fun `lazy version serializes with retrieved version after project is configured`() {
        val version = "1.0.0"
        val lazyVersion = LazyVersion(
            { version },
            { true }
        )
        val serializedVersion = ByteArrayOutputStream().use {
            ObjectOutputStream(it).use { outputStream ->
                outputStream.writeObject(lazyVersion)
            }
            it.toByteArray()
        }

        ByteArrayInputStream(serializedVersion).use { byteArrayInputStream ->
            ObjectInputStream(byteArrayInputStream).use {
                val deserializedVersion = it.readObject() as LazyVersion
                Assertions.assertEquals(version, deserializedVersion.toString())
            }
        }
    }
}
