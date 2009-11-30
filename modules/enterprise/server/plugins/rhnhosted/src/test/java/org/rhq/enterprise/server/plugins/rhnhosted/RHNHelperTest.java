package org.rhq.enterprise.server.plugins.rhnhosted;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import org.rhq.enterprise.server.plugin.pc.content.ContentProviderPackageDetails;
import org.rhq.enterprise.server.plugins.rhnhosted.xmlrpc.ApacheXmlRpcExecutor;
import org.rhq.enterprise.server.plugins.rhnhosted.xmlrpc.MockRhnHttpURLConnection;
import org.rhq.enterprise.server.plugins.rhnhosted.xmlrpc.MockRhnXmlRpcExecutor;
import org.rhq.enterprise.server.plugins.rhnhosted.xmlrpc.RhnHttpURLConnectionFactory;

/**
 * RHNHelper Tester.
 *
 * @author pkilambi
 */
public class RHNHelperTest extends BaseRHNTest {

    private static final String REPO_LABEL = "rhel-i386-server-vt-5";

    private RHNHelper helper;

    public void setUp() throws Exception {
        super.setUp();
        System.setProperty(ApacheXmlRpcExecutor.class.getName(), MockRhnXmlRpcExecutor.class.getName());
        System.setProperty(RhnHttpURLConnectionFactory.RHN_MOCK_HTTP_URL_CONNECTION, MockRhnHttpURLConnection.class
            .getName());
        helper = new RHNHelper(TEST_SERVER_URL, SYSTEM_ID);
    }

    public void testGetPackageDetails() throws Exception {
        boolean success;

        try {
            List pids = helper.getChannelPackages(REPO_LABEL);
            List<ContentProviderPackageDetails> pkgdetails = helper.getPackageDetails(pids, REPO_LABEL);
            for (ContentProviderPackageDetails pkg : pkgdetails) {
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
            List<String> pkgIds = helper.getChannelPackages(REPO_LABEL);
            assertTrue(pkgIds.size() > 0);
        } catch (Exception e) {
            e.printStackTrace();
            success = false;
        }

        assertTrue(success);
    }

    public void testGetSyncableKickstartTrees() throws Exception {
        boolean success = true;
        try {
            List<String> ksLabels = helper.getSyncableKickstartLabels(REPO_LABEL);
            assertTrue(ksLabels.size() > 0);
        } catch (Exception e) {
            e.printStackTrace();
            success = false;
        }
        assertTrue(success);
    }

}
