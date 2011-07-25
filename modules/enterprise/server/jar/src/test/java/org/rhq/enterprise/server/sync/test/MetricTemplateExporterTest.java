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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.rhq.core.clientapi.descriptor.plugin.PluginDescriptor;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.criteria.MeasurementDefinitionCriteria;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.DisplayType;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.plugin.Plugin;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.sync.ExporterMessages;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.measurement.MeasurementDefinitionManagerLocal;
import org.rhq.enterprise.server.resource.metadata.PluginManagerLocal;
import org.rhq.enterprise.server.resource.metadata.PluginStats;
import org.rhq.enterprise.server.sync.ExportingInputStream;
import org.rhq.enterprise.server.sync.exporters.Exporter;
import org.rhq.enterprise.server.sync.exporters.MetricTemplatesExporter;

/**
 * 
 *
 * @author Lukas Krejci
 */
@Test
public class MetricTemplateExporterTest {

    private static final Log LOG = LogFactory.getLog(MetricTemplateExporterTest.class);

    private MeasurementDefinitionManagerLocal measurementDefManagerStub = new MeasurementDefinitionManagerLocal() {

        @Override
        public void removeMeasurementDefinition(MeasurementDefinition def) {
        }

        @Override
        public MeasurementDefinition getMeasurementDefinition(Subject subject, int definitionId) {
            return null;
        }

        @Override
        public List<MeasurementDefinition> findMeasurementDefinitionsByResourceType(Subject user, int resourceTypeId,
            DataType dataType, DisplayType displayType) {
            return null;
        }

        @Override
        public List<MeasurementDefinition> findMeasurementDefinitionsByIds(Subject subject,
            Integer[] measurementDefinitionIds) {
            return null;
        }

        @Override
        public PageList<MeasurementDefinition> findMeasurementDefinitionsByCriteria(Subject subject,
            MeasurementDefinitionCriteria criteria) {
            List<MeasurementDefinition> ret = new ArrayList<MeasurementDefinition>();

            ResourceType rt = new ResourceType("fakeType", "fakePlugin", ResourceCategory.PLATFORM, null);

            ret.add(new MeasurementDefinition(rt, "m1"));
            ret.add(new MeasurementDefinition(rt, "m2"));
            ret.add(new MeasurementDefinition(rt, "m3"));

            return new PageList<MeasurementDefinition>(ret, PageControl.getUnlimitedInstance());
        }
    };

    private PluginManagerLocal pluginManagerStub = new PluginManagerLocal() {

        @Override
        public void setPluginEnabledFlag(Subject subject, int pluginId, boolean enabled) throws Exception {
        }

        @Override
        public boolean registerPluginTypes(Plugin newPlugin, PluginDescriptor pluginDescriptor, boolean newOrUpdated,
            boolean forceUpdate) throws Exception {
            return false;
        }

        @Override
        public void registerPlugin(Subject subject, Plugin plugin, PluginDescriptor metadata, File pluginFile,
            boolean forceUpdate) throws Exception {
        }

        @Override
        public void purgePlugins(List<Plugin> plugins) {
        }

        @Override
        public void markPluginsForPurge(Subject subject, List<Integer> pluginIds) throws Exception {
        }

        @Override
        public boolean isReadyForPurge(Plugin plugin) {
            return false;
        }

        @Override
        public boolean installPluginJar(Subject subject, Plugin newPlugin, PluginDescriptor pluginDescriptor,
            File pluginFile) throws Exception {
            return false;
        }

        @Override
        public List<Plugin> getPluginsByResourceTypeAndCategory(String resourceTypeName,
            ResourceCategory resourceCategory) {
            return null;
        }

        @Override
        public List<Plugin> getPlugins() {
            return null;
        }

        @Override
        public List<PluginStats> getPluginStats(List<Integer> pluginIds) {
            return null;
        }

        @Override
        public Plugin getPlugin(String name) {
            return null;
        }

        @Override
        public List<Plugin> getInstalledPlugins() {
            Plugin p = new Plugin("fakePlugin", null, "12345");
            p.setVersion("1.0.0.test");
            return Collections.singletonList(p);
        }

        @Override
        public List<Plugin> getAllPluginsById(List<Integer> pluginIds) {
            return null;
        }

        @Override
        public List<Plugin> findPluginsMarkedForPurge() {
            return null;
        }

        @Override
        public List<Plugin> findAllDeletedPlugins() {
            return null;
        }

        @Override
        public void enablePlugins(Subject subject, List<Integer> pluginIds) throws Exception {
        }

        @Override
        public void disablePlugins(Subject subject, List<Integer> pluginIds) throws Exception {
        }

        @Override
        public void deletePlugins(Subject subject, List<Integer> pluginIds) throws Exception {
        }
    };

    public void testCanExport() throws Exception {
        MetricTemplatesExporter exporter = new MetricTemplatesExporter(null, measurementDefManagerStub,
            pluginManagerStub);

        Set<Exporter<?, ?>> exporters = new HashSet<Exporter<?, ?>>();
        exporters.add(exporter);

        InputStream eis = new ExportingInputStream(exporters, new HashMap<String, ExporterMessages>(), 65536, false);

        String exportContents = readAll(new InputStreamReader(eis, "UTF-8"));

        LOG.warn("Export contents:\n" + exportContents);

//        eis = new ByteArrayInputStream(exportContents.getBytes("UTF-8"));
//
//        DocumentBuilder bld = DocumentBuilderFactory.newInstance().newDocumentBuilder();
//
//        Document doc = bld.parse(eis);
//
//        Element root = doc.getDocumentElement();

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
}
