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

import org.apache.commons.io.IOUtils;
import org.hibernate.Hibernate;
import org.testng.annotations.Test;

import org.rhq.core.util.MessageDigestGenerator;
import org.rhq.test.TransactionCallback;

import static org.apache.commons.io.IOUtils.toInputStream;

public class DriftFileTest extends DriftDataAccessTest {

    static private final MessageDigestGenerator digestGen = new MessageDigestGenerator(MessageDigestGenerator.SHA_256);

    // Note, this test is more of a general Blob handling test. A real JPADriftFile never has its content updated.
    // But this is a useful test to just ensure Blob handling is working as expected.
    @Test(groups = {"driftFile", "drift.ejb"})
    public void updateDriftFileData() throws Exception {
        String content = "driftFile data";
        String hashId = digestGen.calcDigestString(content);

        // Create the initial driftFile
        final JPADriftFileBits df1 = new JPADriftFileBits();
        df1.setHashId(hashId);
        df1.setDataSize((long) content.length());
        df1.setData(Hibernate.createBlob(toInputStream(content), content.length()));

        executeInTransaction(new TransactionCallback() {
            @Override
            public void execute() {
                getEntityManager().persist(df1);
            }
        });

        // Make the update
        final String newContent = "driftFile data updated...";
        executeInTransaction(new TransactionCallback() {
            @Override
            public void execute() {
                JPADriftFileBits df2 = getEntityManager().find(JPADriftFileBits.class, df1.getHashId());
                df2.setData(Hibernate.createBlob(toInputStream(newContent), newContent.length()));
                getEntityManager().merge(df2);
            }
        });

        // Fetch the driftFile to verify that the update was persisted
        executeInTransaction(new TransactionCallback() {
            @Override
            public void execute() {
                try {
                    JPADriftFileBits df3 = getEntityManager().find(JPADriftFileBits.class, df1.getHashId());
                    String expected = newContent;
                    String actual = IOUtils.toString(df3.getData());

                    assertEquals("Failed to update driftFile data", expected, actual);
                } catch (Exception e) {
                    fail("Failed to load driftFile data: " + e.getMessage());
                }
            }
        });
    }

    // The purpose of this test is to store a large amount of data and then
    // load the driftFiles to verify that the driftFile data is not also loaded.
    // In other words, to ensure LazyLoad semantics are working for Blobs
    // Because of the amount data involved is very large the test is long
    // running and should be moved to an integration test suite.
    @Test(groups = {"driftFile", "drift.ejb"})
    public void loadMultipleDriftFilesWithoutLoadingData() throws Exception {
        int numDriftFiles = 3;
        final List<String> driftFileHashIds = new ArrayList<String>();

        for (int i = 0; i < numDriftFiles; ++i) {
            File dataFile = createDataFile("test_data.txt", 1, (char) ('a' + i));
            final JPADriftFileBits driftFile = new JPADriftFileBits();
            driftFile.setDataSize(dataFile.length());
            driftFile.setHashId(digestGen.calcDigestString(dataFile));
            driftFile.setData(Hibernate.createBlob(new BufferedInputStream(new FileInputStream(dataFile))));
            dataFile.delete();

            executeInTransaction(new TransactionCallback() {
                @Override
                public void execute() {
                    getEntityManager().persist(driftFile);
                    driftFileHashIds.add(driftFile.getHashId());
                }
            });
        }

        final List<Blob> blobs = new ArrayList<Blob>();
        final List<JPADriftFileBits> driftFiles = new ArrayList<JPADriftFileBits>();
        for (final String hashId : driftFileHashIds) {
            executeInTransaction(new TransactionCallback() {
                @Override
                public void execute() {
                    JPADriftFileBits driftFileBits = getEntityManager().find(JPADriftFileBits.class, hashId);
                    blobs.add(driftFileBits.getBlob());
                    driftFiles.add(driftFileBits);
                }
            });
        }

        assertEquals("Failed to save or load " + numDriftFiles + " driftFiles", numDriftFiles, driftFiles.size());
    }

    // The purpose of this test is to ensure we won't store two drift files for
    // the same content.
    @Test(groups = {"driftFile", "drift.ejb"})
    public void loadSameFile() throws Exception {
        int numDriftFiles = 2;
        final List<String> driftFileHashIds = new ArrayList<String>();

        for (int i = 0; i < numDriftFiles; ++i) {
            File dataFile = createDataFile("test_data.txt", 10, 'X');
            final JPADriftFileBits driftFile = new JPADriftFileBits();
            final int driftFileNum = i;
            driftFile.setDataSize(dataFile.length());
            driftFile.setHashId(digestGen.calcDigestString(dataFile));
            driftFile.setData(Hibernate.createBlob(new BufferedInputStream(new FileInputStream(dataFile))));
            dataFile.delete();

            try {
                executeInTransaction(new TransactionCallback() {
                    @Override
                    public void execute() {
                        getEntityManager().persist(driftFile);
                        driftFileHashIds.add(driftFile.getHashId());
                    }
                });
            } catch (Exception e) {
                // expected for file 2 or higher
                if (driftFileNum == 0) {
                    fail("Should not be able to store JPADriftFile with same hashId more than once.");
                }
            }
        }

        final List<Blob> blobs = new ArrayList<Blob>();
        final List<JPADriftFileBits> driftFiles = new ArrayList<JPADriftFileBits>();
        for (final String hashId : driftFileHashIds) {
            executeInTransaction(new TransactionCallback() {
                @Override
                public void execute() {
                    JPADriftFileBits driftFileBits = getEntityManager().find(JPADriftFileBits.class, hashId);
                    blobs.add(driftFileBits.getBlob());
                    driftFiles.add(driftFileBits);
                }
            });
        }

        assertEquals("Failed to save or load " + numDriftFiles + " driftFiles", numDriftFiles, driftFiles.size());
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
    File createDataFile(String name, int size, char fillChar) throws Exception {
        File file = new File(workDir(), name);

        long oneMB = 1048576;
        long sizeInBytes = size * oneMB;
        int lineSize = 80; // size in bytes
        long numLines = sizeInBytes / lineSize;

        PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(file)));

        for (int i = 0; i < numLines; ++i) {
            StringBuilder line = new StringBuilder();
            for (int j = 1; j < lineSize; ++j) {
                line.append(fillChar);
            }
            writer.println(line.toString());
        }
        writer.close();
        return file;
    }

}
