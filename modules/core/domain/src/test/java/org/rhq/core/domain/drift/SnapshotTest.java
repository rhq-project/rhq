package org.rhq.core.domain.drift;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.sql.Blob;
import java.util.ArrayList;
import java.util.List;

import javax.transaction.SystemException;

import org.apache.commons.io.IOUtils;
import org.hibernate.Hibernate;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeGroups;
import org.testng.annotations.Test;

import org.rhq.core.domain.test.AbstractEJB3Test;

import static org.apache.commons.io.IOUtils.toInputStream;

public class SnapshotTest extends AbstractEJB3Test {

    static interface TransactionCallback {
        void execute() throws Exception;
    }

    @BeforeGroups(groups = {"snapshot"})
    public void resetDB() throws Exception {
        executeInTransaction(new TransactionCallback() {
            @Override
            public void execute() throws Exception {
                getEntityManager().createQuery("delete from Snapshot").executeUpdate();
            }
        });
    }

    @Test(groups = {"integration.ejb3", "snapshot"})
    public void updateSnapshotData() throws Exception {
        String content = "snapshot data";

        // Create the initial snapshot
        final Snapshot s1 = new Snapshot();
        s1.setDataSize(content.length());
        s1.setData(Hibernate.createBlob(toInputStream(content), content.length()));
        s1.setBasedir("snapshot/test/s1");

        executeInTransaction(new TransactionCallback() {
            @Override
            public void execute() {
                getEntityManager().persist(s1);
            }
        });

        // Make the update
        final String newContent = "snapshot data updated...";
        executeInTransaction(new TransactionCallback() {
            @Override
            public void execute() {
                Snapshot s2 = getEntityManager().find(Snapshot.class, s1.getId());
                s2.setData(Hibernate.createBlob(toInputStream(newContent), newContent.length()));
                getEntityManager().merge(s2);
            }
        });

        // Fetch the snapshot to verify that the update was persisted
        executeInTransaction(new TransactionCallback() {
            @Override
            public void execute() {
                try {
                    Snapshot s3 = getEntityManager().find(Snapshot.class, s1.getId());
                    String expected = newContent;
                    String actual = IOUtils.toString(s3.getData());

                    assertEquals("Failed to update snapshot data", expected, actual);
                } catch (Exception e) {
                    fail("Failed to load snapshot data: " + e.getMessage());
                }
            }
        });
    }

    // The purpose of this test is to store a large amount of data and then
    // load the snapshots to verify that the snapshot data is not also loaded.
    // Because of the amount data involved is very large the test is long
    // running and should be moved to an integration test suite.
    @Test(groups = {"integration.ejb3", "snapshot"})
    public void loadMultipleSnapshotsWithoutLoadingData() throws Exception {
        File dataFile = createDataFile("test_data.txt", 10);
        int numSnapshots = 10;
        final List<Integer> snapshotIds = new ArrayList<Integer>();

        for (int i = 0; i <numSnapshots; ++i) {
            final Snapshot snapshot = new Snapshot();
            snapshot.setDataSize(dataFile.length());
            snapshot.setBasedir("snapshot/" + i);
            snapshot.setData(Hibernate.createBlob(new BufferedInputStream(new FileInputStream(dataFile))));

            executeInTransaction(new TransactionCallback() {
                @Override
                public void execute() {
                    getEntityManager().persist(snapshot);
                    snapshotIds.add(snapshot.getId());
                }
            });
        }

        final List<Blob> blobs = new ArrayList<Blob>();
        final List<Snapshot> snapshots = new ArrayList<Snapshot>();
        for (final Integer id : snapshotIds) {
            executeInTransaction(new TransactionCallback() {
                @Override
                public void execute() {
                    Snapshot snapshot = getEntityManager().find(Snapshot.class, id);
                    blobs.add(snapshot.getBlob());
                    snapshots.add(snapshot);
                }
            });
        }

        assertEquals("Failed to save or load " + numSnapshots + " snapshots", numSnapshots, snapshots.size());
    }

    void executeInTransaction(TransactionCallback callback) {
        try {
            getTransactionManager().begin();
            callback.execute();
            getTransactionManager().commit();
        } catch (Throwable t) {
            try {
                getTransactionManager().rollback();
            } catch (SystemException e) {
                throw new RuntimeException("Failed to rollback transaction", e);
            }
            throw new RuntimeException(t.getCause());
        }
    }

    File snapshotDir() throws URISyntaxException {
        File dir = new File(new File(getClass().getResource(".").toURI()), "snapshots");
        dir.mkdir();
        return dir;
    }

    File workDir() throws URISyntaxException {
        File dir = new File(new File(getClass().getResource(".").toURI()), "work");
        dir.mkdir();
        return dir;
    }

    /**
     * Creates a file in {@link #workDir()} that is filled with arbitrary data up to <code>size</code> in megabytes.
     *
     * @param name The name of the file to create
     * @param size The size of the file in megabytes
     * @return The generated file
     */
    File createDataFile(String name, int size) throws Exception  {
        File file = new File(workDir(), name);

        long oneMB = 1048576;
        long sizeInBytes = size * oneMB;
        int lineSize = 80;  // size in bytes
        long numLines = sizeInBytes / lineSize;

        PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(file)));

        for (int i = 0; i < numLines; ++i) {
            StringBuilder line = new StringBuilder();
            for (int j = 1; j < lineSize; ++j) {
                line.append('x');
            }
            writer.println(line.toString());
        }
        writer.close();
        return file;
    }

}
