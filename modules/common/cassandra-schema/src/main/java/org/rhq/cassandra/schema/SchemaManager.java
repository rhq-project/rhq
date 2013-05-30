/*
 *
 *  * RHQ Management Platform
 *  * Copyright (C) 2005-2012 Red Hat, Inc.
 *  * All rights reserved.
 *  *
 *  * This program is free software; you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License, version 2, as
 *  * published by the Free Software Foundation, and/or the GNU Lesser
 *  * General Public License, version 2.1, also as published by the Free
 *  * Software Foundation.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  * GNU General Public License and the GNU Lesser General Public License
 *  * for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * and the GNU Lesser General Public License along with this program;
 *  * if not, write to the Free Software Foundation, Inc.,
 *  * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 */

package org.rhq.cassandra.schema;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ProtocolOptions.Compression;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.exceptions.NoHostAvailableException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.rhq.cassandra.util.ClusterBuilder;
import org.rhq.core.domain.cloud.StorageNode;
import org.rhq.core.util.StringUtil;

/**
 * @author John Sanda
 */
public class SchemaManager {

    private static final String RHQ_KEYSPACE = "rhq";
    private static final String SCHEMA_BASE_FOLDER = "schema";
    private static final String UPDATE_PLAN_ELEMENT = "updatePlan";
    private static final String STEP_ELEMENT = "step";

    private static final String SCHEMA_EXISTS_QUERY = "SELECT * FROM system.schema_keyspaces WHERE keyspace_name = 'rhq';";
    private static final String VERSION_COLUMNFAMILY_EXISTS_QUERY = "SELECT * from system.schema_columnfamilies WHERE keyspace_name='rhq' AND columnfamily_name='schema_version';";
    private static final String VERSION_QUERY = "SELECT version FROM rhq.schema_version";
    private static final String INSERT_VERSION_QUERY = "INSERT INTO rhq.schema_version (version, time ) VALUES ( ?, ?);";


    private final Log log = LogFactory.getLog(SchemaManager.class);

    private enum Task {
        Drop("drop"),
        Create("create"),
        Update("update");

        private final String folder;

        private Task(String folder){
            this.folder = folder;
        }

        protected String getFolder() {
            return "" + SCHEMA_BASE_FOLDER + "/" + this.folder + "/";
        }
    }

    private Session session;
    private String username;
    private String password;

    private List<StorageNode> nodes = new ArrayList<StorageNode>();

    public SchemaManager(String username, String password, String... nodes) {
        this(username, password, parseNodeInformation(nodes));
    }

    public SchemaManager(String username, String password, List<StorageNode> nodes) {
        try {
            this.username = username;
            this.password = password;
            this.nodes = nodes;

            this.initCluster();
            this.shutdown();
        } catch (NoHostAvailableException e) {
            throw new RuntimeException("Unable create storage node session.", e);
        }
    }


    public void install() throws Exception {
        log.info("Preparing to install schema");
        try {
            initCluster();

            if (!schemaExists()) {
                this.executeTask(Task.Create);
            } else {
                log.info("RHQ schema already exists.");
            }

            this.executeTask(Task.Update);
        } catch (NoHostAvailableException e) {
            throw new RuntimeException(e);
        } finally {
            shutdown();
        }
    }

    public void drop() throws Exception {
        log.info("Preparing to drop RHQ schema");
        try {
            initCluster();

            if (schemaExists()) {
                this.executeTask(Task.Drop);
            } else {
                log.info("RHQ schema does not exist. Drop operation not required.");
            }
        } catch (NoHostAvailableException e) {
            throw new RuntimeException(e);
        } finally {
            shutdown();
        }
    }

    private boolean schemaExists() {
        try {
            ResultSet resultSet = session.execute(SCHEMA_EXISTS_QUERY);
            if (!resultSet.all().isEmpty()) {
                resultSet = session.execute(VERSION_COLUMNFAMILY_EXISTS_QUERY);
                return !resultSet.all().isEmpty();
            }
            return false;
        } catch (Exception e) {
            log.error(e);
            throw new RuntimeException(e);
        }
    }

    private int getSchemaVersion() {
        int maxVersion = 0;
        try {
            ResultSet resultSet = session.execute(VERSION_QUERY);
            for (Row row : resultSet.all()) {
                if (maxVersion < row.getInt(0)) {
                    maxVersion = row.getInt(0);
                }
            }
        } catch (Exception e) {
            log.error(e);
            throw new RuntimeException(e);
        }

        return maxVersion;
    }

    private void executeTask(Task task) throws Exception {
        try {
            log.info("Starting to execute " + task + " task.");

            List<String> updateFiles = this.getUpdateFiles(task);

            if (Task.Update.equals(task)) {
                int currentSchemaVersion = this.getSchemaVersion();
                log.info("Current schema version is " + currentSchemaVersion);
                this.removeAppliedUpdates(updateFiles, currentSchemaVersion);
            }

            if (updateFiles.size() == 0 && Task.Update.equals(task)) {
                log.info("RHQ schema is current! No updates applied.");
            }

            for (String updateFile : updateFiles) {
                log.info("Applying file " + updateFile + " for " + task + " task.");
                for (String step : getSteps(updateFile)) {
                    log.info("Statement: \n" + step);
                    session.execute(step);
                }

                if (Task.Update.equals(task)) {
                    this.updateSchemaVersion(updateFile);
                }

                log.info("File " + updateFile + " applied for " + task + " task.");
            }
        } catch (Exception e) {
            log.error(e);
            throw new RuntimeException(e);
        }

        log.info("Successfully executed " + task + " task.");
    }

    private void updateSchemaVersion(String test) {
        PreparedStatement preparedStatement = session.prepare(INSERT_VERSION_QUERY);
        BoundStatement boundStatement = preparedStatement.bind(this.extractVersionFromUpdateFile(test), new Date());
        session.execute(boundStatement);
    }

    private void removeAppliedUpdates(List<String> updateFiles, int currentSchemaVersion) {
        while (!updateFiles.isEmpty()) {
            int version = this.extractVersionFromUpdateFile(updateFiles.get(0));
            if (version <= currentSchemaVersion) {
                updateFiles.remove(0);
            } else {
                break;
            }
        }
    }

    private int extractVersionFromUpdateFile(String file) {
        file = file.substring(file.lastIndexOf('/') + 1);
        file = file.substring(0, file.indexOf('.'));
        return Integer.parseInt(file);
    }

    private List<String> getSteps(String file) throws Exception {
        List<String> steps = new ArrayList<String>();
        InputStream stream = null;
        try {
            stream = this.getClass().getClassLoader().getResourceAsStream(file);

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(stream);

            Node rootDocument = doc.getElementsByTagName(UPDATE_PLAN_ELEMENT).item(0);
            NodeList updateStepElements = rootDocument.getChildNodes();

            for (int index = 0; index < updateStepElements.getLength(); index++) {
                Node updateStepElement = updateStepElements.item(index);
                if (STEP_ELEMENT.equals(updateStepElement.getNodeName()) && updateStepElement.getTextContent() != null) {
                    steps.add(updateStepElement.getTextContent());
                }
            }
        } catch (Exception e) {
            log.error("Error reading the list of steps from " + file + " file.", e);
            throw e;
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (Exception e) {
                    log.error("Error closing the stream with the list of steps from " + file + " file.", e);
                    throw e;
                }
            }
        }

        return steps;
    }

    private List<String> getUpdateFiles(Task task) throws Exception {
        List<String> files = new ArrayList<String>();
        InputStream stream = null;

        try {
            URL resourceFolderURL = this.getClass().getClassLoader().getResource(task.getFolder());

            if (resourceFolderURL.getProtocol().equals("file")) {
                stream = this.getClass().getResourceAsStream(task.getFolder());
                BufferedReader reader = new BufferedReader(new InputStreamReader(stream));

                String updateFile;
                while ((updateFile = reader.readLine()) != null) {
                    files.add(task.getFolder() + updateFile);
                }
            } else if (resourceFolderURL.getProtocol().equals("jar")) {
                URL jarURL = this.getClass().getClassLoader().getResources(task.getFolder()).nextElement();
                JarURLConnection jarURLCon = (JarURLConnection) (jarURL.openConnection());
                JarFile jarFile = jarURLCon.getJarFile();
                Enumeration<JarEntry> entries = jarFile.entries();
                while (entries.hasMoreElements()) {
                    String entry = entries.nextElement().getName();
                    if (entry.startsWith(task.getFolder()) && !entry.equals(task.getFolder())) {
                        files.add(entry);
                    }
                }
            }

            Collections.sort(files, new Comparator<String>() {
                @Override
                public int compare(String o1, String o2) {
                    return o1.compareTo(o2);
                }
            });
        } catch (Exception e) {
            log.error("Error reading the list of update files.", e);
            throw e;
        } finally {
            if (stream != null) {
                try{
                    stream.close();
                } catch (Exception e) {
                    log.error("Error closing the stream with the list of update files.", e);
                    throw e;
                }
            }
        }

        return files;
    }

    private static List<StorageNode> parseNodeInformation(String... nodes) {
        List<StorageNode> parsedNodes = new ArrayList<StorageNode>();
        for (String node : nodes) {
            StorageNode storageNode = new StorageNode();
            storageNode.parseNodeInformation(node);
            parsedNodes.add(storageNode);
        }

        return parsedNodes;
    }

    private void initCluster() throws NoHostAvailableException {
        String[] hostNames = new String[nodes.size()];
        for (int i = 0; i < hostNames.length; ++i) {
            hostNames[i] = nodes.get(i).getAddress();
        }

        log.info("Initializing session to connect to " + StringUtil.arrayToString(hostNames));

        Cluster cluster = new ClusterBuilder().addContactPoints(hostNames).withCredentials(username, password)
            .withPort(nodes.get(0).getCqlPort()).withCompression(Compression.NONE).build();

        log.info("Cluster connection configured.");

        session = cluster.connect("system");
        log.info("Cluster connected.");
    }

    private void shutdown() {
        log.info("Shutting down connections");
        session.getCluster().shutdown();
    }

    public static void main(String[] args) throws Exception {
        try {
            Logger root = Logger.getRootLogger();
            if (!root.getAllAppenders().hasMoreElements()) {
                root.addAppender(new ConsoleAppender(new PatternLayout(PatternLayout.TTCC_CONVERSION_PATTERN)));
            }
            Logger migratorLogging = root.getLoggerRepository().getLogger("org.rhq");
            migratorLogging.setLevel(Level.ALL);

            if (args.length < 4) {
                System.out.println("Usage      : command username password nodes...");
                System.out.println("\n");
                System.out.println("Commands   : install | drop ");
                System.out.println("Node format: hostname|thriftPort|nativeTransportPort");

                return;
            }

            String command = args[0];
            String username = args[1];
            String password = args[2];

            System.out.println(args[3]);

            SchemaManager schemaManager = new SchemaManager(username, password,
                Arrays.copyOfRange(args, 3, args.length));

            System.out.println(command);
            if ("install".equalsIgnoreCase(command)) {
                schemaManager.install();
            } else if ("drop".equalsIgnoreCase(command)) {
                schemaManager.drop();
            } else {
                throw new IllegalArgumentException(command + " not available.");
            }
        } catch (Exception e) {
            System.err.println(e);
        } finally {
            System.exit(0);
        }

    }

}
