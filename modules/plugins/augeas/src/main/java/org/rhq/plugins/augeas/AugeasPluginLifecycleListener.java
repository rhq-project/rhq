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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.pluginapi.plugin.PluginContext;
import org.rhq.core.pluginapi.plugin.PluginLifecycleListener;
import org.rhq.core.util.stream.StreamUtil;

/**
 * This is a plugin lifecycle listener object for the abstract Augeas plugin.
 * It is used to copy the lenses from the standard location of 'META-INF/augeas-lenses/'
 * to the data directory of the plugin, in the "augeas-lenses" subdirectory ({@link #LENSES_SUBDIRECTORY_NAME}).
 * <p>
 * The list of lenses to copy is obtained from the file META-INF/augeas-lenses/list which is assumed to contain
 * a list of all the files that should be on the augeas load path, each on a new line. This file is allowed to
 * have comments - a line starting with '#' is considered a comment.
 * 
 * @author Lukas Krejci
 */
public class AugeasPluginLifecycleListener implements PluginLifecycleListener {
    private static final Log LOG = LogFactory.getLog(AugeasPluginLifecycleListener.class);

    public static final String LENSES_SUBDIRECTORY_NAME = "augeas-lenses";
    
    public void initialize(PluginContext context) throws Exception {
        List<String> lenses = getLenses(context.getPluginName());
        
        File lensesDir = ensureLensesDirExists(context.getDataDirectory(), context.getPluginName());
        
        copyToDir(lenses, lensesDir, context.getPluginName());
    }

    public void shutdown() {
    }
    
    private List<String> getLenses(String pluginName) throws IOException {
        InputStream lensesDescriptor = getClass().getResourceAsStream("/META-INF/augeas-lenses/list");
        
        if (lensesDescriptor == null) {
            return Collections.emptyList();
        }        
        
        BufferedReader rdr = null;
        try {
            rdr = new BufferedReader(new InputStreamReader(lensesDescriptor, "UTF-8"));
            
            ArrayList<String> ret = new ArrayList<String>();
            
            String line;
            while ((line = rdr.readLine()) != null) {
                if (line.startsWith("#") || line.trim().isEmpty()) {
                    continue;
                }
                
                ret.add(line.trim());
            }
            
            return ret;
        } finally {
            try {
                rdr.close();
            } catch (IOException e) {
                LOG.warn("Failed to close the reader of the 'META-INF/augeas-lenses/list' file in the plugin jar of plugin '" + pluginName + "'.");
            }
        }
    }
    
    private void copyToDir(List<String> lenses, File targetDir, String pluginName) throws IOException {
        for (String lens : lenses) {
            URL lensURL = null;
            
            if (lens.startsWith("/")) {
                lensURL = getClass().getResource(lens);
            } else {
                lensURL = getClass().getResource("/META-INF/augeas-lenses/" + lens);
            }
            
            String lensName = lensURL.getPath();
            if (lensName.indexOf('/') >= 0) {
                lensName = lensName.substring(lensName.lastIndexOf('/') + 1);
            }
            
            copyURLToFile(lensURL, new File(targetDir, lensName));
        }
    }
    
    private static File ensureLensesDirExists(File dataDir, String pluginName) {
        if (!dataDir.exists()) {
            if (!dataDir.mkdir()) {
                throw new IllegalStateException("Failed to create the data directory for plugin '" + pluginName + "'.");
            }
        }
        
        File lensesDir = new File(dataDir, LENSES_SUBDIRECTORY_NAME);
        
        if (lensesDir.exists()) {
            //clear it out
            for(File f : lensesDir.listFiles()) {
                f.delete();
            }
        } else if (!lensesDir.mkdir()) {
            throw new IllegalStateException("Failed to create the lenses subdirectory under the data directory for plugin '" + pluginName + "'.");
        }
        
        return lensesDir;
    }
    
    private static void copyURLToFile(URL source, File target) throws IOException {
        InputStream sourceStream = source.openStream();
        FileOutputStream targetStream = new FileOutputStream(target);
        
        StreamUtil.copy(sourceStream, targetStream, true);
    }
}
