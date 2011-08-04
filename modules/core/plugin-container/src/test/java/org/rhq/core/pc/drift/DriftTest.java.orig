package org.rhq.core.pc.drift;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.poi.util.IOUtils;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;

import org.rhq.core.domain.drift.DriftConfiguration;
import org.rhq.core.util.MessageDigestGenerator;

import static org.apache.commons.io.FileUtils.deleteDirectory;

public class DriftTest {

    protected File changeSetsDir;

    protected File resourcesDir;

    protected ChangeSetManager changeSetMgr;

    private int resourceId;

    protected File resourceDir;

    private MessageDigestGenerator digestGenerator;

    @BeforeClass
    public void initResourcesAndChangeSetsDirs() throws Exception {
        File basedir = new File("target", getClass().getSimpleName());
        deleteDirectory(basedir);
        basedir.mkdir();

        changeSetsDir = mkdir(basedir, "changesets");
        resourcesDir = mkdir(basedir, "resources");

        digestGenerator = new MessageDigestGenerator(MessageDigestGenerator.SHA_256);
    }

    @BeforeMethod
    public void setUp() {
        resourceDir = mkdir(resourcesDir, "resource-" + nextResourceId());
        changeSetMgr = new ChangeSetManagerImpl(changeSetsDir);
    }

    protected int resourceId() {
        return resourceId;
    }

    protected int nextResourceId() {
        return ++resourceId;
    }

    protected File mkdir(File parent, String name) {
        File dir = new File(parent, name);
        dir.mkdirs();
        return dir;
    }

    protected File changeSetDir(String driftConfigName) {
        File dir = new File(new File(changeSetsDir, Integer.toString(resourceId)), driftConfigName);
        dir.mkdirs();
        return dir;
    }

    protected String sha256(File file) throws IOException {
        return digestGenerator.calcDigestString(file);
    }
}
