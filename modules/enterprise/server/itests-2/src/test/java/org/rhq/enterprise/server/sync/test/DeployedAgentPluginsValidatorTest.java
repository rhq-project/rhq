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

package org.rhq.enterprise.server.sync.test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.fail;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jmock.Expectations;
import org.testng.annotations.Test;

import org.rhq.core.domain.plugin.Plugin;
import org.rhq.core.domain.plugin.PluginStatusType;
import org.rhq.enterprise.server.resource.metadata.PluginManagerLocal;
import org.rhq.enterprise.server.sync.ExportReader;
import org.rhq.enterprise.server.sync.ExportWriter;
import org.rhq.enterprise.server.sync.validators.DeployedAgentPluginsValidator;
import org.rhq.enterprise.server.sync.validators.InconsistentStateException;
import org.rhq.test.JMockTest;

/**
 * 
 *
 * @author Lukas Krejci
 */
//@Test - switching this off for now because this validator is no longer used in any
//of the default synchronizers
public class DeployedAgentPluginsValidatorTest extends JMockTest {

    private static final Log LOG = LogFactory.getLog(DeployedAgentPluginsValidator.class);
    
    public void testCanExportAndImportState() throws Exception {
        final PluginManagerLocal pluginManager = context.mock(PluginManagerLocal.class);
        
        final DeployedAgentPluginsValidator validator = new DeployedAgentPluginsValidator(pluginManager);
        
        context.checking(new Expectations() {
            {
                oneOf(pluginManager).getInstalledPlugins();
                will(returnValue(new ArrayList<Plugin>(getDeployedPlugins())));
            }
        });
        
        validator.initialize(null, null);
        
        StringWriter output = new StringWriter();
        try {
            XMLOutputFactory ofactory = XMLOutputFactory.newInstance();
            
            XMLStreamWriter wrt = ofactory.createXMLStreamWriter(output);
            //wrap the exported plugins in "something" so that we produce
            //a valid xml
            wrt.writeStartDocument();
            wrt.writeStartElement("root");
            
            validator.exportState(new ExportWriter(wrt));
        
            wrt.writeEndDocument();
            
            wrt.close();
            
            StringReader input = new StringReader(output.toString());
            
            try {
                XMLInputFactory ifactory = XMLInputFactory.newInstance();
                XMLStreamReader rdr = ifactory.createXMLStreamReader(input);
                
                //push the reader to the start of the plugin elements
                //this is what is expected by the validators
                rdr.nextTag();
                
                validator.initializeExportedStateValidation(new ExportReader(rdr));
                
                rdr.close();
                
                assertEquals(validator.getPluginsToValidate(), getDeployedPlugins());
            } finally {
                input.close();
            }
        } catch (Exception e) {
            LOG.error("Test failed. Output generated so far:\n" + output, e);
            throw e;
        } finally {
            output.close();
        }
    }
    
    public void testDetectsPluginDifferencies() {
        final PluginManagerLocal pluginManager = context.mock(PluginManagerLocal.class);
        
        //this is what we are going to return from the mocked plugin manager
        final List<Plugin> installedPlugins = new ArrayList<Plugin>(getDeployedPlugins());
        
        context.checking(new Expectations() {
            {
                allowing(pluginManager).getInstalledPlugins();
                will(returnValue(installedPlugins));
            }
        });
        
        DeployedAgentPluginsValidator validator = new DeployedAgentPluginsValidator(pluginManager);
        
        Set<DeployedAgentPluginsValidator.ConsistentPlugin> pluginsToValidate = getDeployedPlugins();
        
        validator.initialize(null, null);
        
        validator.setPluginsToValidate(pluginsToValidate);
        
        //this should validate cleanly
        validator.validateExportedState();
        
        //now add 1 plugin to the validated state
        DeployedAgentPluginsValidator.ConsistentPlugin newPlugin = createPlugin("superfluous", "1", "md5_4");
        pluginsToValidate.add(newPlugin);
        
        try {
            validator.validateExportedState();
            fail("validation should have detected that the current installation has one plugin less.");
        } catch (InconsistentStateException e) {
            //this is expected
        }
        
        //ok, remove the new plugin from the "exported state" and put it in the current state
        pluginsToValidate.remove(newPlugin);
        installedPlugins.add(newPlugin);
        
        try {
            validator.validateExportedState();
            fail("validation should have detected that the current installation has one plugin more.");
        } catch (InconsistentStateException e) {
            //ok
        }
        
        //make the current state and exported state be in sync again
        DeployedAgentPluginsValidator.ConsistentPlugin newPluginCopy = new DeployedAgentPluginsValidator.ConsistentPlugin(newPlugin);
        pluginsToValidate.add(newPluginCopy);
        
        //change the newPlugin to not be equal
        newPlugin.setMd5("md5_different");
        
        try {
            validator.validateExportedState();
            fail("validation should have failed because one of the plugins differs between current and exported states.");
        } catch (InconsistentStateException e) {
            //ok
        }
        
        newPlugin.setMd5(newPluginCopy.getMd5());
        newPluginCopy.setMd5("md5_different");
        
        try {
            validator.validateExportedState();
            fail("validation should have failed because one of the plugins differs between current and exported states.");
        } catch (InconsistentStateException e) {
            //ok
        }        
    }
    
    public void testAllValidatorsEqual() {
        PluginManagerLocal pluginManager = context.mock(PluginManagerLocal.class);
        
        DeployedAgentPluginsValidator v1 = new DeployedAgentPluginsValidator(pluginManager);
        DeployedAgentPluginsValidator v2 = new DeployedAgentPluginsValidator(pluginManager);
        DeployedAgentPluginsValidator v3 = new DeployedAgentPluginsValidator(pluginManager);
        Object o = new Object();
        
        assertEquals(v1, v2);
        assertEquals(v1, v3);
        assertEquals(v2, v3);
        
        assertFalse(v1.equals(o));
        assertFalse(v2.equals(o));
        assertFalse(v3.equals(o));
    }
    
    private static Set<DeployedAgentPluginsValidator.ConsistentPlugin> getDeployedPlugins() {
        HashSet<DeployedAgentPluginsValidator.ConsistentPlugin> ret = new HashSet<DeployedAgentPluginsValidator.ConsistentPlugin>();
        
        ret.add(createPlugin("p1", "1", "md5_1"));
        ret.add(createPlugin("p2", "1", "md5_2"));
        ret.add(createPlugin("p3", "1", "md5_3"));
        
        return ret;
    }
    
    private static DeployedAgentPluginsValidator.ConsistentPlugin createPlugin(String name, String version, String md5) {
        return new DeployedAgentPluginsValidator.ConsistentPlugin(new Plugin(0, name, null, null, true, PluginStatusType.INSTALLED, null, null, md5, version, null, 0, 0));
    }
}
