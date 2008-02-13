/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.core.util;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.Map;
import java.util.Properties;

/**
 * This utility helps update one or more properties in a .properties file without losing the ordering of existing
 * properties or comment lines. You can update changes to existing properties or add new properties. Currently, there is
 * no way to remove properties from a properties file (but you can set their values to an empty string).
 *
 * <p>Note that this utility only works on simple properties files where each name=value pair exists on single lines
 * (i.e. they do not span multiple lines). But it can handle #-prefixed lines (i.e. comments are preserved).</p>
 *
 * <p>This utility takes care to read and write using the ISO-8859-1 character set since that is what {@link Properties}
 * uses to load and store properties, too.</p>
 *
 * @author John Mazzitelli
 */
public class PropertiesFileUpdate {
    private File file;

    /**
     * Constructor given the full path to the .properties file.
     *
     * @param location location of the file
     */
    public PropertiesFileUpdate(String location) {
        this.file = new File(location);
    }

    /**
     * Updates the properties file so it will contain the key with the value. If value is <code>null</code>, an empty
     * string will be used in the properties file. If the property does not yet exist in the properties file, it will be
     * appended to the end of the file.
     *
     * @param  key   the property name whose value is to be updated
     * @param  value the new property value
     *
     * @throws IOException
     */
    public void update(String key, String value) throws IOException {
        if (value == null) {
            value = "";
        }

        Properties existingProps = loadExistingProperties();

        // if the given property is new (doesn't exist in the file yet) just append it and return
        // if the property exists, update the value in place (ignore if the value isn't really changing)
        if (!existingProps.containsKey(key)) {
            PrintStream ps = new PrintStream(new FileOutputStream(file, true), true, "8859_1");
            ps.println(key + "=" + value);
            ps.flush();
            ps.close();
        } else if (!value.equals(existingProps.getProperty(key))) {
            Properties newProp = new Properties();
            newProp.setProperty(key, value);
            update(newProp);
        }

        return;
    }

    /**
     * Updates the existing properties file with the new properties. If a property is in <code>newProps</code> that
     * already exists in the properties file, the existing property is updated in place. Any new properties found in
     * <code>newProps</code> that does not yet exist in the properties file will be added. Currently existing properties
     * in the properties file that are not found in <code>newProps</code> will remain as-is.
     *
     * @param  newProps properties that are added or updated in the file
     *
     * @throws IOException
     */
    public void update(Properties newProps) throws IOException {
        // make our own copy - we will eventually empty out our copy (also avoids concurrent mod exceptions later)
        Properties propsToUpdate = new Properties();
        propsToUpdate.putAll(newProps);

        // load these in so we don't have to parse out the =value ourselves
        Properties existingProps = loadExistingProperties();

        // Immediately eliminate new properties whose values are the same as the existing properties.
        // Once we finish this, we are assured all new properties are always different than existing properties.
        for (Map.Entry<Object, Object> entry : newProps.entrySet()) {
            if (entry.getValue().equals(existingProps.get(entry.getKey()))) {
                propsToUpdate.remove(entry.getKey());
            }
        }

        // Now go line-by-line in the properties file, updating property values as we go along.
        // When we get to the end of the existing file, append any new props that didn't exist before.
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(baos, true, "8859_1");
        InputStreamReader isr = new InputStreamReader(new FileInputStream(file), "8859_1");
        BufferedReader in = new BufferedReader(isr);

        for (String line = in.readLine(); line != null; line = in.readLine()) {
            int equalsSign = line.indexOf('=');

            // echo lines that are not name=value property lines;
            // this includes blank lines, comments or lines that do not have an = character
            if (line.startsWith("#") || (line.trim().length() == 0) || (equalsSign < 0)) {
                out.println(line);
            } else {
                String existingKey = line.substring(0, equalsSign);
                if (!propsToUpdate.containsKey(existingKey)) {
                    out.println(line); // property that is not being updated; leave it alone and write it out as-is
                } else {
                    out.println(existingKey + "=" + propsToUpdate.getProperty(existingKey));
                    propsToUpdate.remove(existingKey); // done with it so we can remove it from our copy
                }
            }
        }

        // done reading the file, we can close it now
        in.close();

        // append to the output any new properties that did not exist before
        for (Map.Entry<Object, Object> entry : propsToUpdate.entrySet()) {
            out.println(entry.getKey() + "=" + entry.getValue());
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
    public Properties loadExistingProperties() throws IOException {
        Properties props = new Properties();

        if (file.exists()) {
            FileInputStream is = new FileInputStream(file);
            props.load(is);
            is.close();
        }

        return props;
    }
}