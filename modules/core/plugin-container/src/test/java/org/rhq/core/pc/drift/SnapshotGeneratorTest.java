package org.rhq.core.pc.drift;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.StringWriter;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.testng.annotations.Test;

import org.rhq.core.util.MessageDigestGenerator;

import static java.util.Arrays.asList;
import static org.apache.commons.io.IOUtils.copy;
import static org.testng.Assert.*;
import static org.apache.commons.io.FileUtils.deleteDirectory;
import static org.apache.commons.io.FileUtils.writeLines;

public class SnapshotGeneratorTest {

    MessageDigestGenerator digestGenerator = new MessageDigestGenerator(MessageDigestGenerator.SHA_256);

    @Test
    public void generateSnapshot() throws Exception {
        File basedir = new File("target/." + getClass().getSimpleName());
        File snapshotsDir = new File("target", "snapshots");

        deleteDirectory(basedir);
        deleteDirectory(snapshotsDir);

        assertTrue(basedir.mkdirs(), "Failed to create " + basedir.getAbsolutePath());
        assertTrue(snapshotsDir.mkdirs(), "Failed to crete " + snapshotsDir.getAbsolutePath());

        File menu = new File(basedir, "menu");
        File drinks = new File(menu, "drinks");
        drinks.mkdirs();

        File beers = new File(drinks, "beers.txt");

        writeLines(beers, asList(
            "Amstel Light",
            "Corona",
            "Flying Dog Snake Dog IPA",
            "Terrapin Hopsecutioner"
        ));

        File food = new File(menu, "food");
        food.mkdirs();
        File appetizers = new File(food, "appetizers.txt");

        writeLines(appetizers, asList("Cheese Whiz", "Soft Pretzels", "Hummus", "Fruit Loops"));

        int resourceId = 1;
        SnapshotGenerator generator = new SnapshotGenerator();
        generator.setSnapshotDir(snapshotsDir);
        SnapshotHandle snapshotHandle = generator.generateSnapshot(resourceId, menu);

//        SnapshotHandle snapshotHandle = generator.generateSnapshot(resourceId,
//            new File("/home/jsanda/Development/redhat/rhq/modules/core/plugin-container/jboss-eap-5.0"));

        long start = System.currentTimeMillis();
        long end = System.currentTimeMillis();

        System.out.println("TIME: " + (end - start));

        StringWriter metadataWriter = new StringWriter();
        metadataWriter.write("menu/drinks/beers.txt" + " " + sha256(beers) + "\n");
        metadataWriter.write("menu/food/appetizers.txt" + " " + sha256(appetizers) + "\n");

        assertEquals(sha256(snapshotHandle.getMetadataFile()), sha256(metadataWriter.toString()), "SHA-256 for " +
            "snapshot meta data file at " + snapshotHandle.getMetadataFile().getAbsolutePath() + " does not match " +
            "expected value");

        File zipFile = new File(basedir, "menu.zip");
        ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(zipFile)));

        zos.putNextEntry(new ZipEntry("menu/drinks/beers.txt"));
        copy(new FileInputStream(beers), zos);

        zos.putNextEntry(new ZipEntry("menu/food/appetizers.txt"));
        copy(new FileInputStream(appetizers), zos);

        zos.close();
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        copy(new FileInputStream(zipFile), byteStream);

        assertEquals(sha256(zipFile), sha256(snapshotHandle.getDataFile()), "SHA-256 for snapshot data file at " +
            snapshotHandle.getDataFile().getAbsolutePath() + " does not match expected value");
    }


    String sha256(File f) throws Exception {
        return digestGenerator.calcDigestString(f);
    }

    String sha256(String string) throws Exception {
        return digestGenerator.calcDigestString(string);
    }

}
