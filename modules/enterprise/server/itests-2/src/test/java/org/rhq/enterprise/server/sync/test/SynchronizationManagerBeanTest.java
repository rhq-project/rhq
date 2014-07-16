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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.persistence.EntityManager;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.common.composite.SystemSetting;
import org.rhq.core.domain.common.composite.SystemSettings;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.criteria.MeasurementDefinitionCriteria;
import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.DisplayType;
import org.rhq.core.domain.measurement.MeasurementCategory;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.MeasurementSchedule;
import org.rhq.core.domain.measurement.MeasurementUnits;
import org.rhq.core.domain.resource.InventoryStatus;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.sync.ExportReport;
import org.rhq.core.domain.sync.ImportConfiguration;
import org.rhq.core.domain.util.PageControl;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.authz.PermissionException;
import org.rhq.enterprise.server.measurement.MeasurementDefinitionManagerLocal;
import org.rhq.enterprise.server.measurement.MeasurementScheduleManagerLocal;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.sync.ExportReader;
import org.rhq.enterprise.server.sync.ExportWriter;
import org.rhq.enterprise.server.sync.MetricTemplateSynchronizer;
import org.rhq.enterprise.server.sync.NoSingleEntity;
import org.rhq.enterprise.server.sync.SynchronizationConstants;
import org.rhq.enterprise.server.sync.SynchronizationManagerLocal;
import org.rhq.enterprise.server.sync.Synchronizer;
import org.rhq.enterprise.server.sync.SynchronizerFactory;
import org.rhq.enterprise.server.sync.SystemSettingsSynchronizer;
import org.rhq.enterprise.server.sync.ValidationException;
import org.rhq.enterprise.server.sync.exporters.Exporter;
import org.rhq.enterprise.server.sync.exporters.ExportingIterator;
import org.rhq.enterprise.server.sync.importers.ExportedEntityMatcher;
import org.rhq.enterprise.server.sync.importers.Importer;
import org.rhq.enterprise.server.sync.validators.ConsistencyValidator;
import org.rhq.enterprise.server.sync.validators.EntityValidator;
import org.rhq.enterprise.server.system.SystemManagerLocal;
import org.rhq.enterprise.server.test.AbstractEJB3Test;
import org.rhq.enterprise.server.test.TestServerPluginService;
import org.rhq.enterprise.server.util.LookupUtil;
import org.rhq.enterprise.server.util.ResourceTreeHelper;

/**
 * Tests the SynchronizationManagerBean on a live dataset.
 *
 * @author Lukas Krejci
 */
@Test
public class SynchronizationManagerBeanTest extends AbstractEJB3Test {

    private SynchronizationManagerLocal synchronizationManager;
    private Subject user;
    private ExportReport export;

    private static final String RESOURCE_TYPE_NAME = "SynchronizationManagerBeanTest";
    private static final String PLUGIN_NAME = "SynchronizationManagerBeanTest";
    private static final String METRIC_NAME = "SynchronizationManagerBeanTest";

    private static class TestData {
        public SystemSettings systemSettings;
        public ResourceType fakeType;
        public Resource fakePlatform;
        public TestServerPluginService testServerPluginService;
    }

    //this can't be mocked out because the config sync machinery has to be able to
    //instantiate this class
    public static class ImportConfigurationCheckingSynchronizer implements Synchronizer<NoSingleEntity, String> {

        public static boolean importerConfigured;
        public static boolean importValidatorsObtainedAfterConfiguration;

        public static void reset() {
            importerConfigured = false;
            importValidatorsObtainedAfterConfiguration = false;
        }

        @Override
        public void initialize(Subject subject, EntityManager entityManager) {
        }

        @Override
        public Exporter<NoSingleEntity, String> getExporter() {
            return new Exporter<NoSingleEntity, String>() {

                @Override
                public ExportingIterator<String> getExportingIterator() {
                    return new ExportingIterator<String>() {
                        boolean ran = false;

                        @Override
                        public boolean hasNext() {
                            if (ran) {
                                return false;
                            }

                            ran = true;
                            return true;
                        }

                        @Override
                        public String next() {
                            return null;
                        }

                        @Override
                        public void remove() {
                        }

                        @Override
                        public void export(ExportWriter output) throws XMLStreamException {
                            output.writeStartElement("my-entity");
                            output.writeCharacters("value");
                            output.writeEndElement();
                        }

                        @Override
                        public String getNotes() {
                            return null;
                        }

                    };
                }

                @Override
                public String getNotes() {
                    return null;
                }

            };
        }

        @Override
        public Importer<NoSingleEntity, String> getImporter() {
            return new Importer<NoSingleEntity, String>() {

                @Override
                public ConfigurationDefinition getImportConfigurationDefinition() {
                    return new ConfigurationDefinition("fake", null);
                }

                @Override
                public void configure(Configuration importConfiguration) {
                    importerConfigured = true;
                }

                @Override
                public ExportedEntityMatcher<NoSingleEntity, String> getExportedEntityMatcher() {
                    return null;
                }

                @Override
                public Set<EntityValidator<String>> getEntityValidators() {
                    EntityValidator<String> v = new EntityValidator<String>() {

                        @Override
                        public void validateExportedEntity(String entity) throws ValidationException {
                        }

                        @Override
                        public void initialize(Subject subject, EntityManager entityManager) {
                        }
                    };

                    if (importerConfigured) {
                        importValidatorsObtainedAfterConfiguration = true;
                    }

                    return Collections.singleton(v);
                }

                @Override
                public void update(NoSingleEntity entity, String exportedEntity) throws Exception {
                }

                @Override
                public String unmarshallExportedEntity(ExportReader reader) throws XMLStreamException {
                    return reader.getElementText();
                }

                @Override
                public String finishImport() throws Exception {
                    return null;
                }

            };
        }

        @Override
        public Set<ConsistencyValidator> getRequiredValidators() {
            return Collections.emptySet();
        }

    }

    private TestData testData;

    //I just don't get why this can't be a @BeforeTest
    //but when it was declared as such (together with tearDown() being an @AfterTest)
    //no tests would work. I have no idea why...
    private void setup(boolean createExport) throws Exception {
        testData = new TestData();
        getTransactionManager().begin();
        try {
            EntityManager em = getEntityManager();

            //add our new metric template that we are going to perform the tests with
            testData.fakeType = new ResourceType(RESOURCE_TYPE_NAME, PLUGIN_NAME, ResourceCategory.PLATFORM, null);

            MeasurementDefinition mdef = new MeasurementDefinition(METRIC_NAME, MeasurementCategory.PERFORMANCE,
                MeasurementUnits.NONE, DataType.MEASUREMENT, true, 600000, DisplayType.SUMMARY);
            testData.fakeType.addMetricDefinition(mdef);

            em.persist(testData.fakeType);

            testData.fakePlatform = new Resource(RESOURCE_TYPE_NAME, RESOURCE_TYPE_NAME, testData.fakeType);
            testData.fakePlatform.setUuid(UUID.randomUUID().toString());
            testData.fakePlatform.setInventoryStatus(InventoryStatus.COMMITTED);

            MeasurementSchedule sched = new MeasurementSchedule(mdef, testData.fakePlatform);
            sched.setInterval(600000);

            testData.fakePlatform.addSchedule(sched);

            em.persist(testData.fakePlatform);

            em.persist(sched);

            em.flush();

            getTransactionManager().commit();
        } catch (Exception e) {
            getTransactionManager().rollback();
            throw e;
        }

        //we need this because the drift plugins are referenced from the system settings that we use in our tests
        testData.testServerPluginService = new TestServerPluginService(getTempDir());
        prepareCustomServerPluginService(testData.testServerPluginService);
        testData.testServerPluginService.startMasterPluginContainer();

        synchronizationManager = LookupUtil.getSynchronizationManager();

        SystemManagerLocal systemManager = LookupUtil.getSystemManager();
        //make sure the system manager is in sync w/ the db we just changed in dbsetup
        systemManager.loadSystemConfigurationCache();

        testData.systemSettings = systemManager.getUnmaskedSystemSettings(false);

        if (createExport) {
            export = synchronizationManager.exportAllSubsystems(freshUser());
        }
    }

    private void tearDown() throws Exception {
        try {
            getTransactionManager().begin();
            try {
                LookupUtil.getSystemManager().setAnySystemSettings(testData.systemSettings, true, true);

                EntityManager em = getEntityManager();

                MeasurementSchedule sched = em.find(MeasurementSchedule.class, testData.fakePlatform.getSchedules()
                    .iterator().next().getId());
                em.remove(sched);

                Resource attachedPlatform = em.find(Resource.class, testData.fakePlatform.getId());
                ResourceTreeHelper.deleteResource(em, attachedPlatform);

                ResourceType attachedType = em.find(ResourceType.class, testData.fakeType.getId());
                em.remove(attachedType);

                em.flush();

                getTransactionManager().commit();
            } catch (Exception e) {
                getTransactionManager().rollback();
                throw e;
            }
        } finally {
            unprepareServerPluginService();
            //unnecessary, done by above method
            //testData.testServerPluginService.stopMasterPluginContainer();

            export = null;
            testData = null;
            synchronizationManager = null;
        }
    }

    public void testExport() throws Exception {
        setup(true);
        try {
            assertNull("Export shouldn't generate an error message.", export.getErrorMessage());
            assertTrue("The export should contain some data.", export.getExportFile().length > 0);
        } finally {
            tearDown();
        }
    }

    public void testImportWithDefaultConfiguration() throws Exception {
        setup(true);

        try {
            SystemManagerLocal systemManager = LookupUtil.getSystemManager();
            MeasurementDefinitionManagerLocal measurementDefinitionManager = LookupUtil
                .getMeasurementDefinitionManager();

            SystemSettings beforeSystemSettings = systemManager.getUnmaskedSystemSettings(false);
            MeasurementDefinitionCriteria criteria = new MeasurementDefinitionCriteria();
            criteria.setPageControl(PageControl.getUnlimitedInstance());
            criteria.fetchResourceType(true);

            List<MeasurementDefinition> beforeMeasurementDefinitions = measurementDefinitionManager
                .findMeasurementDefinitionsByCriteria(freshUser(), criteria);

            synchronizationManager.importAllSubsystems(freshUser(), export.getExportFile(), null);

            //this is to work around BZ 735810
            systemManager.loadSystemConfigurationCache();

            SystemSettings afterSystemSettings = systemManager.getUnmaskedSystemSettings(false);
            List<MeasurementDefinition> afterMeasurementDefinitions = measurementDefinitionManager
                .findMeasurementDefinitionsByCriteria(freshUser(), criteria);

            assertEquals("System settings unexpectedly differ", beforeSystemSettings, afterSystemSettings);

            //make sure we don't fail on simple order differences, which are not important here..
            Set<MeasurementDefinition> beforeDefsToCheck = new HashSet<MeasurementDefinition>(
                beforeMeasurementDefinitions);
            Set<MeasurementDefinition> afterDefsToCheck = new HashSet<MeasurementDefinition>(
                afterMeasurementDefinitions);
            assertEquals("Measurement definitions unexpectedly differ", beforeDefsToCheck, afterDefsToCheck);
        } finally {
            tearDown();
        }
    }

    public void testImportWithRedefinedConfigurationInExportFile() throws Exception {
        setup(true);

        try {
            String exportXML = getExportData();

            exportXML = updateSystemSettingsImportConfiguration(exportXML);
            exportXML = updateMetricTemplatesImportConfiguration(exportXML);

            InputStream exportData = createCompressedStream(exportXML);

            try {
                synchronizationManager.importAllSubsystems(freshUser(), exportData, null);
            } finally {
                exportData.close();
            }

            //now check that everything got imported according to the changed configuration
            SystemManagerLocal systemManager = LookupUtil.getSystemManager();
            SystemSettings settings = systemManager.getUnmaskedSystemSettings(false);

            assertEquals(settings.get(SystemSetting.BASE_URL), "http://testing.domain:7080");

            MeasurementDefinitionManagerLocal measurementDefinitionManager = LookupUtil
                .getMeasurementDefinitionManager();
            MeasurementDefinitionCriteria crit = new MeasurementDefinitionCriteria();
            crit.addFilterResourceTypeName(RESOURCE_TYPE_NAME);
            crit.addFilterName(METRIC_NAME);
            MeasurementDefinition mdef = measurementDefinitionManager.findMeasurementDefinitionsByCriteria(freshUser(),
                crit).get(0);

            assertEquals("The " + METRIC_NAME + " metric should have been updated with default interval of 30s",
                Long.valueOf(30000), new Long(mdef.getDefaultInterval()));

            //ok, and now test that the platform resource schedule was updated as well

            //get the resource id first
            ResourceManagerLocal resourceManager = LookupUtil.getResourceManager();
            ResourceCriteria rcrit = new ResourceCriteria();
            rcrit.addFilterResourceTypeName(RESOURCE_TYPE_NAME);
            List<Resource> platforms = resourceManager.findResourcesByCriteria(freshUser(), rcrit);

            assertEquals("Unexpected number of platform resources found.", 1, platforms.size());

            int platformResourceId = platforms.get(0).getId();

            //now find the schedule for the measurement
            MeasurementScheduleManagerLocal measurementScheduleManager = LookupUtil.getMeasurementScheduleManager();
            List<MeasurementSchedule> schedules = measurementScheduleManager.findSchedulesByResourceIdAndDefinitionIds(
                freshUser(), platformResourceId, new int[] { mdef.getId() });

            assertEquals("Unexpected number of '" + METRIC_NAME + "' schedules found.", 1, schedules.size());

            assertEquals("The schedule should have been updated along with the definition during the config sync",
                30000, schedules.get(0).getInterval());

        } finally {
            tearDown();
        }
    }

    public void testManuallyPassedImportConfigurationHasPrecendenceOverTheInlinedOne() throws Exception {
        setup(true);

        try {
            //let's read the original values from the database, so that we know what to compare against
            SystemManagerLocal systemManager = LookupUtil.getSystemManager();
            SystemSettings settings = systemManager.getUnmaskedSystemSettings(false);

            String originalBaseUrl = settings.get(SystemSetting.BASE_URL);

            MeasurementDefinitionManagerLocal measurementDefinitionManager = LookupUtil
                .getMeasurementDefinitionManager();
            MeasurementDefinitionCriteria crit = new MeasurementDefinitionCriteria();
            crit.addFilterResourceTypeName(RESOURCE_TYPE_NAME);
            crit.addFilterName(METRIC_NAME);
            MeasurementDefinition distroNameDef = measurementDefinitionManager.findMeasurementDefinitionsByCriteria(
                freshUser(), crit).get(0);

            long originalInterval = distroNameDef.getDefaultInterval();

            //now modify the default configuration in the export file
            String exportXML = getExportData();

            exportXML = updateSystemSettingsImportConfiguration(exportXML);
            exportXML = updateMetricTemplatesImportConfiguration(exportXML);

            InputStream exportData = createCompressedStream(exportXML);

            //let's just use the default configs so that we don't apply the changes suggested in
            //the changed default configs created above
            ImportConfiguration systemSettingsConfiguration = new ImportConfiguration(
                SystemSettingsSynchronizer.class.getName(), new SystemSettingsSynchronizer().getImporter()
                    .getImportConfigurationDefinition().getDefaultTemplate().createConfiguration());
            ImportConfiguration metricTemplatesConfiguration = new ImportConfiguration(
                MetricTemplateSynchronizer.class.getName(), new MetricTemplateSynchronizer().getImporter()
                    .getImportConfigurationDefinition().getDefaultTemplate().createConfiguration());

            try {
                synchronizationManager.importAllSubsystems(freshUser(), exportData,
                    Arrays.asList(systemSettingsConfiguration, metricTemplatesConfiguration));
            } finally {
                exportData.close();
            }

            //now check that we import using the manually create configurations, not the inlined ones
            settings = systemManager.getUnmaskedSystemSettings(false);

            assertEquals(settings.get(SystemSetting.BASE_URL), originalBaseUrl);

            measurementDefinitionManager = LookupUtil.getMeasurementDefinitionManager();
            distroNameDef = measurementDefinitionManager.findMeasurementDefinitionsByCriteria(freshUser(), crit).get(0);

            //the definition should have been updated by the data from the export file
            assertEquals("The " + METRIC_NAME + " metric shouldn't have changed its default interval", 30000,
                distroNameDef.getDefaultInterval());

            //ok, and now test that the platform resource schedule was NOT updated because it was configured not to.

            //get the resource id first
            ResourceManagerLocal resourceManager = LookupUtil.getResourceManager();
            ResourceCriteria rcrit = new ResourceCriteria();
            rcrit.addFilterResourceTypeName(RESOURCE_TYPE_NAME);
            List<Resource> platforms = resourceManager.findResourcesByCriteria(freshUser(), rcrit);

            assertEquals("Unexpected number of platform resources found.", 1, platforms.size());

            int platformResourceId = platforms.get(0).getId();

            //now find the schedule for the measurement
            MeasurementScheduleManagerLocal measurementScheduleManager = LookupUtil.getMeasurementScheduleManager();
            List<MeasurementSchedule> schedules = measurementScheduleManager.findSchedulesByResourceIdAndDefinitionIds(
                freshUser(), platformResourceId, new int[] { distroNameDef.getId() });

            assertEquals("Unexpected number of '" + METRIC_NAME + "' schedules found.", 1, schedules.size());

            assertEquals("The schedule should have been updated along with the definition during the config sync",
                originalInterval, schedules.get(0).getInterval());

        } finally {
            tearDown();
        }
    }

    public void testUnknownValidatorsAreIgnored() throws Exception {
        setup(true);

        try {
            String export = getExportData();

            Document xml = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(new InputSource(new StringReader(export)));

            Element unknownValidator = xml.createElement("validator");
            unknownValidator.setAttribute(SynchronizationConstants.CLASS_ATTRIBUTE, "org.nothing.UnknownValidator");

            xml.getDocumentElement().insertBefore(unknownValidator, xml.getDocumentElement().getFirstChild());

            export = documentToString(xml);

            InputStream exportStream = createCompressedStream(export);

            synchronizationManager.importAllSubsystems(freshUser(), exportStream, null);
        } finally {
            tearDown();
        }
    }

    public void testImporterConfiguredBeforeValidatorsObtained() throws Exception {
        setup(false);

        try {
            ImportConfigurationCheckingSynchronizer.reset();

            synchronizationManager.setSynchronizerFactory(new SynchronizerFactory() {
                @Override
                public Set<Synchronizer<?, ?>> getAllSynchronizers() {
                    return Collections.<Synchronizer<?, ?>> singleton(new ImportConfigurationCheckingSynchronizer());
                }
            });

            export = synchronizationManager.exportAllSubsystems(freshUser());

            //and import it back again, so that we actually invoke the import validation
            synchronizationManager.importAllSubsystems(freshUser(), export.getExportFile(), null);

            assertTrue(ImportConfigurationCheckingSynchronizer.importerConfigured);
            assertTrue(ImportConfigurationCheckingSynchronizer.importValidatorsObtainedAfterConfiguration);
        } finally {
            //reset the factory so that other tests work w/ the default syncers
            synchronizationManager.setSynchronizerFactory(new SynchronizerFactory());
            tearDown();
        }
    }

    private String getExportData() throws IOException {
        InputStreamReader str = new InputStreamReader(new GZIPInputStream(new ByteArrayInputStream(
            export.getExportFile())), "UTF-8");
        try {
            char[] buf = new char[32768];
            StringBuilder bld = new StringBuilder();

            int cnt = 0;
            while ((cnt = str.read(buf)) >= 0) {
                bld.append(buf, 0, cnt);
            }
            System.out.println("*********************************************");
            System.out.println(bld.toString());
            System.out.println("*********************************************");
            return bld.toString();
        } finally {
            str.close();
        }
    }

    private static String documentToString(Document document) throws TransformerFactoryConfigurationError,
        TransformerException, IOException {

        StringWriter ret = new StringWriter();
        try {
            Source source = new DOMSource(document);
            Result result = new StreamResult(ret);

            Transformer xformer = TransformerFactory.newInstance().newTransformer();
            xformer.transform(source, result);

            return ret.toString();
        } finally {
            ret.close();
        }
    }

    private String updateSystemSettingsImportConfiguration(String exportXML) throws Exception {
        XPath xpath = XPathFactory.newInstance().newXPath();
        xpath.setNamespaceContext(SynchronizationConstants.createConfigurationExportNamespaceContext());

        XPathExpression systemSettingsConfigurationPath = xpath
            .compile("/:configuration-export/:entities[@id='org.rhq.enterprise.server.sync.SystemSettingsSynchronizer']/:default-configuration/ci:simple-property[@name='propertiesToImport']");

        XPathExpression baseUrlSettingPath = xpath
            .compile("/:configuration-export/:entities[@id='org.rhq.enterprise.server.sync.SystemSettingsSynchronizer']/:entity/:data/systemSettings/entry[@key='CAM_BASE_URL']");

        Element systemSettingsConfiguration = (Element) systemSettingsConfigurationPath.evaluate(new InputSource(
            new StringReader(exportXML)), XPathConstants.NODE);
        String propsToImport = systemSettingsConfiguration.getAttribute("value");

        propsToImport += ", CAM_BASE_URL";

        systemSettingsConfiguration.setAttribute("value", propsToImport);

        exportXML = documentToString(systemSettingsConfiguration.getOwnerDocument());

        Element baseUrlSetting = (Element) baseUrlSettingPath.evaluate(new InputSource(new StringReader(exportXML)),
            XPathConstants.NODE);

        baseUrlSetting.setTextContent("http://testing.domain:7080");

        return documentToString(baseUrlSetting.getOwnerDocument());
    }

    private String updateMetricTemplatesImportConfiguration(String exportXML) throws Exception {
        XPath xpath = XPathFactory.newInstance().newXPath();
        xpath.setNamespaceContext(SynchronizationConstants.createConfigurationExportNamespaceContext());

        XPathExpression overridesPath = xpath
            .compile("/:configuration-export/:entities[@id='org.rhq.enterprise.server.sync.MetricTemplateSynchronizer']/:default-configuration/ci:list-property[@name='metricUpdateOverrides']");

        Element overrides = (Element) overridesPath.evaluate(new InputSource(new StringReader(exportXML)),
            XPathConstants.NODE);

        Document doc = overrides.getOwnerDocument();

        String ns = SynchronizationConstants.CONFIGURATION_INSTANCE_NAMESPACE;
        String prefix = SynchronizationConstants.CONFIGURATION_INSTANCE_NAMESPACE_PREFIX;

        Element values = doc.createElementNS(ns, prefix + ":values");
        overrides.appendChild(values);

        Element mapValue = doc.createElementNS(ns, prefix + ":map-value");
        values.appendChild(mapValue);

        addSimpleValue(mapValue, "metricName", METRIC_NAME);
        addSimpleValue(mapValue, "resourceTypeName", RESOURCE_TYPE_NAME);
        addSimpleValue(mapValue, "resourceTypePlugin", PLUGIN_NAME);
        addSimpleValue(mapValue, "updateSchedules", "true");

        exportXML = documentToString(doc);

        //now redefine the collection interval of the above metric so that we can see the change after the import
        XPathExpression distroNameMetricPath = xpath
            .compile("/:configuration-export/:entities[@id='org.rhq.enterprise.server.sync.MetricTemplateSynchronizer']/:entity/:data/metricTemplate[@metricName='"
                + METRIC_NAME + "']");

        Element metric = (Element) distroNameMetricPath.evaluate(new InputSource(new StringReader(exportXML)),
            XPathConstants.NODE);

        doc = metric.getOwnerDocument();

        metric.setAttribute("defaultInterval", "30000");

        return documentToString(doc);
    }

    private static void addSimpleValue(Element parent, String propertyName, String value) {
        String ns = SynchronizationConstants.CONFIGURATION_INSTANCE_NAMESPACE;
        String prefix = SynchronizationConstants.CONFIGURATION_INSTANCE_NAMESPACE_PREFIX;
        Element simpleValue = parent.getOwnerDocument().createElementNS(ns, prefix + ":simple-value");
        simpleValue.setAttribute("property-name", propertyName);
        simpleValue.setAttribute("value", value);

        parent.appendChild(simpleValue);
    }

    private Subject freshUser() {
        SubjectManagerLocal subjectManager = LookupUtil.getSubjectManager();

        if (user != null) {
            try {
                subjectManager.logout(user);
            } catch (PermissionException e) {
                //we can safely ignore a permission exception during logout...
            }
        }

        user = subjectManager.getOverlord();
        return user;
    }

    private InputStream createCompressedStream(String exportData) throws UnsupportedEncodingException, IOException {
        ByteArrayOutputStream compressed = new ByteArrayOutputStream();
        OutputStreamWriter wrt = new OutputStreamWriter(new GZIPOutputStream(compressed), "UTF-8");
        try {
            wrt.write(exportData);
        } finally {
            wrt.close();
        }

        return new ByteArrayInputStream(compressed.toByteArray());
    }
}
