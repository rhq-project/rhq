package org.rhq.enterprise.server.plugins.rhnhosted;

import junit.framework.TestCase;

import java.util.List;
import java.util.ArrayList;
import java.io.IOException;
import java.io.File;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.io.FileUtils;
import org.rhq.core.clientapi.server.plugin.content.ContentProviderPackageDetails;

/**
 * RHNHelper Tester.
 *
 * @author pkilambi
 */
public class RHNHelperTest extends TestCase {

    private static final String TEST_SERVER_URL = "http://satellite.rhn.redhat.com/";
    private static final String REPO_LABEL = "rhel-i386-server-vt-5";

    private RHNHelper helper;

    public RHNHelperTest(String name) {
        super(name);
    }

    public void setUp() throws Exception {
        super.setUp();
        helper = new RHNHelper(TEST_SERVER_URL, REPO_LABEL);
    }

    public void testGetPackageDetails() throws Exception {
        boolean success;

        try {
            List pids = helper.getChannelPackages();
            List<ContentProviderPackageDetails> pkgdetails = helper.getPackageDetails(pids);
            for (ContentProviderPackageDetails pkg: pkgdetails) {
                assertFalse(StringUtils.isBlank(pkg.getDisplayName()));
                assertFalse(StringUtils.isBlank(pkg.getArchitectureName()));
                assertFalse(StringUtils.isBlank(pkg.getPackageTypeName()));
                assertFalse(StringUtils.isBlank(pkg.getName()));
                assertFalse(StringUtils.isBlank(pkg.getFileName()));
                assertFalse(StringUtils.isBlank(pkg.getLongDescription()));
                assertFalse(StringUtils.isBlank(pkg.getMD5()));

            }
            success = true;
        } catch (Exception e) {
            e.printStackTrace();
            success = false;
        }

        assertTrue(success);
    }

    public void testGetChannelPackages() throws Exception {
        boolean success = true;
        try {
            ArrayList pkgIds = helper.getChannelPackages();
            assertTrue(pkgIds.size() > 0);
        } catch (Exception e) {
            e.printStackTrace();
            success = false;
        }

        assertTrue(success);
    }


    protected String readSystemId() throws Exception {
        try {
            return FileUtils.readFileToString(new File(RHNConstants.DEFAULT_SYSTEM_ID));
        } catch (IOException e) {
            return "";
        }
    }
}
