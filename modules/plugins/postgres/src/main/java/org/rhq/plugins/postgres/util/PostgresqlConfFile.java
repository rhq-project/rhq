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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package org.rhq.plugins.postgres.util;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import org.rhq.core.util.stream.StreamUtil;

/**
 * Represents a PostgreSQL configuration file (i.e. postgresql.conf) - provides methods for reading and updating
 * configuration parameters.
 *
 * @author Greg Hinkle
 * @author Ian Springer
 */
public class PostgresqlConfFile {

    private Log log = LogFactory.getLog(PostgresqlConfFile.class);

    private Map<String, String> properties = new HashMap<String, String>();

    private File configurationFile;

    private static final String LINE_SEPARATOR = System.getProperty("line.separator");

    public PostgresqlConfFile(File configurationFile) throws IOException {
        if (!configurationFile.exists()) {
            throw new IOException("PostgreSQL configuration file [" + configurationFile
                    + "] does not exist or is not readable. Make sure the user the RHQ Agent is running as has read permissions on the file and its parent directory.");
        }

        if (!configurationFile.canRead()) {
            throw new IOException("PostgreSQL configuration file [" + configurationFile
                    + "] is not readable. Make sure the user the RHQ Agent is running as has read permissions on the file.");
        }

        this.configurationFile = configurationFile;

        // declared outside the try block, so they can be safely closed in finally block
        BufferedReader r = null;
        try {
            r = new BufferedReader(new FileReader(configurationFile));
            Pattern p = Pattern.compile("^" // start match at the beginning of lines
                + "\\s*" // optional whitespace
                + "(\\w+)" // a word, captured
                + "\\s*" // optional whitespace
                + "\\=" // '='
                + "\\s*" // optional whitespace
                + "(" // then capture either
                + "(?:\\w+)" // a word; non-captured, but captured by outer parentheses
                + "|" // or
                + "(?:\\'[^\\']*\\')" // a sequence of non-apostrophe characters surrounded by two apostrophes;
                // also non-captured, but captured by outer parenthesis
                + ")" + ".*" // ignored the rest of the line
                + "$"); // end match at the end of lines
            String line;
            while ((line = r.readLine()) != null) {
                Matcher m = p.matcher(line);
                if (m.matches()) {
                    String val = m.group(2);
                    if (val.startsWith("'") && val.endsWith("'")) {
                        val = val.substring(1, val.length() - 1);
                    }

                    this.properties.put(m.group(1), val);
                }
            }
        } finally {
            StreamUtil.safeClose(r);
        }
    }

    @Nullable
    public String getProperty(String paramName) {
        return this.properties.get(paramName);
    }

    @NotNull
    public List<String> getPropertyList(String propertyName) {
        String prop = getProperty(propertyName);
        
        if (prop == null) return Collections.emptyList();
        
        return Arrays.asList(stripQuotes(prop).split(","));
    }
    
    @NotNull
    public static String stripQuotes(@NotNull String value) {
        if (value.startsWith("'") && value.endsWith("'")) {
            return value.substring(1, value.length() - 1);
        }
        
        return value;
    }
    
    @Nullable
    public String getPort() {
        return getProperty("port");
    }

    public void setProperties(Map<String, String> parameters) {
        // declared outside the try block, so they can be safely closed in finally block
        BufferedReader r = null;
        BufferedOutputStream bos = null;

        try {
            r = new BufferedReader(new FileReader(configurationFile));
            StringBuilder newBuffer = new StringBuilder();

            Pattern p = Pattern.compile("^" // start match at the beginning of lines
                + "\\s*" // optional whitespace
                + "(\\#?)" // an optional '#' sign, signifying a commented line, captured
                + "\\s*" // optional whitespace
                + "(\\w+)" // a word, captured
                + "\\s*" // optional whitespace
                + "\\=" // '='
                + "\\s*" // optional whitespace
                + "(" // then capture either
                + "(?:\\w+)" // a word; non-captured, but captured by outer parentheses
                + "|" // or
                + "(?:\\'[^\\']*\\')" // a sequence of non-apostrophe characters surrounded by two apostrophes;
                // also non-captured, but captured by outer parenthesis
                + ")" + "(.*)" // capture the rest of the characters on the line
                + "$"); // end match at the end of lines

            String line;
            // update existing properties

            while ((line = r.readLine()) != null) {
                Matcher m = p.matcher(line);
                if (m.matches()) {
                    boolean isCommented = m.group(1).equals("#");
                    String paramName = m.group(2);
                    String oldParamValue = m.group(3);
                    String tail = m.group(4);
                    if (oldParamValue.startsWith("'") && oldParamValue.endsWith("'")) {
                        //noinspection UnusedAssignment
                        oldParamValue = oldParamValue.substring(1, oldParamValue.length() - 1);
                    }

                    if (parameters.containsKey(paramName)) {
                        String paramValue = parameters.get(paramName);
                        if (paramValue == null) {
                            // A null value means the parameter should not be specified, so
                            // comment out the original line if it wasn't already commented out.
                            if (!isCommented) {
                                newBuffer.append('#');
                            }

                            newBuffer.append(line).append(LINE_SEPARATOR);
                        } else {
                            newBuffer.append(paramName).append(" = ").append(paramValue);
                            if (tail != null) {
                                newBuffer.append(tail);
                            }

                            newBuffer.append(LINE_SEPARATOR);
                        }

                        parameters.remove(paramName);
                    } else {
                        newBuffer.append(line).append(LINE_SEPARATOR);
                    }
                } else {
                    newBuffer.append(line).append(LINE_SEPARATOR);
                }
            }

            // add previously non-existent, non-null params at the end
            for (String paramName : parameters.keySet()) {
                String paramValue = parameters.get(paramName);
                if (paramValue != null) {
                    newBuffer.append(paramName).append(" = ").append(paramValue).append(LINE_SEPARATOR);
                }
            }

            backupFile(configurationFile);

            bos = new BufferedOutputStream(new FileOutputStream(configurationFile));
            bos.write(newBuffer.toString().getBytes());
        } catch (FileNotFoundException e) {
            log.warn("Unable to find Postgres configuration file", e);
        } catch (IOException e) {
            log.warn("Unable to update Postgres configuration file", e);
        } finally {
            try {
                r.close();
                bos.close();
            } catch (IOException e) {
                // ignore
            }
        }
    }

    private static void backupFile(File file) throws IOException {
        String backupName = file.getName();
        File dir = file.getParentFile();
        int i = 0;
        while (new File(dir, backupName + ".bak." + i).exists()) {
            i++;
        }

        backupName += ".bak." + i;
        copyFile(file, new File(dir, backupName));
    }

    private static void copyFile(File in, File out) throws IOException {
        FileInputStream fis = new FileInputStream(in);
        FileOutputStream fos = new FileOutputStream(out);
        byte[] buf = new byte[1024];
        int i;
        try {
            while ((i = fis.read(buf)) != -1) {
                fos.write(buf, 0, i);
            }
        } finally {
            try {
                fis.close();
                fos.close();
            } catch (IOException e) {
                // ignore
            }
        }
    }

    @Override
    public String toString() {
        return this.configurationFile.toString();
    }

}
