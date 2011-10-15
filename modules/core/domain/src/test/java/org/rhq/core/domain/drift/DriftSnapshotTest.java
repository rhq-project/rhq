package org.rhq.core.domain.drift;

import static java.util.Arrays.asList;
import static org.rhq.core.domain.drift.DriftCategory.FILE_ADDED;
import static org.rhq.core.domain.drift.DriftCategory.FILE_CHANGED;
import static org.rhq.core.domain.drift.DriftCategory.FILE_REMOVED;
import static org.rhq.core.domain.drift.DriftChangeSetCategory.COVERAGE;
import static org.rhq.core.domain.drift.DriftChangeSetCategory.DRIFT;
import static org.rhq.core.domain.drift.DriftFileStatus.LOADED;
import static org.rhq.test.AssertUtils.assertCollectionMatchesNoOrder;
import static org.testng.Assert.assertEquals;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.testng.annotations.Test;

import org.rhq.core.domain.drift.DriftConfigurationDefinition.DriftHandlingMode;
import org.rhq.core.domain.drift.DriftSnapshot.DriftSnapshotDirectory;

@SuppressWarnings("unchecked")
public class DriftSnapshotTest {

    @Test
    public void addChangeSetWithAddedFile() {
        int defId = 1;
        DriftSnapshotRequest request = new DriftSnapshotRequest(defId);

        FakeDriftChangeSet changeSet = new FakeDriftChangeSet(0, COVERAGE, defId).add(new FakeDrift(FILE_ADDED, null,
            new FakeDriftFile("a1b2c3", 1024, LOADED), "/drift/1.txt"));

        Set drifts = changeSet.getDrifts();
        DriftSnapshot snapshot = new DriftSnapshot(request).addChangeSet(changeSet);

        assertCollectionMatchesNoOrder(drifts, snapshot.getDriftInstances(), "Failed to build "
            + "a snapshot that contains a single change set");
        assertEquals(snapshot.getVersion(), changeSet.getVersion(), "Snapshot version is wrong");
    }

    @Test
    public void addChangeSetsWithAddedFiles() {
        int defId = 1;
        DriftSnapshotRequest request = new DriftSnapshotRequest(defId, 1);

        Drift<?, ?> entry1 = new FakeDrift(FILE_ADDED, null, new FakeDriftFile("a1b2c3", 1024, LOADED), "/drift/1.txt");
        FakeDriftChangeSet changeSet0 = new FakeDriftChangeSet(0, COVERAGE, defId).add(entry1);

        Drift<?, ?> entry2 = new FakeDrift(FILE_ADDED, null, new FakeDriftFile("4a5b6c", 1024, LOADED), "/drift/2.txt");
        FakeDriftChangeSet changeSet1 = new FakeDriftChangeSet(1, DRIFT, defId).add(entry2);

        DriftSnapshot snapshot = new DriftSnapshot(request).addChangeSet(changeSet0).addChangeSet(changeSet1);

        // not sure but asSet with one arg was not working for me, so create Set manually
        Set set = new HashSet<Drift<?, ?>>();
        set.add(entry1);
        set.add(entry2);
        assertCollectionMatchesNoOrder(set, snapshot.getDriftInstances(), "Failed to build snapshot "
            + "with two change sets and file added in second change set");
        assertEquals(snapshot.getVersion(), changeSet1.getVersion(), "Snapshot version is wrong");
    }

    @Test
    public void replaceFileWithChangedVersion() {
        int defId = 1;
        DriftSnapshotRequest request = new DriftSnapshotRequest(defId, 1);

        Drift<?, ?> entry1 = new FakeDrift(FILE_ADDED, null, new FakeDriftFile("a1b2c3", 1024, LOADED), "/drift/1.txt");
        FakeDriftChangeSet changeSet0 = new FakeDriftChangeSet(0, COVERAGE, defId).add(entry1);

        Drift<?, ?> entry2 = new FakeDrift(FILE_CHANGED, new FakeDriftFile("a1b2c3", 1024, LOADED), new FakeDriftFile(
            "4d5e6f", 1024, LOADED), "/drift/1.txt");
        FakeDriftChangeSet changeSet1 = new FakeDriftChangeSet(1, DRIFT, defId).add(entry2);

        DriftSnapshot snapshot = new DriftSnapshot(request).addChangeSet(changeSet0).addChangeSet(changeSet1);

        // not sure but asSet with one arg was not working for me, so create Set manually
        Set set = new HashSet<Drift<?, ?>>();
        set.add(entry2);
        assertCollectionMatchesNoOrder(set, snapshot.getDriftInstances(), "Failed to build snapshot with file changed");
    }

    @Test
    public void deleteFileThatHasBeenRemoved() {
        int defId = 1;
        DriftSnapshotRequest request = new DriftSnapshotRequest(defId, 1);

        Drift<?, ?> entry1 = new FakeDrift(FILE_ADDED, null, new FakeDriftFile("a1b2c3", 1024, LOADED), "/drift/1.txt");
        Drift<?, ?> entry2 = new FakeDrift(FILE_ADDED, null, new FakeDriftFile("d1f2a3", 1024, LOADED), "/drift/2.txt");
        FakeDriftChangeSet changeSet0 = new FakeDriftChangeSet(0, COVERAGE, defId).add(entry1).add(entry2);

        Drift entry3 = new FakeDrift(FILE_REMOVED, new FakeDriftFile("a1b2c3", 1024, LOADED), null, "/drift/1.txt");
        FakeDriftChangeSet changeSet1 = new FakeDriftChangeSet(1, DRIFT, defId).add(entry3);

        DriftSnapshot snapshot = new DriftSnapshot(request).addChangeSet(changeSet0).addChangeSet(changeSet1);

        // not sure but asSet with one arg was not working for me, so create Set manually
        Set set = new HashSet<Drift<?, ?>>();
        set.add(entry2);
        assertCollectionMatchesNoOrder(set, snapshot.getDriftInstances(), "Failed to build snapshot with a file "
            + "removed.");
    }

    @Test
    public void directorySnapshotTest() {
        int defId = 1;
        DriftSnapshotRequest request = new DriftSnapshotRequest(defId, 1, null, null, true, false);

        Drift<?, ?> entry1 = new FakeDrift(FILE_ADDED, null, new FakeDriftFile("a1b2c3", 1024, LOADED), "/drift/1.txt");
        Drift<?, ?> entry2 = new FakeDrift(FILE_ADDED, null, new FakeDriftFile("d1f2a3", 1024, LOADED), "/drift/2.txt");
        Drift<?, ?> entry3 = new FakeDrift(FILE_ADDED, null, new FakeDriftFile("abcd12", 1024, LOADED),
            "/another/1.txt");
        FakeDriftChangeSet changeSet0 = new FakeDriftChangeSet(0, COVERAGE, defId).add(entry1).add(entry2).add(entry3);

        Drift entry4 = new FakeDrift(FILE_REMOVED, new FakeDriftFile("a1b2c3", 1024, LOADED), null, "/drift/1.txt");
        FakeDriftChangeSet changeSet1 = new FakeDriftChangeSet(1, DRIFT, defId).add(entry4);

        DriftSnapshot snapshot = new DriftSnapshot(request).addChangeSet(changeSet0).addChangeSet(changeSet1);

        assert null == snapshot.getDriftInstances();
        assert null != snapshot.getDriftDirectories();
        assert 2 == snapshot.getDriftDirectories().size();
        Iterator<DriftSnapshotDirectory> iterator = snapshot.getDriftDirectories().iterator(); // should be sorted alphabetically
        DriftSnapshotDirectory dir = iterator.next();
        assert "/another".equals(dir.getDirectoryPath());
        assert 1 == dir.getFiles();
        assert 1 == dir.getAdded();
        assert 0 == dir.getChanged();
        assert 0 == dir.getRemoved();

        dir = iterator.next();
        assert "/drift".equals(dir.getDirectoryPath());
        assert 1 == dir.getFiles();
        assert 2 == dir.getAdded();
        assert 0 == dir.getChanged();
        assert 1 == dir.getRemoved();
    }

    @Test
    public void diffShowsEntriesInLeftAndNotInRight() {
        int defId = 1;
        DriftSnapshotRequest rightRequest = new DriftSnapshotRequest(defId);
        DriftSnapshotRequest leftRequest = new DriftSnapshotRequest(defId);

        Drift entry1 = new FakeDrift(FILE_ADDED, null, new FakeDriftFile("a1b2c3", 1024, LOADED), "/drift/1.txt");
        FakeDriftChangeSet rightChangeSet0 = new FakeDriftChangeSet(0, COVERAGE, defId).add(entry1);
        DriftSnapshot right = new DriftSnapshot(rightRequest).addChangeSet(rightChangeSet0);

        // recreate rightChangeSet0 because addChangeSet is destructive
        rightChangeSet0 = new FakeDriftChangeSet(0, COVERAGE, defId).add(entry1);
        Drift entry2 = new FakeDrift(FILE_ADDED, null, new FakeDriftFile("a3b6c9", 1024, LOADED), "/drift/2.txt");
        FakeDriftChangeSet leftChangeSet0 = new FakeDriftChangeSet(0, COVERAGE, defId).add(entry1).add(entry2);
        DriftSnapshot left = new DriftSnapshot(leftRequest).addChangeSet(rightChangeSet0).addChangeSet(leftChangeSet0);

        DriftDiffReport diff = left.diff(right);

        assertCollectionMatchesNoOrder(asList(entry2), diff.getElementsNotInRight(), "Diff report does not contain "
            + "elements that are in the left but not in the right.");
    }

    @Test
    public void diffShowsEntriesInRightAndNotInLeft() {
        int defId = 1;
        DriftSnapshotRequest rightRequest = new DriftSnapshotRequest(defId);
        DriftSnapshotRequest leftRequest = new DriftSnapshotRequest(defId);

        Drift entry1 = new FakeDrift(FILE_ADDED, null, new FakeDriftFile("a1b2c3", 1024, LOADED), "/drift/1.txt");
        FakeDriftChangeSet rightChangeSet0 = new FakeDriftChangeSet(0, COVERAGE, defId).add(entry1);
        DriftSnapshot right = new DriftSnapshot(rightRequest).addChangeSet(rightChangeSet0);

        Drift entry2 = new FakeDrift(FILE_ADDED, null, new FakeDriftFile("a3b6c9", 1024, LOADED), "/drift/2.txt");
        FakeDriftChangeSet leftChangeSet0 = new FakeDriftChangeSet(0, COVERAGE, defId).add(entry2);
        DriftSnapshot left = new DriftSnapshot(leftRequest).addChangeSet(leftChangeSet0);

        DriftDiffReport diff = left.diff(right);

        assertCollectionMatchesNoOrder(asList(entry1), diff.getElementsNotInLeft(), "Diff report does not contain "
            + "elements that are in the left but not in the right");
    }

    @Test
    public void diffShowsEntriesInLeftAndRightThatAreInConflict() {
        int defId = 1;
        DriftSnapshotRequest rightRequest = new DriftSnapshotRequest(defId);
        DriftSnapshotRequest leftRequest = new DriftSnapshotRequest(defId);

        Drift entry1 = new FakeDrift(FILE_ADDED, null, new FakeDriftFile("a1b2c3", 1024, LOADED), "/drfit/1.txt");
        FakeDriftChangeSet changeSet0 = new FakeDriftChangeSet(0, COVERAGE, defId).add(entry1);
        DriftSnapshot right = new DriftSnapshot(rightRequest).addChangeSet(changeSet0);

        Drift entry2 = new FakeDrift(FILE_ADDED, null, new FakeDriftFile("c3b2a1", 1024, LOADED), "/drfit/1.txt");
        FakeDriftChangeSet changeSet1 = new FakeDriftChangeSet(1, DRIFT, defId).add(entry2);
        DriftSnapshot left = new DriftSnapshot(leftRequest).addChangeSet(changeSet1);

        DriftDiffReport diff = left.diff(right);

        assertCollectionMatchesNoOrder(asList(entry2), diff.getElementsInConflict(), "Diff report does not contain "
            + "element that are in both left and right and are in conflict");
    }

    <E> Set<E> asSet(E... elements) {
        HashSet<E> set = new HashSet<E>();
        for (E element : elements) {
            set.add(element);
        }
        return set;
    }

    static class FakeDriftChangeSet implements DriftChangeSet {

        private String id;
        private int version;
        private DriftChangeSetCategory category;
        private int configId;
        private DriftHandlingMode mode = DriftHandlingMode.normal;
        private Set<Drift> drifts = new HashSet<Drift>();
        private long ctime = System.currentTimeMillis();

        public FakeDriftChangeSet() {
        }

        public FakeDriftChangeSet(int version, DriftChangeSetCategory category, int configId) {
            this.version = version;
            this.category = category;
            this.configId = configId;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public void setId(String id) {
            this.id = id;
        }

        @Override
        public Long getCtime() {
            return ctime;
        }

        @Override
        public int getVersion() {
            return version;
        }

        @Override
        public void setVersion(int version) {
            this.version = version;
        }

        @Override
        public DriftChangeSetCategory getCategory() {
            return category;
        }

        @Override
        public void setCategory(DriftChangeSetCategory category) {
            this.category = category;
        }

        @Override
        public int getDriftDefinitionId() {
            return configId;
        }

        @Override
        public void setDriftDefinitionId(int id) {
            configId = id;
        }

        @Override
        public int getResourceId() {
            return 0;
        }

        @Override
        public void setResourceId(int id) {
        }

        @Override
        public Set getDrifts() {
            return drifts;
        }

        @Override
        public void setDrifts(Set drifts) {
            this.drifts = drifts;
        }

        public FakeDriftChangeSet add(Drift drift) {
            drifts.add(drift);
            return this;
        }

        @Override
        public DriftHandlingMode getDriftHandlingMode() {
            return this.mode;
        }

        @Override
        public void setDriftHandlingMode(DriftHandlingMode mode) {
            this.mode = mode;
        }
    }

    static class FakeDrift implements Drift {

        private String id;
        private DriftCategory category;
        private String path;
        private String directory;
        private DriftFile oldFile;
        private DriftFile newFile;
        private long ctime = System.currentTimeMillis();

        public FakeDrift() {
        }

        public FakeDrift(DriftCategory category, DriftFile oldFile, DriftFile newFile, String path) {
            this.category = category;
            this.oldFile = oldFile;
            this.newFile = newFile;
            this.path = path;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public void setId(String id) {
            this.id = id;
        }

        @Override
        public Long getCtime() {
            return ctime;
        }

        @Override
        public DriftChangeSet getChangeSet() {
            return null;
        }

        @Override
        public void setChangeSet(DriftChangeSet changeSet) {

        }

        @Override
        public DriftCategory getCategory() {
            return category;
        }

        @Override
        public void setCategory(DriftCategory category) {
            this.category = category;
        }

        @Override
        public String getPath() {
            return path;
        }

        @Override
        public void setPath(String path) {
            this.path = path;
        }

        @Override
        public String getDirectory() {
            return this.directory;
        }

        @Override
        public void setDirectory(String directory) {
            this.directory = directory;
        }

        @Override
        public DriftFile getOldDriftFile() {
            return oldFile;
        }

        @Override
        public void setOldDriftFile(DriftFile oldDriftFile) {
            this.oldFile = oldDriftFile;
        }

        @Override
        public DriftFile getNewDriftFile() {
            return newFile;
        }

        @Override
        public void setNewDriftFile(DriftFile newDriftFile) {
            newFile = newDriftFile;
        }

        @Override
        public String toString() {
            return "Drift[category: " + category.code() + ", oldFile: " + oldFile + ", newFile: " + newFile
                + ", path: " + path + "]";
        }
    }

    static class FakeDriftFile implements Serializable, DriftFile {

        private static final long serialVersionUID = 1L;
        private String hash;
        private long size;
        private DriftFileStatus status;
        private long ctime = System.currentTimeMillis();

        public FakeDriftFile() {
        }

        public FakeDriftFile(String hash, long size, DriftFileStatus status) {
            this.hash = hash;
            this.size = size;
            this.status = status;
        }

        @Override
        public String getHashId() {
            return hash;
        }

        @Override
        public void setHashId(String hashId) {
            hash = hashId;
        }

        @Override
        public Long getCtime() {
            return ctime;
        }

        @Override
        public Long getDataSize() {
            return size;
        }

        @Override
        public void setDataSize(Long size) {
            this.size = size;
        }

        @Override
        public DriftFileStatus getStatus() {
            return status;
        }

        @Override
        public void setStatus(DriftFileStatus status) {
            this.status = status;
        }

        @Override
        public String toString() {
            return "DriftFile[hashId: " + hash + ", dataSize: " + size + ", status: " + status.ordinal() + "]";
        }
    }

}
