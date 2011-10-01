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

package org.rhq.enterprise.server.sync;

import javax.xml.bind.annotation.XmlRootElement;

import org.rhq.enterprise.server.xmlschema.generated.configuration.instance.ConfigurationInstanceDescriptor;

/**
 * 
 *
 * @author Lukas Krejci
 */
@XmlRootElement(name = "default-configuration", namespace = SynchronizationConstants.EXPORT_NAMESPACE)
public class DefaultImportConfigurationDescriptor extends ConfigurationInstanceDescriptor {

    public DefaultImportConfigurationDescriptor() {
        
    }
    
    public static DefaultImportConfigurationDescriptor create(ConfigurationInstanceDescriptor descriptor) {
        DefaultImportConfigurationDescriptor ret = new DefaultImportConfigurationDescriptor();
        ret.configurationProperty = descriptor.getConfigurationProperty();
        return ret;
    }
}
