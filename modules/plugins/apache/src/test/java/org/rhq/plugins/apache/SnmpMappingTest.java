/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package org.rhq.plugins.apache;

import static org.testng.Assert.assertEquals;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import org.rhq.core.pluginapi.util.FileUtils;
import org.rhq.core.util.file.FileUtil;
import org.rhq.core.util.stream.StreamUtil;
import org.rhq.plugins.apache.parser.ApacheConfigReader;
import org.rhq.plugins.apache.parser.ApacheDirective;
import org.rhq.plugins.apache.parser.ApacheDirectiveTree;
import org.rhq.plugins.apache.parser.ApacheParser;
import org.rhq.plugins.apache.parser.ApacheParserImpl;
import org.rhq.plugins.apache.util.HttpdAddressUtility;
import org.rhq.plugins.apache.util.MockApacheBinaryInfo;
import org.rhq.plugins.apache.util.MockProcessInfo;

/**
 * 
 *
 * @author Lukas Krejci
 */
@Test
public class SnmpMappingTest {

    private File tmpDir;
    
    private static final String[] VHOST_NAMES_CONFIGURATION_TEST_FILES = {
        "snmp-mapping/httpd.conf",
        "snmp-mapping/vhost-with-servername-by-ip.conf",
        "snmp-mapping/vhost-with-servername-by-unresolvable-hostname.conf",
        "snmp-mapping/vhost-without-servername-resolvable-ip.conf",
        "snmp-mapping/vhost-without-servername-unresolvable-hostname.conf",
        "snmp-mapping/vhost-without-servername-unresolvable-ip.conf"
    };
    
    private static final String[] EXPECTED_SNMP_NAMES = {
        "the-main-server-name:42", //httpd.conf
        "12.34.56.78:0", //vhost-with-servername-by-ip.conf
        "this-will-never-resolve.weird-server.net:90", //vhost-with-servername-by-unresolvable-hostname.conf
        "<<<LOCALHOST>>>:1002", //vhost-without-servernama-resolvable-ip.conf
        "bogus_host_without_forward_dns:42", //vhost-without-servername-unresolvable-hostname.conf
        "bogus_host_without_reverse_dns:1003" //vhost-without-servername-unresolvable-ip.conf
    };

    @BeforeClass
    public void copyConfigurationFiles() throws Exception {
        tmpDir = FileUtil.createTempDirectory("apache-runtime-config-tests", null, null);
        
        for(String path : VHOST_NAMES_CONFIGURATION_TEST_FILES) {
            copyResourceToFile(path, new File(tmpDir, path));
        }
    }
    
    @BeforeClass
    public void initExpectedResults() throws Exception {
        String localhost = InetAddress.getByName("127.0.0.1").getHostName();
        for(int i = 0; i < EXPECTED_SNMP_NAMES.length; ++i) {
            EXPECTED_SNMP_NAMES[i] = EXPECTED_SNMP_NAMES[i].replaceAll("<<<LOCALHOST>>>", localhost);
        }
    }
    
    @AfterClass
    public void deleteConfigurationFiles() throws IOException {
        FileUtils.purge(tmpDir, true);
    }
    

    public void testVhostNames() {
        MockApacheBinaryInfo binfo = new MockApacheBinaryInfo();
        binfo.setVersion("2.2.17");
        MockProcessInfo pinfo = new MockProcessInfo();
        pinfo.setCommandLine(new String[] {"blahblah"});
        
        ApacheDirectiveTree tree = new ApacheDirectiveTree();
        ApacheParser parser = new ApacheParserImpl(tree, new File(tmpDir, "snmp-mapping").getAbsolutePath());
        ApacheConfigReader.buildTree(new File(tmpDir, "snmp-mapping/httpd.conf").getAbsolutePath(), parser);
        
        HttpdAddressUtility addrUtil = HttpdAddressUtility.get("2.2.17");
        List<ApacheDirective> vhosts = tree.search("/<VirtualHost");
        List<String> snmpNames = new ArrayList<String>(vhosts.size() + 1);
        snmpNames.add(addrUtil.getHttpdInternalMainServerAddressRepresentation(tree).toString(false, false));
        for(ApacheDirective vhost : vhosts) {
            String vhostDef = vhost.getValues().get(0);
            String serverName = null;
            List<ApacheDirective> serverNames = vhost.getChildByName("ServerName");
            if (serverNames.size() > 0) {
                serverName = serverNames.get(serverNames.size() - 1).getValuesAsString();
            }
            
            snmpNames.add(addrUtil.getHttpdInternalVirtualHostAddressRepresentation(tree, vhostDef, serverName).toString(false, false));
        }
        
        assertEquals(snmpNames, Arrays.asList(EXPECTED_SNMP_NAMES));
    }
    

    private void copyResourceToFile(String resourcePath, File destination) throws IOException {        
        InputStream input = getClass().getClassLoader().getResourceAsStream(resourcePath);
        
        if (input != null) {
            destination.getParentFile().mkdirs();
            destination.createNewFile();
            
            StreamUtil.copy(input, new BufferedOutputStream(new FileOutputStream(destination)), true);
        }
    }    
}
