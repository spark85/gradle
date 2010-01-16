/*
 * Copyright 2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gradle.api.internal.changedetection


import static org.junit.Assert.*
import static org.hamcrest.Matchers.*

import org.gradle.util.JUnit4GroovyMockery
import org.jmock.integration.junit4.JMock
import org.junit.runner.RunWith
import org.junit.Test
import org.junit.Rule
import org.gradle.util.TemporaryFolder
import org.gradle.util.TestFile
import org.gradle.api.file.FileCollection
import org.gradle.api.internal.changedetection.FileCollectionSnapshot.ChangeListener

@RunWith(JMock.class)
public class DefaultFileSnapshotterTest {
    private final JUnit4GroovyMockery context = new JUnit4GroovyMockery()
    private final Hasher hasher = new DefaultHasher()
    private int counter
    private FileCollectionSnapshot.ChangeListener listener = context.mock(FileCollectionSnapshot.ChangeListener.class)
    private final DefaultFileSnapshotter snapshotter = new DefaultFileSnapshotter(hasher)
    @Rule
    public final TemporaryFolder tmpDir = new TemporaryFolder()

    @Test
    public void notifiesListenerWhenFileAdded() {
        TestFile file1 = tmpDir.createFile('file1')
        TestFile file2 = tmpDir.createFile('file2')

        FileCollectionSnapshot snapshot = snapshotter.snapshot(files(file1))

        context.checking {
            one(listener).added(file2)
        }
        snapshotter.snapshot(files(file1, file2)).changesSince(snapshot, listener)
    }

    @Test
    public void notifiesListenerWhenFileRemoved() {
        TestFile file1 = tmpDir.createFile('file1')
        TestFile file2 = tmpDir.createFile('file2')

        FileCollectionSnapshot snapshot = snapshotter.snapshot(files(file1, file2))

        context.checking {
            one(listener).removed(file2)
        }
        snapshotter.snapshot(files(file1)).changesSince(snapshot, listener)
    }

    @Test
    public void fileIsUpToDateWhenTypeAndHashHaveNotChanged() {
        TestFile file = tmpDir.createFile('file')

        FileCollectionSnapshot snapshot = snapshotter.snapshot(files(file))
        assertThat(snapshot, notNullValue())

        snapshotter.snapshot(files(file)).changesSince(snapshot, listener)
    }

    @Test
    public void fileHasChangedWhenTypeHasChanged() {
        TestFile file = tmpDir.createFile('file')

        FileCollectionSnapshot snapshot = snapshotter.snapshot(files(file))

        file.delete()
        file.createDir()

        context.checking {
            one(listener).changed(file)
        }
        snapshotter.snapshot(files(file)).changesSince(snapshot, listener)
    }

    @Test
    public void fileIsOutOfDateWhenHashHasChanged() {
        TestFile file = tmpDir.createFile('file')

        FileCollectionSnapshot snapshot = snapshotter.snapshot(files(file))

        file.write('new content')

        context.checking {
            one(listener).changed(file)
        }
        snapshotter.snapshot(files(file)).changesSince(snapshot, listener)
    }

    @Test
    public void directoryIsUpToDateWhenTypeHasNotChanged() {
        TestFile dir = tmpDir.createDir('dir')

        FileCollectionSnapshot snapshot = snapshotter.snapshot(files(dir))

        snapshotter.snapshot(files(dir)).changesSince(snapshot, listener)
    }

    @Test
    public void directoryIsOutOfDateWhenTypeHasChanged() {
        TestFile dir = tmpDir.createDir('dir')

        FileCollectionSnapshot snapshot = snapshotter.snapshot(files(dir))

        dir.deleteDir()
        dir.createFile()

        context.checking {
            one(listener).changed(dir)
        }
        snapshotter.snapshot(files(dir)).changesSince(snapshot, listener)
    }

    @Test
    public void nonExistentFileIsUpToDateWhenTypeHasNotChanged() {
        TestFile file = tmpDir.file('unknown')

        FileCollectionSnapshot snapshot = snapshotter.snapshot(files(file))

        snapshotter.snapshot(files(file)).changesSince(snapshot, listener)
    }

    @Test
    public void nonExistentFileIsOutOfDateWhenTypeHasChanged() {
        TestFile file = tmpDir.file('unknown')

        FileCollectionSnapshot snapshot = snapshotter.snapshot(files(file))

        file.createFile()

        context.checking {
            one(listener).changed(file)
        }
        snapshotter.snapshot(files(file)).changesSince(snapshot, listener)
    }

    @Test
    public void ignoresDuplicatesInFileCollection() {
        TestFile file1 = tmpDir.createFile('file')
        TestFile file2 = tmpDir.createFile('file')

        FileCollectionSnapshot snapshot = snapshotter.snapshot(files(file1, file2))

        snapshotter.snapshot(files(file1)).changesSince(snapshot, listener)
    }

    private FileCollection files(File... files) {
        FileCollection collection = context.mock(FileCollection.class, "collection ${counter++}")
        context.checking {
            allowing(collection).iterator()
            will(returnIterator(files as List))
        }
        return collection
    }
    
}
