/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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

package org.rhq.enterprise.server.sync.test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.rhq.core.db.DatabaseType;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.common.ProductInfo;
import org.rhq.core.domain.common.ServerDetails;
import org.rhq.core.domain.sync.ExporterMessages;
import org.rhq.core.domain.sync.entity.SystemSettings;
import org.rhq.enterprise.server.sync.ExportingInputStream;
import org.rhq.enterprise.server.sync.exporters.Exporter;
import org.rhq.enterprise.server.sync.exporters.MetricTemplateExporter;
import org.rhq.enterprise.server.sync.exporters.SystemSettingsExporter;
import org.rhq.enterprise.server.sync.importers.SystemSettingsImporter;
import org.rhq.enterprise.server.sync.validators.ConsistencyValidator;
import org.rhq.enterprise.server.system.SystemManagerLocal;

/**
 * 
 *
 * @author Lukas Krejci
 */
@Test
public class SystemSettingsExporterTest {

    private static final Log LOG = LogFactory.getLog(MetricTemplateExporterTest.class);

    private SystemManagerLocal systemManagerStub = new SystemManagerLocal() {

        @Override
        public long vacuumAppdef(Subject whoami) {
            return 0;
        }

        @Override
        public long vacuum(Subject whoami, String[] tableNames) {
            return 0;
        }

        @Override
        public long vacuum(Subject whoami) {
            return 0;
        }

        @Override
        public void undeployInstaller() {
        }

        @Override
        public void setSystemConfiguration(Subject subject, Properties properties, boolean skipValidation)
            throws Exception {
        }

        @Override
        public void scheduleConfigCacheReloader() {
        }

        @Override
        public long reindex(Subject whoami) {
            return 0;
        }

        @Override
        public void reconfigureSystem(Subject whoami) {
        }

        @Override
        public void loadSystemConfigurationCacheInNewTx() {
        }

        @Override
        public void loadSystemConfigurationCache() {
        }

        @Override
        public boolean isExperimentalFeaturesEnabled() {
            return false;
        }

        @Override
        public boolean isDebugModeEnabled() {
            return false;
        }

        @Override
        public Properties getSystemConfiguration(Subject subject) {
            SystemSettings settings = new SystemSettings();

            settings.setBaseURL("herethereandeverywhere");

            return settings.toProperties();
        }

        @Override
        public ServerDetails getServerDetails(Subject subject) {
            return null;
        }

        @Override
        public ProductInfo getProductInfo(Subject subject) {
            return null;
        }

        @Override
        public DatabaseType getDatabaseType() {
            return null;
        }

        @Override
        public void enableHibernateStatistics() {
        }

        @Override
        public long analyze(Subject whoami) {
            return 0;
        }
    };

    public void testCanExport() throws Exception {
        SystemSettingsExporter exporter = new SystemSettingsExporter(systemManagerStub);

        Set<Exporter<?, ?>> exporters = new HashSet<Exporter<?, ?>>();
        exporters.add(exporter);

        InputStream eis = new ExportingInputStream(exporters, new HashMap<String, ExporterMessages>(), 65536, false);

        //        String exportContents = readAll(new InputStreamReader(eis, "UTF-8"));
        //        
        //        LOG.warn("Export contents:\n" + exportContents);
        //        
        //        eis = new ByteArrayInputStream(exportContents.getBytes("UTF-8"));

//         <?xml version="1.0" ?>
//        <configuration-export>
//            <entities id="org.rhq.enterprise.server.sync.importers.SystemSettingsImporter">
//                <entity>
//                    <data>
//                        <systemSettings referencedEntityId="0">
//                            <jAASProvider></jAASProvider>
//                            <jDBCJAASProvider></jDBCJAASProvider>
//                            <lDAPJAASProvider></lDAPJAASProvider>
//                            <lDAPFactory></lDAPFactory>
//                            <lDAPUrl></lDAPUrl>
//                            <lDAPProtocol></lDAPProtocol>
//                            <lDAPLoginProperty></lDAPLoginProperty>
//                            <lDAPFilter></lDAPFilter>
//                            <lDAPGroupFilter></lDAPGroupFilter>
//                            <lDAPGroupMember></lDAPGroupMember>
//                            <lDAPBaseDN></lDAPBaseDN>
//                            <lDAPBindDN></lDAPBindDN>
//                            <lDAPBindPW></lDAPBindPW>
//                            <baseURL>herethereandeverywhere</baseURL>
//                            <agentMaxQuietTimeAllowed></agentMaxQuietTimeAllowed>
//                            <enableAgentAutoUpdate></enableAgentAutoUpdate>
//                            <enableDebugMode></enableDebugMode>
//                            <enableExperimentalFeatures></enableExperimentalFeatures>
//                            <dataPurge1Hour></dataPurge1Hour>
//                            <dataPurge6Hour></dataPurge6Hour>
//                            <dataPurge1Day></dataPurge1Day>
//                            <dataMaintenance></dataMaintenance>
//                            <dataReindex></dataReindex>
//                            <rtDataPurge></rtDataPurge>
//                            <alertPurge></alertPurge>
//                            <eventPurge></eventPurge>
//                            <traitPurge></traitPurge>
//                            <availabilityPurge></availabilityPurge>
//                            <baselineFrequency></baselineFrequency>
//                            <baselineDataSet></baselineDataSet>
//                        </systemSettings>
//                    </data>
//                </entity>
//            </entities>
//        </configuration-export>        
        DocumentBuilder bld = DocumentBuilderFactory.newInstance().newDocumentBuilder();

        Document doc = bld.parse(eis);

        Element root = doc.getDocumentElement();

        Element entities = (Element) getFirstDirectChildByTagName(root, ExportingInputStream.ENTITIES_EXPORT_ELEMENT);

        assertEquals(entities.getAttribute(ExportingInputStream.ID_ATTRIBUTE), SystemSettingsImporter.class.getName(),
            "Unexpected id of the entities element.");

        NodeList systemSettings = entities.getElementsByTagName("systemSettings");

        assertEquals(systemSettings.getLength(), 1, "Unexpected number of exported system settings.");

        for (int i = 0; i < systemSettings.getLength(); ++i) {
            Element m = (Element) systemSettings.item(i);

            assertEquals(m.getAttribute("referencedEntityId"), "0", "Unexpected referencedEntityId value");
            
            //i'm too lazy to repeat the check for the presence of all 30 properties in the export file
            //let's just count them.
            assertEquals(m.getChildNodes().getLength(), 30, "Unexpected number of properties in the system settings.");
            
            //but let's check that the explicitly assigned value to baseURL has been exported.
            NodeList baseURLs = m.getElementsByTagName("baseURL");
            
            assertEquals(baseURLs.getLength(), 1, "Unexpected number of baseURL elements in the system settings export.");
            
            Element baseURL = (Element) baseURLs.item(0);
            
            assertEquals(baseURL.getTextContent(), "herethereandeverywhere", "Unexpected value of baseURL");
        }
    }

    private static String readAll(Reader rdr) throws IOException {
        try {
            StringBuilder bld = new StringBuilder();
            int c;
            while ((c = rdr.read()) != -1) {
                bld.append((char) c);
            }

            return bld.toString();
        } finally {
            rdr.close();
        }
    }

    private static Node getFirstDirectChildByTagName(Node node, String tagName) {
        for (int i = 0; i < node.getChildNodes().getLength(); ++i) {
            Node n = node.getChildNodes().item(i);
            if (n.getNodeName().equals(tagName)) {
                return n;
            }
        }

        return null;
    }

    private static List<Node> getDirectChildrenByTagName(Node node, String tagName) {
        List<Node> ret = new ArrayList<Node>();
        for (int i = 0; i < node.getChildNodes().getLength(); ++i) {
            Node n = node.getChildNodes().item(i);
            if (n.getNodeName().equals(tagName)) {
                ret.add(n);
            }
        }

        return ret;
    }
}
