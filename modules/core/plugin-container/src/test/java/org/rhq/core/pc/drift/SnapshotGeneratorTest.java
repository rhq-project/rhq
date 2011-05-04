package org.rhq.core.pc.drift;

import java.io.File;
import java.util.Arrays;
import java.util.Map;

import com.google.common.collect.ImmutableMap;

import org.apache.commons.io.FileUtils;
import org.testng.annotations.Test;

import org.rhq.core.domain.drift.Snapshot;
import org.rhq.core.util.MessageDigestGenerator;

import static java.util.Arrays.asList;
import static org.testng.Assert.*;
import static org.apache.commons.io.FileUtils.deleteDirectory;
import static org.apache.commons.io.FileUtils.writeLines;

public class SnapshotGeneratorTest {

    MessageDigestGenerator digestGenerator = new MessageDigestGenerator(MessageDigestGenerator.SHA_256);

    @Test
    public void generateSnapshot() throws Exception {
        File basedir = new File("target/." + getClass().getSimpleName());
        deleteDirectory(basedir);
        assertTrue(basedir.mkdirs(), "Failed to create " + basedir.getAbsolutePath());

        File menu = new File(basedir, "menu");
        File drinks = new File(menu, "drinks.txt");
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

        SnapshotGenerator generator = new SnapshotGenerator();
        Snapshot snapshot = generator.generateSnapshot(menu);

        assertNotNull(snapshot);

        Map<String, String> expected = ImmutableMap.of(
            beers.getPath(), sha256(beers),
            appetizers.getPath(), sha256(appetizers));
        assertMetadataEquals("Snapshot meta data is wrong.", snapshot, expected);
    }


    void assertMetadataEquals(String msg, Snapshot snapshot, Map<String, String> expected) {
        assertEquals(snapshot.getMetadata(), expected, msg);
    }

    String sha256(File f) throws Exception {
        return digestGenerator.calcDigestString(f);
    }

}
