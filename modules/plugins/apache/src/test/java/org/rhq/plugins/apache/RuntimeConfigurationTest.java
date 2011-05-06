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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.testng.Assert.*;
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
import org.rhq.plugins.apache.util.MockApacheBinaryInfo;
import org.rhq.plugins.apache.util.MockProcessInfo;
import org.rhq.plugins.apache.util.RuntimeApacheConfiguration;

/**
 * 
 *
 * @author Lukas Krejci
 */
@Test
public class RuntimeConfigurationTest {

    private File tmpDir;
    
    private static final String[] CONDITIONAL_CONFIGURATION_TEST_FILES = {
        "runtime-config/conditional/httpd.conf",
        "runtime-config/conditional/ifdefine-defined.conf",
        "runtime-config/conditional/ifdefine-undefined.conf",
        "runtime-config/conditional/ifmodule-loaded.conf",
        "runtime-config/conditional/ifmodule-not-loaded.conf",
        "runtime-config/conditional/ifversion.conf",
        "runtime-config/conditional/nested-mess.conf"        
    };
    
    private static final String[] INCLUSION_ORDER_CONFIGURATION_TEST_FILES = {
        "runtime-config/incl-order/a.conf",
        "runtime-config/incl-order/b.conf",
        "runtime-config/incl-order/c.conf",
        "runtime-config/incl-order/httpd.conf"
    };
    
    @BeforeClass
    public void copyConfigurationFiles() throws Exception {
        tmpDir = FileUtil.createTempDirectory("apache-runtime-config-tests", null, null);
        
        for(String path : CONDITIONAL_CONFIGURATION_TEST_FILES) {            
            copyResourceToFile(path, new File(tmpDir, path));
        }
        
        for(String path : INCLUSION_ORDER_CONFIGURATION_TEST_FILES) {
            copyResourceToFile(path, new File(tmpDir, path));
        }
    }
    
    @AfterClass
    public void deleteConfigurationFiles() throws IOException {
        FileUtils.purge(tmpDir, true);
    }
    
    public void testConditionalInclusion() {
        MockApacheBinaryInfo binfo = new MockApacheBinaryInfo();
        binfo.setVersion("2.2.17");
        MockProcessInfo pinfo = new MockProcessInfo();
        pinfo.setCommandLine(new String[] {"blahblah", "-D", "DEFINED"});
        
        ApacheDirectiveTree tree = new ApacheDirectiveTree();
        ApacheParser parser = new ApacheParserImpl(tree, new File(tmpDir, "runtime-config/conditional").getAbsolutePath());
        ApacheConfigReader.buildTree(new File(tmpDir, "runtime-config/conditional/httpd.conf").getAbsolutePath(), parser);
        
        tree = RuntimeApacheConfiguration.extract(tree, pinfo, binfo, ApacheServerDiscoveryComponent.MODULE_SOURCE_FILE_TO_MODULE_NAME_20);
        
        List<VhostSpec> vhosts = VhostSpec.detect(tree);
        
        List<VhostSpec> expectedVhosts = new ArrayList<VhostSpec>();
                
        expectedVhosts.add(new VhostSpec(Collections.singleton("127.0.0.1:100"), "ifdefine.defined"));
        expectedVhosts.add(new VhostSpec(Collections.singleton("127.0.0.1:300"), "ifmodule.loaded.source-file"));
        expectedVhosts.add(new VhostSpec(Collections.singleton("127.0.0.1:301"), "ifmodule.loaded.module-name"));
        expectedVhosts.add(new VhostSpec(Collections.singleton("127.0.0.1:501"), "ifversion.module-loaded.implied-equals"));
        expectedVhosts.add(new VhostSpec(Collections.singleton("127.0.0.1:502"), "ifversion.module-loaded.equals"));
        expectedVhosts.add(new VhostSpec(Collections.singleton("127.0.0.1:503"), "ifversion.module-loaded.not-equals"));
        expectedVhosts.add(new VhostSpec(Collections.singleton("127.0.0.1:504"), "ifversion.module-loaded.regex"));
        expectedVhosts.add(new VhostSpec(Collections.singleton("127.0.0.1:505"), "ifversion.module-loaded.implied-regex"));
        expectedVhosts.add(new VhostSpec(Collections.singleton("127.0.0.1:602"), "ifdefine.ifmodule.loaded.source-file"));
        expectedVhosts.add(new VhostSpec(Collections.singleton("127.0.0.1:603"), "ifdefine.ifmodule.loaded.module-name"));      
        
        assertEquals(vhosts, expectedVhosts);
    }
    
    public void testInclusionOrder() {
        MockApacheBinaryInfo binfo = new MockApacheBinaryInfo();
        binfo.setVersion("2.2.17");
        MockProcessInfo pinfo = new MockProcessInfo();
        pinfo.setCommandLine(new String[] {"blahblah"});
        
        ApacheDirectiveTree tree = new ApacheDirectiveTree();
        ApacheParser parser = new ApacheParserImpl(tree, new File(tmpDir, "runtime-config/incl-order").getAbsolutePath());
        ApacheConfigReader.buildTree(new File(tmpDir, "runtime-config/incl-order/httpd.conf").getAbsolutePath(), parser);
        
        tree = RuntimeApacheConfiguration.extract(tree, pinfo, binfo, ApacheServerDiscoveryComponent.MODULE_SOURCE_FILE_TO_MODULE_NAME_20);
                
        List<ApacheDirective> listens = tree.search("/Listen");
        
        assertEquals(listens.size(), 3, "There should be 3 listen directives");
        
        assertEquals(listens.get(0).getValuesAsString(), "80");
        assertEquals(listens.get(1).getValuesAsString(), "81");
        assertEquals(listens.get(2).getValuesAsString(), "82");
    }
    
    private void copyResourceToFile(String resourcePath, File destination) throws IOException {        
        InputStream input = getClass().getClassLoader().getResourceAsStream(resourcePath);
        
        if (input != null) {
            destination.getParentFile().mkdirs();
            destination.createNewFile();
            
            StreamUtil.copy(input, new BufferedOutputStream(new FileOutputStream(destination)), true);
        }
    }
    
    private static class VhostSpec {
        public List<String> definition;
        public String serverName;
        
        public static List<VhostSpec> detect(ApacheDirectiveTree tree) {
            List<VhostSpec> ret = new ArrayList<VhostSpec>();
            
            for(ApacheDirective vhost : tree.search("/<VirtualHost")) {
                ret.add(new VhostSpec(vhost));
            }
                        
            return ret;
        }
        
        public VhostSpec(ApacheDirective vhost) {
            definition = vhost.getValues();
            List<ApacheDirective> serverNames = vhost.getChildByName("ServerName");
            if (serverNames.size() > 0) {
                serverName = serverNames.get(0).getValuesAsString();
            }
        }
        
        public VhostSpec(Collection<String> definition, String serverName) {
            this.definition = new ArrayList<String>(definition);
            this.serverName = serverName;
        }
        
        @Override
        public String toString() {
            return "VhostSpec[serverName='" + serverName + "', definition=" + definition + "]";
        }
        
        @Override
        public int hashCode() {
            return serverName.hashCode();
        }
        
        @Override
        public boolean equals(Object other) {
            if (other == this) {
                return true;
            }
            
            if (!(other instanceof VhostSpec)) {
                return false;                
            }
            
            VhostSpec o = (VhostSpec) other;
            
            return serverName.equals(o.serverName) && definition.equals(o.definition);
        }
    }
}
