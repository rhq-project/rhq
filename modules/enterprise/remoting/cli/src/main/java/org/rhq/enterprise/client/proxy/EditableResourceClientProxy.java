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

package org.rhq.enterprise.client.proxy;

import org.rhq.bindings.client.ResourceClientProxy;
import org.rhq.bindings.client.RhqFacade;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.enterprise.client.ClientMain;
import org.rhq.enterprise.client.RemoteClient;

/**
 * Extends the {@link ResourceClientProxy} and provides the interactive configuration editing features.
 *
 * @author Lukas Krejci
 */
public class EditableResourceClientProxy extends ResourceClientProxy {

    public interface EditableResourceConfigurable extends ResourceClientProxy.ResourceConfigurable {
        void editResourceConfiguration();        
    }
    
    public interface EditablePluginConfigurable extends ResourceClientProxy.PluginConfigurable {
        public void editPluginConfiguration();
    }
    
    public static class MethodHandler extends ResourceClientProxy.ClientProxyMethodHandler implements EditableResourceConfigurable, EditablePluginConfigurable {
        
        private ClientMain client;
        public MethodHandler(ResourceClientProxy resourceClientProxy, RhqFacade remoteClient, ClientMain client) {
            super(resourceClientProxy, remoteClient);
            this.client = client;
        }

        public void editPluginConfiguration() {
            ConfigurationEditor editor = new ConfigurationEditor(client);
            Configuration config = editor.editConfiguration(getPluginConfigurationDefinition(), getPluginConfiguration());
            if (config != null) {
                updatePluginConfiguration(config);
            }
        }
        
        public void editResourceConfiguration() {
            ConfigurationEditor editor = new ConfigurationEditor(client);
            Configuration config = editor.editConfiguration(getResourceConfigurationDefinition(), getResourceConfiguration());
            if (config != null) {
                updateResourceConfiguration(config);
            }
        }
    }
}
