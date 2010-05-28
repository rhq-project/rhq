/*
 * RHQ Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
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
package org.rhq.plugins.augeas;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.pc.PluginContainer;
import org.rhq.core.pluginapi.plugin.PluginContext;
import org.rhq.core.pluginapi.plugin.PluginLifecycleListener;

/**
 * This is a plugin lifecycle listener object for the abstract Augeas plugin.
 * It is used to load and unload the augeas shared library from memory.
 * 
 * @author John Mazzitelli
 */
public class AugeasPluginLifecycleListener implements PluginLifecycleListener {
    private static final Log log = LogFactory.getLog(AugeasPluginLifecycleListener.class);

    
    public void initialize(PluginContext context) throws Exception {       
    }

    public void shutdown() {
    }
}
