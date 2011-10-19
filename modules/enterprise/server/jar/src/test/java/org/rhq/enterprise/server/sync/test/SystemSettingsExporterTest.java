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

import java.io.ByteArrayInputStream;
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
import org.jmock.Expectations;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.sync.ExporterMessages;
import org.rhq.core.domain.sync.entity.SystemSettings;
import org.rhq.enterprise.server.sync.ExportingInputStream;
import org.rhq.enterprise.server.sync.SynchronizationConstants;
import org.rhq.enterprise.server.sync.Synchronizer;
import org.rhq.enterprise.server.sync.SystemSettingsSynchronizer;
import org.rhq.enterprise.server.system.SystemManagerLocal;
import org.rhq.test.JMockTest;

/**
 * 
 *
 * @author Lukas Krejci
 */
@Test
public class SystemSettingsExporterTest extends JMockTest {

    private static final Log LOG = LogFactory.getLog(MetricTemplateExporterTest.class);

    public void testCanExport() throws Exception {
        final SystemManagerLocal systemManager = context.mock(SystemManagerLocal.class);
        
        context.checking(new Expectations() {
            {
                allowing(systemManager).getSystemConfiguration(with(any(Subject.class)));
                will(returnValue(getFakeSystemConfiguration()));
            }
        });
        
        SystemSettingsSynchronizer exporter = new SystemSettingsSynchronizer(systemManager);

        Set<Synchronizer<?, ?>> exporters = new HashSet<Synchronizer<?, ?>>();
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
//                            <entry key="BaseUrl">herethereandeverywhere</entry>
//                        </systemSettings>
//                    </data>
//                </entity>
//            </entities>
//        </configuration-export>        
        DocumentBuilder bld = DocumentBuilderFactory.newInstance().newDocumentBuilder();

        Document doc = bld.parse(eis);

        Element root = doc.getDocumentElement();

        Element entities = (Element) getFirstDirectChildByTagName(root, SynchronizationConstants.ENTITIES_EXPORT_ELEMENT);

        assertEquals(entities.getAttribute(SynchronizationConstants.ID_ATTRIBUTE), SystemSettingsSynchronizer.class.getName(),
            "Unexpected id of the entities element.");

        NodeList systemSettings = entities.getElementsByTagName("systemSettings");

        assertEquals(systemSettings.getLength(), 1, "Unexpected number of exported system settings.");

        for (int i = 0; i < systemSettings.getLength(); ++i) {
            Element m = (Element) systemSettings.item(i);

            assertEquals(m.getAttribute("referencedEntityId"), "0", "Unexpected referencedEntityId value");
            
            NodeList entries = m.getElementsByTagName("entry");
            
            assertEquals(entries.getLength(), 1, "Unexpected number of entry elements in the system settings export.");
            
            Element baseURL = (Element) entries.item(0);
            
            assertEquals(baseURL.getAttribute("key"), "BaseURL");
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

    private static Properties getFakeSystemConfiguration() {
        HashMap<String, String> values = new HashMap<String, String>();
        values.put("BaseURL", "herethereandeverywhere");
        SystemSettings settings = new SystemSettings(values);

        return settings.toProperties();
    }
}
