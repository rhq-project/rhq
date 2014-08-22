/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package org.rhq.plugins.jmx.connection.metadata;

import java.util.Properties;

import org.mc4j.ems.connection.ConnectionFactory;
import org.mc4j.ems.connection.support.metadata.JSR160ConnectionTypeDescriptor;

/**
 * @author Thomas Segismont
 */
public class AS7ConnectionTypeDescriptor extends JSR160ConnectionTypeDescriptor {
    @Override
    public String getConnectionType() {
        return "AS7";
    }

    @Override
    public String getDisplayName() {
        return "JBoss AS7 and Red Hat JBoss EAP6 connection type";
    }

    @Override
    public String getDefaultServerUrl() {
        return "service:jmx:remoting-jmx://localhost:9999";
    }

    @Override
    public String[] getConnectionClasspathEntries() {
        return new String[] { "bin/client/jboss-client.jar" };
    }

    @Override
    public boolean isUseChildFirstClassLoader() {
        return true;
    }

    @Override
    public Properties getDefaultAdvancedProperties() {
        Properties properties = super.getDefaultAdvancedProperties();
        properties.put(ConnectionFactory.USE_CONTEXT_CLASSLOADER, Boolean.FALSE.toString());
        return properties;
    }
}
