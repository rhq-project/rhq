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
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
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

    private File file;

    /**
     * Factory method that creates an update object that is appropriate
     * to update the given file.
     * This creates update options for files that have these extensions:
     *
     * <ul>
     * <li>.conf/inc = {@link JavaServiceWrapperConfigurationFileUpdate}</li>
     * <li>.env = {@link JavaServiceWrapperEnvironmentScriptFileUpdate}</li>
     * <li>.bat/cmd = {@link WindowsEnvironmentScriptFileUpdate}</li>
     * <li>anything else = {@link UnixEnvironmentScriptFileUpdate}</li>
     * </ul>
     * 
     * @param location location of the script file
     * 
     * @return the update object that is appropriate to update the file
     */
    public static EnvironmentScriptFileUpdate create(String location) {
        if (location.endsWith(".env")) {
            return new JavaServiceWrapperEnvironmentScriptFileUpdate(location);
        } else if (location.endsWith(".inc") || location.endsWith(".conf")) {
            return new JavaServiceWrapperConfigurationFileUpdate(location);
        } else if (location.endsWith(".bat") || location.endsWith(".cmd")) {
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
    public void update(NameValuePair nvp) throws IOException {
        if (nvp.value == null) {
            nvp.value = "";
        }

        List<NameValuePair> existingList = loadExisting();
        Properties existing = convertNameValuePairListToProperties(existingList);

        // if the given env var is new (doesn't exist in the file yet) just append it and return.
        // if it does exist, update the value in place (ignore if the value isn't really changing)
        if (!existing.containsKey(nvp.name)) {
            PrintStream ps = new PrintStream(new FileOutputStream(file, true), true);
            ps.println();
            ps.println(createEnvironmentVariableLine(nvp));
            ps.flush();
            ps.close();
        } else if (!nvp.value.equals(existing.getProperty(nvp.name))) {
            List<NameValuePair> newList = new ArrayList<NameValuePair>();
            newList.add(nvp);
            update(newList, false);
        }

        return;
    }

    /**
     * Updates the existing script file with the new name/value settings. If an env var is in <code>newValues</code> that
     * already exists in the file, the existing setting is updated in place. Any new setting found in
     * <code>newValues</code> that does not yet exist in the file will be added. Currently existing settings
     * in the script file that are not found in <code>newValues</code> will remain as-is.
     *
     * @param newValuesList environment variable settings that are added or updated in the file
     * @param deleteMissing if <code>true</code>, any settings found in the existing file that are missing
     *                      from the given <code>newValues</code> will be removed from the existing file.
     *                      if <code>false</code>, then <code>newValues</code> is assumed to be only a subset
     *                      of the settings that can go in the file and thus any settings found in the
     *                      existing file but are missing from the new values will not be deleted.
     * @throws IOException
     */
    public void update(List<NameValuePair> newValuesList, boolean deleteMissing) throws IOException {
        Properties newValues = convertNameValuePairListToProperties(newValuesList);

        // make our own copy - we will eventually empty out our copy (also avoids concurrent mod exceptions later)
        Properties settingsToUpdate = new Properties();
        settingsToUpdate.putAll(newValues);

        // prepare the in-memory buffer where we will store the new file contents.
        // yes this means we expect to load the entire file contents in memory, but these
        // files are very small and this should not be a problem.
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(baos, true);

        if (file.exists()) {
            // load these in so we don't have to parse out the =value ourselves
            List<NameValuePair> existingList = loadExisting();
            Properties existing = convertNameValuePairListToProperties(existingList);

            // Immediately eliminate new settings whose values are the same as the existing properties.
            // Once we finish this, we are assured all new properties are always different than existing properties.
            for (Map.Entry<Object, Object> entry : newValues.entrySet()) {
                if (entry.getValue().equals(existing.get(entry.getKey()))) {
                    settingsToUpdate.remove(entry.getKey());
                }
            }

            // Now go line-by-line in the script file, updating name=value settings as we go along.
            // When we get to the end of the existing file, append any new settings that didn't exist before.
            InputStreamReader isr = new InputStreamReader(new FileInputStream(file));
            BufferedReader in = new BufferedReader(isr);

            for (String line = in.readLine(); line != null; line = in.readLine()) {
                NameValuePair nameValue = parseEnvironmentVariableLine(line);

                // echo lines that are not name=value settings;
                // this includes blank lines, comments, etc
                if (nameValue == null) {
                    out.println(line);
                } else {
                    String existingKey = nameValue.name;
                    if (!settingsToUpdate.containsKey(existingKey)) {
                        if (!deleteMissing || newValues.getProperty(existingKey) != null) {
                            out.println(line); // property that is not being updated or removed; leave it alone and write it out as-is
                        }
                    } else {
                        NameValuePair newNvp = new NameValuePair(existingKey, settingsToUpdate.getProperty(existingKey));
                        out.println(createEnvironmentVariableLine(newNvp));
                        settingsToUpdate.remove(existingKey); // done with it so we can remove it from our copy
                    }
                }
            }

            // done reading the file, we can close it now
            in.close();
        }

        // append to the output any new properties that did not exist before
        for (Map.Entry<Object, Object> entry : settingsToUpdate.entrySet()) {
            NameValuePair nvp = new NameValuePair(entry.getKey().toString(), entry.getValue().toString());
            out.println(createEnvironmentVariableLine(nvp));
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
     * If the file does not exist, an empty set of properties is returned.
     * 
     * @return properties that exist in the properties file
     *
     * @throws IOException
     */
    public List<NameValuePair> loadExisting() throws IOException {
        List<NameValuePair> props = new ArrayList<NameValuePair>();

        if (file.exists()) {
            BufferedReader in = new BufferedReader(new FileReader(file));
            try {
                String line = in.readLine();
                while (line != null) {
                    NameValuePair nvp = parseEnvironmentVariableLine(line);
                    if (nvp != null) {
                        props.add(nvp);
                    }
                    line = in.readLine();
                }
            } finally {
                in.close();
            }
        }

        return props;
    }

    public Properties convertNameValuePairListToProperties(List<NameValuePair> list) {
        // converting the list to a properties loses the ordering, but sometimes you don't care
        Properties props = new Properties();
        if (list != null) {
            for (NameValuePair nvp : list) {
                props.setProperty(nvp.name, nvp.value);
            }
        }
        return props;
    }

    public List<NameValuePair> convertPropertiesToNameValuePairList(Properties props) {
        // the ordering if the list is indeterminate - but sometimes you'd like a list object instead of a properties
        List<NameValuePair> list = new ArrayList<NameValuePair>();
        if (props != null) {
            for (Map.Entry<Object, Object> entry : props.entrySet()) {
                list.add(new NameValuePair(entry.getKey().toString(), entry.getValue().toString()));
            }
        }
        return list;
    }

    public static class NameValuePair implements Serializable {
        private static final long serialVersionUID = 1L;
        public String name;
        public String value;

        public NameValuePair(String n, String v) {
            if (n == null) {
                throw new IllegalArgumentException("n == null");
            }
            name = n;
            value = v;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof NameValuePair) {
                return name.equals(((NameValuePair) obj).name);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return name.hashCode();
        }
    }

    /**
     * Creates a line that defines an environment variable name and its value.
     * 
     * @param nvp the environment variable definition
     * 
     * @return the line that is to be used in the script file to define the environment variable
     */
    abstract protected String createEnvironmentVariableLine(NameValuePair nvp);

    /**
     * Parses the given string that is a line from a environment script file.
     * If this is not a line that defines an environment variable, <code>null</code>
     * is returned, otherwise, the environment variable name and value is returned.
     * 
     * @param line the line from the environment script file
     * 
     * @return the name, value of the environment that is defined in the line, or <code>null</code>
     *         if the line doesn't define an env var.
     */
    abstract protected NameValuePair parseEnvironmentVariableLine(String line);
}