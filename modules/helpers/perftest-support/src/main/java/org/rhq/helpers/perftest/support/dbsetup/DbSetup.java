/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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

package org.rhq.helpers.perftest.support.dbsetup;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Target;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.UnknownElement;
import org.apache.tools.ant.helper.ProjectHelper2;
import org.rhq.core.db.ant.dbupgrade.DBUpgrader;
import org.rhq.core.db.setup.DBSetup;

/**
 * This class is a utility wrapper around the actual {@link DBSetup} and {@link DBUpgrader} classes defined in the
 * rhq-core-dbutils module.
 *
 * @author Lukas Krejci
 */
public class DbSetup {

    private static final String MINIMAL_VERSION_OF_DATA = "db-data-combined.2.94.xml";
    private static final String MINIMAL_VERSION_OF_SCHEMA = "db-schema-combined.2.94.xml";

    private static final Map<String, String> REPLACEMENTS;

    static {
        REPLACEMENTS = new HashMap<String, String>();
        // these are all the replacements from the dbsetup-build.xml
        REPLACEMENTS.put("@@@SERVER_VERSION@@@", "project.version");
        REPLACEMENTS.put("@@@DB_SCHEMA_VERSION@@@", "db.schema.version");
        REPLACEMENTS.put("@@@ADMINUSERNAME@@@", "server.admin.username");
        REPLACEMENTS.put("@@@ADMINPASSWORD@@@", "server.admin.password.encrypted");
        REPLACEMENTS.put("@@@ADMINEMAIL@@@", "server.admin.email");
        REPLACEMENTS.put("@@@BASEURL@@@", "server.webapp.baseurl");
        REPLACEMENTS.put("@@@JAASPROVIDER@@@", "server.jaas.provider");
        REPLACEMENTS.put("@@@LDAPURL@@@", "server.ldap.url");
        REPLACEMENTS.put("@@@LDAPPROTOCOL@@@", "server.ldap.protocol");
        REPLACEMENTS.put("@@@LDAPLOGINPROP@@@", "server.ldap.loginProperty");
        REPLACEMENTS.put("@@@LDAPBASEDN@@@", "server.ldap.baseDN");
        REPLACEMENTS.put("@@@LDAPSEARCHFILTER@@@", "server.ldap.searchFilter");
        REPLACEMENTS.put("@@@LDAPBINDDN@@@", "server.ldap.bindDN");
        REPLACEMENTS.put("@@@LDAPBINDPW@@@", "server.ldap.bindPW");
        REPLACEMENTS.put("@@@MULTICAST_ADDR@@@", "server.highavail.address");
        REPLACEMENTS.put("@@@MULTICAST_PORT@@@", "server.highavail.port");
    }

    private Connection connection;

    public DbSetup(Connection connection) throws Exception {
        this.connection = connection;
    }

    public void setup(String targetVersion) throws Exception {
        setup();
        upgrade(targetVersion);
    }

    public void upgrade(String targetVersion) throws Exception {
        Project project = new Project();
        File upgradeFile = getFileFromDbUtils("db-upgrade.xml");

        try {
            project.setCoreLoader(getClass().getClassLoader());
            project.init();
            project.setProperty("target.schema.version", targetVersion == null ? "LATEST" : targetVersion);
            loadDbSetupAntTasksProperties(project);

            new ProjectHelper2().parse(project, upgradeFile);

            Target defaultTarget = (Target) project.getTargets().get(project.getDefaultTarget());

            for (Task t : defaultTarget.getTasks()) {
                DBUpgrader upgrader = null;
                if (t instanceof DBUpgrader) {
                    upgrader = (DBUpgrader) t;
                } else if (t instanceof UnknownElement) {
                    if ("dbupgrade".equals(t.getTaskType())) {
                        UnknownElement u = (UnknownElement)t;
                        u.maybeConfigure();

                        if (u.getTask() instanceof DBUpgrader) {
                            upgrader = (DBUpgrader) u.getTask();
                        }
                    }
                }

                if (upgrader != null) {
                    upgrader.setConnection(connection);
                    break;
                }
            }

            project.executeTarget(project.getDefaultTarget());
        } catch (BuildException e) {
            throw new RuntimeException("Cannot run ANT on script [" + upgradeFile + "]. Cause: " + e, e);
        } finally {
            upgradeFile.delete();
        }
    }

    private void setup() throws Exception {
        DBSetup dbSetup = new DBSetup(connection);

        File minimalSchema = getFileFromResource(MINIMAL_VERSION_OF_SCHEMA, getClass().getClassLoader());
        File currentSchema = getFileFromDbUtils("db-schema-combined.xml");
        
        try {
            replaceTokensInFile(minimalSchema);
            replaceTokensInFile(currentSchema);
            dbSetup.uninstall(currentSchema.getAbsolutePath());
            dbSetup.setup(minimalSchema.getAbsolutePath());
        } finally {
            minimalSchema.delete();
            currentSchema.delete();
        }

        File data = getFileFromResource(MINIMAL_VERSION_OF_DATA, getClass().getClassLoader());

        try {
            replaceTokensInFile(data);
            dbSetup.setup(data.getAbsolutePath(), null, true, false);
        } finally {
            data.delete();
        }
    }

    private void replaceTokensInFile(File f) throws IOException {
        Properties properties = getDbSetupProperties();

        String contents = readIntoString(new FileInputStream(f));

        for(Map.Entry<String, String> entry : REPLACEMENTS.entrySet()) {
            String token = entry.getKey();
            String value = properties.getProperty(token);
            if (value != null) {
                contents = contents.replaceAll(token, value);
            }
        }

        FileWriter wrt = new FileWriter(f);

        try {
            wrt.write(contents.toCharArray());
        } finally {
            safeClose(wrt);
        }
    }

    private static Properties getDbSetupProperties() throws IOException {
        Properties dbSetupProperties = loadPropertiesFromDbUtils("dbsetup.properties");
        //add the project.version manually because that has to be found out in a different way
        dbSetupProperties.put("project.version", DbSetup.class.getPackage().getImplementationVersion());
        return dbSetupProperties;
    }

    private static void loadDbSetupAntTasksProperties(Project project) throws Exception {
        Properties taskDefs = loadPropertiesFromDbUtils("db-ant-tasks.properties");

        for(Map.Entry<Object, Object> entry : taskDefs.entrySet()) {
            String taskName = (String) entry.getKey();
            String taskClassName = (String) entry.getValue();

            project.addTaskDefinition(taskName, Class.forName(taskClassName));
        }
    }

    private static Properties loadPropertiesFromDbUtils(String resourceName) throws IOException {
        InputStream propertiesStream = DBSetup.class.getClassLoader().getResourceAsStream(resourceName);
        try {
            Properties properties = new Properties();
            properties.load(propertiesStream);
            return properties;
        } finally {
            safeClose(propertiesStream);
        }
    }

    private static File getFileFromDbUtils(String fileName) throws IOException {
        return getFileFromResource(fileName, DBSetup.class.getClassLoader());
    }

    private static File getFileFromResource(String resourceName, ClassLoader cl) throws IOException {
        InputStream stream = cl.getResourceAsStream(resourceName);

        if (stream == null) {
            throw new FileNotFoundException("Could not find " + resourceName + " in the classloader.");
        }

        File tmpFile = File.createTempFile("DbSetup", null);
        OutputStream out = new BufferedOutputStream(new FileOutputStream(tmpFile));

        try {
            copy(stream, out);

            return tmpFile;
        } finally {
            safeClose(stream, out);
        }
    }

    private String readIntoString(InputStream s) throws IOException {
        char[] buffer = new char[32768];
        StringBuilder bld = new StringBuilder();

        BufferedReader rdr = new BufferedReader(new InputStreamReader(s));

        try {
            int cnt = 0;

            while ((cnt = rdr.read(buffer, 0, buffer.length)) != -1) {
                bld.append(buffer, 0, cnt);
            }

            return bld.toString();
        } finally {
            rdr.close();
        }
    }
    private static void copy(InputStream source, OutputStream target) throws IOException {
        byte[] buffer = new byte[32768];

        int cnt = 0;
        while ((cnt = source.read(buffer, 0, buffer.length)) != -1) {
            target.write(buffer, 0, cnt);
        }

        target.flush();
    }

    private static void safeClose(Closeable... streams) {
        for(Closeable stream : streams) {
            try {
                if (stream!=null)
                    stream.close();
            } catch (IOException e) {
                //ignore
            }
        }
    }
}
