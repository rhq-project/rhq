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

package org.rhq.enterprise.server.sync.validators;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.rhq.core.domain.plugin.Plugin;
import org.rhq.core.domain.plugin.PluginDeploymentType;
import org.rhq.enterprise.server.resource.metadata.PluginManagerLocal;
import org.rhq.enterprise.server.sync.ExportReader;
import org.rhq.enterprise.server.sync.ExportWriter;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * This implementation checks that the two installations
 * have the exact same plugins deployed.
 *
 * @author Lukas Krejci
 */
public class DeployedAgentPluginsValidator implements ConsistencyValidator {

    private static class ConsistentPlugin extends Plugin {
        
        private static final long serialVersionUID = 1L;

        public ConsistentPlugin() {
            
        }
        
        public ConsistentPlugin(Plugin p) {
            setName(p.getName());
            setVersion(p.getVersion());
            setMd5(p.getMd5());
        }
        
        @Override
        public int hashCode() {
            return getName().hashCode() * getVersion().hashCode() * getMd5().hashCode();
        }
        
        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            
            if (!(other instanceof ConsistentPlugin)) {
                return false;
            }
            
            ConsistentPlugin p = (ConsistentPlugin) other;
            
            return getName().equals(p.getName()) && getVersion().equals(p.getVersion())
                && getMd5().equals(p.getMd5());
        }
    }
    
    private PluginManagerLocal pluginManager;

    private Set<ConsistentPlugin> pluginsToValidate;

    public DeployedAgentPluginsValidator() {
        this(LookupUtil.getPluginManager());        
    }
    
    public DeployedAgentPluginsValidator(PluginManagerLocal pluginManager) {
        this.pluginManager = pluginManager;
    }
    
    @Override
    public void exportState(ExportWriter writer) throws XMLStreamException {
        List<Plugin> plugins = pluginManager.getInstalledPlugins();

        for (Plugin plugin : plugins) {
            if (plugin.getDeployment() == PluginDeploymentType.AGENT) {
                writer.writeStartElement("plugin");
                writer.writeAttribute("name", plugin.getName());
                writer.writeAttribute("hash", plugin.getMd5());
                writer.writeAttribute("version", plugin.getVersion());
                writer.writeEndElement();
            }
        }
    }

    @Override
    public void initializeValidation(ExportReader reader) throws XMLStreamException {
        pluginsToValidate = new HashSet<ConsistentPlugin>();

        while (reader.hasNext()) {
            switch (reader.next()) {
            case XMLStreamReader.START_ELEMENT:
                if ("plugin".equals(reader.getName().getLocalPart())) {
                    ConsistentPlugin p = new ConsistentPlugin();
                    p.setName(reader.getAttributeValue(null, "name"));
                    p.setMd5(reader.getAttributeValue(null, "hash"));
                    p.setVersion(reader.getAttributeValue(null, "version"));
                    pluginsToValidate.add(p);
                } else {
                    throw new XMLStreamException(
                        "Illegal tag encountered in the DeployedAgentPluginsValidator section: " + reader.getName()
                            + " on location: " + reader.getLocation());
                }
            }
        }
    }

    @Override
    public void validate() throws InconsistentStateException {
        List<Plugin> localPlugins = pluginManager.getInstalledPlugins();
        Set<ConsistentPlugin> localAgentPlugins = new HashSet<ConsistentPlugin>();
        for(Plugin p : localPlugins) {
            if (p.getDeployment() == PluginDeploymentType.AGENT) {
                localAgentPlugins.add(new ConsistentPlugin(p));
            }            
        }
        
        if (localAgentPlugins.size() != pluginsToValidate.size()) {
            throwIncosistentException(localAgentPlugins, pluginsToValidate);
        }

        for (ConsistentPlugin localPlugin : localAgentPlugins) {
            if (!pluginsToValidate.contains(localPlugin)) {
                throwIncosistentException(localAgentPlugins, pluginsToValidate);
            }
        }
    }

    @Override
    public int hashCode() {
        //all the deployed plugins validators are equal
        return 0;
    }

    @Override
    public boolean equals(Object obj) {
        //The deployed plugins validators are all equal to each other
        //because there is no difference in the "output" given by any
        //instance.

        if (this == obj) {
            return true;
        }

        if (obj instanceof DeployedAgentPluginsValidator) {
            return true;
        }

        return false;
    }

    private static void throwIncosistentException(Collection<ConsistentPlugin> localPlugins, Collection<ConsistentPlugin> expectedPlugins)
        throws InconsistentStateException {
        StringBuilder bld = new StringBuilder(
            "Installed plugins are not consistent with the plugins required by the export.");

        bld.append(" The export expects these plugins to be deployed: ");
        appendPlugins(bld, expectedPlugins);
        bld.append(". But the following plugins were found in the local installation: ");
        appendPlugins(bld, localPlugins);

        throw new InconsistentStateException(bld.toString());
    }

    private static void appendPlugins(StringBuilder bld, Collection<ConsistentPlugin> plugins) {
        bld.append("[");
        int size = plugins.size();
        int i = 0;
        for (Plugin p : plugins) {
            ++i;
            appendPlugin(bld, p);
            if (i < size) {
                bld.append(", ");
            }
        }
        bld.append("]");
    }

    private static void appendPlugin(StringBuilder bld, Plugin p) {
        bld.append("Plugin[name='").append(p.getName()).append("'");
        bld.append(", version='").append(p.getVersion()).append("'");
        bld.append(", md5='").append(p.getMd5()).append("']");
    }
}
