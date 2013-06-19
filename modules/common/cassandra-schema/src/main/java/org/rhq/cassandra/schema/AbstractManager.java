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
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ProtocolOptions.Compression;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.exceptions.NoHostAvailableException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.rhq.cassandra.util.ClusterBuilder;
import org.rhq.core.domain.cloud.StorageNode;
import org.rhq.core.util.StringUtil;

/**
 * @author Stefan Negrea
 */
public class AbstractManager {

    private static final String UPDATE_PLAN_ELEMENT = "updatePlan";
    private static final String STEP_ELEMENT = "step";

    private static final String SCHEMA_EXISTS_QUERY = "SELECT * FROM system.schema_keyspaces WHERE keyspace_name = 'rhq';";
    private static final String VERSION_COLUMNFAMILY_EXISTS_QUERY = "SELECT * from system.schema_columnfamilies WHERE keyspace_name='rhq' AND columnfamily_name='schema_version';";
    private static final String VERSION_QUERY = "SELECT version FROM rhq.schema_version";
    private static final String REPLICATION_FACTOR_QUERY = "SELECT strategy_options FROM system.schema_keyspaces where keyspace_name='rhq';";



    private final Log log = LogFactory.getLog(AbstractManager.class);

    protected Session session;
    protected final String username;
    protected final String password;
    protected List<StorageNode> nodes = new ArrayList<StorageNode>();

    public AbstractManager(String username, String password, List<StorageNode> nodes) {
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

    protected boolean schemaExists() {
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

    protected int getSchemaVersion() {
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

    protected void removeAppliedUpdates(List<String> updateFiles, int currentSchemaVersion) {
        while (!updateFiles.isEmpty()) {
            int version = this.extractVersionFromUpdateFile(updateFiles.get(0));
            if (version <= currentSchemaVersion) {
                updateFiles.remove(0);
            } else {
                break;
            }
        }
    }

    protected int extractVersionFromUpdateFile(String file) {
        file = file.substring(file.lastIndexOf('/') + 1);
        file = file.substring(0, file.indexOf('.'));
        return Integer.parseInt(file);
    }

    protected List<String> getSteps(String file) throws Exception {
        List<String> steps = new ArrayList<String>();
        InputStream stream = null;
        try {
            stream = SchemaManager.class.getClassLoader().getResourceAsStream(file);

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

    protected List<String> getUpdateFiles(String folder) throws Exception {
        List<String> files = new ArrayList<String>();
        InputStream stream = null;

        try {
            URL resourceFolderURL = this.getClass().getClassLoader().getResource(folder);

            if (resourceFolderURL.getProtocol().equals("file")) {
                stream = this.getClass().getClassLoader().getResourceAsStream(folder);
                BufferedReader reader = new BufferedReader(new InputStreamReader(stream));

                String updateFile;
                while ((updateFile = reader.readLine()) != null) {
                    files.add(folder + updateFile);
                }
            } else if (resourceFolderURL.getProtocol().equals("jar")) {
                URL jarURL = this.getClass().getClassLoader().getResources(folder).nextElement();
                JarURLConnection jarURLCon = (JarURLConnection) (jarURL.openConnection());
                JarFile jarFile = jarURLCon.getJarFile();
                Enumeration<JarEntry> entries = jarFile.entries();
                while (entries.hasMoreElements()) {
                    String entry = entries.nextElement().getName();
                    if (entry.startsWith(folder) && !entry.equals(folder)) {
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


    protected void initCluster() throws NoHostAvailableException {
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

    protected void shutdown() {
        log.info("Shutting down connections");
        session.getCluster().shutdown();
    }

    protected int getReplicationFactor() {
        int replicationFactor = 1;
        try {
            String replicationFactorString = "replication_factor\"";

            ResultSet resultSet = session.execute(REPLICATION_FACTOR_QUERY);
            Row row = resultSet.one();

            String resultString = row.getString(0);
            resultString = resultString.substring(resultString.indexOf(replicationFactorString)
                + replicationFactorString.length());
            resultString = resultString.substring(resultString.indexOf('"') + 1);
            resultString = resultString.substring(0, resultString.indexOf('"'));

            replicationFactor = Integer.parseInt(resultString);
        } catch (Exception e) {
            log.error(e);
        }

        return replicationFactor;
    }
}
