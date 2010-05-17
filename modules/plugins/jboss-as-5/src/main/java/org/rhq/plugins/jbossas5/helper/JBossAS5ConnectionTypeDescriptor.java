/*
 * Jopr Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.rhq.plugins.jbossas5.helper;

import org.mc4j.ems.connection.support.metadata.JBossConnectionTypeDescriptor;

/**
 * A connection type descriptor to support JNP connections to JBoss AS 5.x/6.x through EMS.
 * 
 * @author Lukas Krejci
 */
public class JBossAS5ConnectionTypeDescriptor extends JBossConnectionTypeDescriptor {
    private static final long serialVersionUID = 1L;

    private static final String[] CONNECTION_CLASSPATH_ENTRIES = new String[0];

    @Override
    public String getRecongnitionPath() {
        return "lib/jboss-main.jar";
    }

    // NOTE: We return an empty array, because this plugin uses RHQ's new plugin connection classloader facility
    //       (see org.rhq.core.pluginapi.inventory.ClassLoaderFacet), rather than EMS, to load JBoss AS client jars via
    //       a connection-scoped classloader.
    @Override
    public String[] getConnectionClasspathEntries() {
        return CONNECTION_CLASSPATH_ENTRIES;
    }
}
