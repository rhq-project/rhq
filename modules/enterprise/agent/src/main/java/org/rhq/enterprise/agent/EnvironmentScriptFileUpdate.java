/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.enterprise.agent;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.Map;
import java.util.Properties;

/**
 * This utility helps update one or more environment script files without losing the ordering of existing
 * variables or comment lines. An "environment script" is a script file that simply contains lines that
 * set environment variables (i.e. "RHQ_AGENT_MY_VAR=some.value" or "set NAME=VALUE" for Windows scripts).
 * 
 * <p>You can update changes to existing environment variables or add new ones.</p>
 * 
 * <p>Note that this utility only works on simple environment script files where each name=value
 * pair exists on single lines (i.e. they do not span multiple lines). But it can handle
 * commented lines (i.e. comments are preserved).</p>
 *
 * @author John Mazzitelli
 */
public abstract class EnvironmentScriptFileUpdate {
    public static void main(String[] args) throws Exception {
        EnvironmentScriptFileUpdate fsh;
        fsh = EnvironmentScriptFileUpdate.create("C:/mazz/tmp/rhq-agent-env.sh");
        java.util.Properties psh = fsh.loadExisting();
        fsh.update("FOO_SH", "foo sh value");
        psh.put("RHQ_AGENT_HOME", "/another/agent/home");
        fsh.update(psh);

        EnvironmentScriptFileUpdate fbat;
        fbat = EnvironmentScriptFileUpdate.create("C:/mazz/tmp/rhq-agent-env.bat");
        java.util.Properties pbat = fbat.loadExisting();
        fbat.update("FOO_BAT", "foo bat value");
        pbat.put("RHQ_AGENT_HOME", "/another/agent/home");
        fbat.update(pbat);
    }

    private File file;

    /**
     * Factory method that creates an update object that is appropriate
     * to update the given file.
     *  
     * @param location location of the script file
     * 
     * @return the update object that is appropriate to update the file
     */
    public static EnvironmentScriptFileUpdate create(String location) {
        if (location.endsWith(".bat") || location.endsWith(".cmd")) {
            return new WindowsEnvironmentScriptFileUpdate(location);
        } else {
            return new UnixEnvironmentScriptFileUpdate(location);
        }
    }

    /**
     * Constructor given the full path to script file.
     *
     * @param location location of the file
     */
    public EnvironmentScriptFileUpdate(String location) {
        this.file = new File(location);
    }

    /**
     * Updates the script file so it will contain the key with the value (where the key is the
     * name of the environment variable). If value is <code>null</code>, an empty
     * string will be used in file. If the variable does not yet exist in the properties file, it will be
     * appended to the end of the file.
     *
     * @param  key   the env var name whose value is to be updated
     * @param  value the new env var value
     *
     * @throws IOException
     */
    public void update(String key, String value) throws IOException {
        if (value == null) {
            value = "";
        }

        Properties existing = loadExisting();

        // if the given env var is new (doesn't exist in the file yet) just append it and return.
        // if it does exist, update the value in place (ignore if the value isn't really changing)
        if (!existing.containsKey(key)) {
            PrintStream ps = new PrintStream(new FileOutputStream(file, true), true);
            ps.println();
            ps.println(createEnvironmentVariableLine(key, value));
            ps.flush();
            ps.close();
        } else if (!value.equals(existing.getProperty(key))) {
            Properties newKey = new Properties();
            newKey.setProperty(key, value);
            update(newKey);
        }

        return;
    }

    /**
     * Updates the existing script file with the new name/value settings. If an env var is in <code>newValues</code> that
     * already exists in the file, the existing setting is updated in place. Any new setting found in
     * <code>newValues</code> that does not yet exist in the file will be added. Currently existing settings
     * in the script file that are not found in <code>newValues</code> will remain as-is.
     *
     * @param  newValues environment variable settings that are added or updated in the file
     *
     * @throws IOException
     */
    public void update(Properties newValues) throws IOException {
        // make our own copy - we will eventually empty out our copy (also avoids concurrent mod exceptions later)
        Properties settingsToUpdate = new Properties();
        settingsToUpdate.putAll(newValues);

        // load these in so we don't have to parse out the =value ourselves
        Properties existing = loadExisting();

        // Immediately eliminate new settings whose values are the same as the existing properties.
        // Once we finish this, we are assured all new properties are always different than existing properties.
        for (Map.Entry<Object, Object> entry : newValues.entrySet()) {
            if (entry.getValue().equals(existing.get(entry.getKey()))) {
                settingsToUpdate.remove(entry.getKey());
            }
        }

        // Now go line-by-line in the script file, updating name=value settings as we go along.
        // When we get to the end of the existing file, append any new settings that didn't exist before.
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(baos, true);
        InputStreamReader isr = new InputStreamReader(new FileInputStream(file));
        BufferedReader in = new BufferedReader(isr);

        for (String line = in.readLine(); line != null; line = in.readLine()) {
            String[] nameValue = parseEnvironmentVariableLine(line);

            // echo lines that are not name=value settings;
            // this includes blank lines, comments, etc
            if (nameValue == null) {
                out.println(line);
            } else {
                String existingKey = nameValue[0];
                if (!settingsToUpdate.containsKey(existingKey)) {
                    out.println(line); // property that is not being updated; leave it alone and write it out as-is
                } else {
                    out.println(createEnvironmentVariableLine(existingKey, settingsToUpdate.getProperty(existingKey)));
                    settingsToUpdate.remove(existingKey); // done with it so we can remove it from our copy
                }
            }
        }

        // done reading the file, we can close it now
        in.close();

        // append to the output any new properties that did not exist before
        for (Map.Entry<Object, Object> entry : settingsToUpdate.entrySet()) {
            out.println(createEnvironmentVariableLine(entry.getKey().toString(), entry.getValue().toString()));
        }

        // done with building the contents of the updated properties file
        out.close();

        // now we can take the new contents of the file and overwrite the contents of the old file
        FileOutputStream fos = new FileOutputStream(file, false);
        fos.write(baos.toByteArray());
        fos.flush();
        fos.close();

        return;
    }

    /**
     * Loads and returns the properties that exist currently in the properties file.
     *
     * @return properties that exist in the properties file
     *
     * @throws IOException
     */
    public Properties loadExisting() throws IOException {
        Properties props = new Properties();

        if (file.exists()) {
            BufferedReader in = new BufferedReader(new FileReader(file));
            try {
                String line = in.readLine();
                while (line != null) {
                    String[] nameValue = parseEnvironmentVariableLine(line);
                    if (nameValue != null) {
                        props.setProperty(nameValue[0], nameValue[1]);
                    }
                    line = in.readLine();
                }
            } finally {
                in.close();
            }
        }

        return props;
    }

    /**
     * Creates a line that defines an environment variable name and its value.
     * 
     * @param key the environment variable name
     * @param value the environment variable value
     * 
     * @return the line that is to be used in the script file to define the environment variable
     */
    abstract protected String createEnvironmentVariableLine(String key, String value);

    /**
     * Parses the given string that is a line from a environment script file.
     * If this is not a line that defines an environment variable, <code>null</code>
     * is returned, otherwise, the first element in the array is the environment variable
     * name and the second element is the value of that environment variable.
     * 
     * @param line the line from the environment script file
     * 
     * @return the name, value of the environment that is defined in the line, or <code>null</code>
     *         if the line doesn't define an env var.
     */
    abstract protected String[] parseEnvironmentVariableLine(String line);
}