package org.rhq.core.domain.drift;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

import javax.print.URIException;

import org.apache.commons.io.IOUtils;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.testng.annotations.Test;

import org.rhq.core.domain.test.AbstractEJB3Test;

import static org.apache.commons.io.IOUtils.toInputStream;

public class SnapshotTest extends AbstractEJB3Test {

    @Test(groups = "integration.ejb3")
    public void loadSnapshotWithoutData() throws Exception {
        String content = "This is a test";

        getTransactionManager().begin();
        Snapshot snapshot = new Snapshot();
        snapshot.setDataSize(content.length());
        snapshot.setData(Hibernate.createBlob(toInputStream(content), content.length()));

        getEntityManager().persist(snapshot);
        getTransactionManager().commit();

        getTransactionManager().begin();
        snapshot = getEntityManager().find(Snapshot.class, snapshot.getId());
        getTransactionManager().commit();

        System.out.println("data size = " + snapshot.getDataSize());
    }

    @Test(groups = "integration.ejb3")
    public void loadMultipleSnapshotsWithoutLoadingData() throws Exception {
        File dataFile = createDataFile("test_data.txt", 50);
        int numSnapshots = 10;
        List<Integer> snapshotIds = new ArrayList<Integer>();

        getTransactionManager().begin();
        for (int i = 0; i < numSnapshots; ++i) {
            Snapshot snapshot = new Snapshot();
            snapshot.setDataSize(dataFile.length());
            snapshot.setData(Hibernate.createBlob(new BufferedInputStream(new FileInputStream(dataFile))));

            getEntityManager().persist(snapshot);
            snapshotIds.add(snapshot.getId());
        }
        getTransactionManager().commit();

        getTransactionManager().begin();
        List<Snapshot> snapshots = new ArrayList<Snapshot>();
        for (Integer id : snapshotIds) {
            snapshots.add(getEntityManager().find(Snapshot.class, id));
        }
        getTransactionManager().commit();
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
