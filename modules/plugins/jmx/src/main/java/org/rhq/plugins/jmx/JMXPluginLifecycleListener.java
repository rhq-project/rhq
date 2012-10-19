/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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

package org.rhq.plugins.jmx;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mc4j.ems.connection.support.classloader.ClassLoaderFactory;

import org.rhq.core.pluginapi.plugin.PluginContext;
import org.rhq.core.pluginapi.plugin.PluginLifecycleListener;

public class JMXPluginLifecycleListener implements PluginLifecycleListener {
    private final Log log = LogFactory.getLog(JMXPluginLifecycleListener.class);

    @Override
    public void initialize(PluginContext context) throws Exception {
        // no-op
    }

    @Override
    public void shutdown() {
        // so we do not cause EMS classloaders to leak perm gen, we need to clear
        // out all caches from the EMS ClassLoaderFactory
        try {
            ClassLoaderFactory.clearCaches();
            log.debug("Cleared EMS ClassLoaderFactory caches");
        } catch (Exception e) {
            log.error("Failed to clear EMS ClassLoaderFactory caches - perm gen may leak", e);
        }
    }
}
