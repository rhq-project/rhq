package org.rhq.core.pc.drift;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.unitils.thirdparty.org.apache.commons.io.FileUtils;

import org.rhq.common.drift.Headers;
import org.rhq.core.domain.drift.DriftChangeSetCategory;
import org.rhq.core.domain.drift.DriftFile;

import static java.util.Arrays.asList;
import static org.apache.commons.io.FileUtils.deleteDirectory;
import static org.apache.commons.io.FileUtils.touch;
import static org.apache.commons.io.IOUtils.writeLines;
import static org.rhq.core.domain.drift.DriftChangeSetCategory.COVERAGE;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class DriftFilesSenderTest {

    File changeSetsDir;

    File resourcesDir;

    ChangeSetManager changeSetMgr;

    String driftConfigName = "test";

    File server1Dir;

    @BeforeClass
    public void init() throws Exception {
        File basedir = new File("target", getClass().getSimpleName());
        deleteDirectory(basedir);
        basedir.mkdir();

        changeSetsDir = new File(basedir, "changesets");
        changeSetsDir.mkdir();

        resourcesDir = new File(basedir, "resources");
        resourcesDir.mkdir();
    }

    @BeforeMethod
    public void setUp() {
        changeSetMgr = new ChangeSetManagerImpl(changeSetsDir);
    }

    @Test
    public void sendFiles() throws Exception {
        String driftConfigName = "test";

        File server1Dir = new File(resourcesDir,  "server-1");
        server1Dir.mkdir();

        File confDir = new File(server1Dir, "conf");
        confDir.mkdir();

        touch(new File(confDir, "server.conf"));

        int resourceId = 1;

        File changeSetDir = new File(new File(changeSetsDir, Integer.toString(resourceId)), driftConfigName);
        changeSetDir.mkdirs();

        List<String> changeSet = asList(
            driftConfigName,
            server1Dir.getAbsolutePath(),
            COVERAGE.code(),
            "conf 1",
            "2e345df 0 server.conf A",
            ""
        );
        BufferedOutputStream stream = new BufferedOutputStream(new FileOutputStream(new File(changeSetDir,
            "changeset.txt")));
        writeLines(changeSet, "\n", stream);
        stream.close();

        Headers headers = new Headers(driftConfigName, server1Dir.getAbsolutePath(), COVERAGE);
        List<DriftFile> driftFiles = asList(new DriftFile("2e345df"));

        DriftFilesSender sender = new DriftFilesSender();
        sender.setDriftClient(new DriftClientTestStub());
        sender.setChangeSetManager(changeSetMgr);
        sender.setResourceId(resourceId);
        sender.setDriftFiles(driftFiles);
        sender.setHeaders(headers);
        sender.run();

        File contentDir = new File(changeSetDir, "content");
        contentDir.mkdir();

        File content = new File(contentDir, "2e345df");

        assertEquals(contentDir.list().length, 1, "Expected to find one file in " +
            contentDir.getAbsolutePath());
        assertTrue(content.exists(), "Expected to find file named " + content.getName() + " in content directory. " +
            "SHA-256 hashes should be used as file names");
    }

}
