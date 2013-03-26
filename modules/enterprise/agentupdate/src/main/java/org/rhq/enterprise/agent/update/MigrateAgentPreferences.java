/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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
package org.rhq.enterprise.agent.update;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Properties;
import java.util.prefs.Preferences;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

/**
 * This migrates agent preferences stored by the native preferences implementation to a file compatible with
 * our custom FilePreferences implementation. It works as follows:
 *
 * @author Jay Shaughnessy
 * @author John Mazzitelli
 */
public class MigrateAgentPreferences extends Task {
    /**
     * This is the top level parent node of all agent preferences and is directly under the userRoot preferences node.
     */
    static private final String NODE_PARENT = "rhq-agent";
    static private final String NODE_DELIM = "/";
    static private final String NODE_PREFIX = NODE_PARENT + NODE_DELIM;
    static private final String DEFAULT_PREFS_FILE = "agent-prefs.properties";
    static private final String MAINTAIN_NATIVE_PREFS_SYSPROP = "rhq.preferences.migrate.keep-native-prefs";
    // Currently keeping native prefs until we're sure we don't need them.
    // static private final String MAINTAIN_NATIVE_PREFS_DEFAULT = "false";
    static private final String MAINTAIN_NATIVE_PREFS_DEFAULT = "true";
    static private final Boolean MAINTAIN_NATIVE_PREFS;

    static {
        MAINTAIN_NATIVE_PREFS = Boolean.valueOf(System.getProperty(MAINTAIN_NATIVE_PREFS_SYSPROP,
            MAINTAIN_NATIVE_PREFS_DEFAULT));
    }

    private File toDir;
    private Boolean failonerror = Boolean.FALSE;

    public void setToDir(File toDir) {
        this.toDir = toDir;
    }

    public void setFailonerror(Boolean flag) {
        this.failonerror = flag;
    }

    /**
     * @see org.apache.tools.ant.Task#execute()
     */
    @Override
    public void execute() throws BuildException {
        validateAttributes();

        try {
            // if the prefs file already exists we can assume migration already happened or is unnecessary
            String filePath = System.getProperty("rhq.preferences.file");
            File toFile = (null != filePath) ? new File(filePath) : new File(toDir, DEFAULT_PREFS_FILE);
            if (toFile.exists()) {
                return;
            }

            Preferences userRoot = Preferences.userRoot();

            // if there are no rhq-agent prefs stored then there is nothing to migrate
            if (!userRoot.nodeExists(NODE_PARENT)) {
                return;
            }

            Properties configProps = new Properties();
            Preferences topNode = userRoot.node(NODE_PARENT);

            // each --pref, including the default preferences are just one level down from the parent
            for (String pref : topNode.childrenNames()) {
                Preferences prefNode = topNode.node(pref);

                for (String key : prefNode.keys()) {
                    String configPropKey = NODE_PREFIX + prefNode.name() + NODE_DELIM + key;
                    String configPropVal = prefNode.get(key, "");
                    configProps.setProperty(configPropKey, configPropVal);
                }

            }

            // write out the new prefs file with the migrated properties       
            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(toFile);
                configProps.store(fos, "Created by RHQ MigrateAgentPreferences tool.");
            } finally {
                if (null != fos) {
                    fos.close();
                }
            }

            // wipe the old prefs to leave a clean system.  In general there will only be one --pref child node,
            // typically "default".  Which means only one agent is actually configured under the "rhq-agent" top node.
            // So, cleaning up and removing the top node should be preferable as it removes litter in the native prefs.
            // If for some unlikely reason multiple agents are configured (using multiple --pref settings) then the user
            // can avoid this cleanup by setting the proper system prop.
            if (!MAINTAIN_NATIVE_PREFS) {
                topNode.removeNode();
            }

        } catch (Throwable e) {
            if (failonerror.booleanValue()) {
                throw new BuildException(e);
            } else {
                log("Failed, but will not exit of failure: " + e);
            }
        }

        return;
    }

    private void validateAttributes() throws BuildException {
        if (null == toDir || !toDir.exists()) {
            throw new BuildException("Must specify existing 'todir' directory");
        }
    }
}