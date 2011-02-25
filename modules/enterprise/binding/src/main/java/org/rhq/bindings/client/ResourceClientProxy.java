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
package org.rhq.bindings.client;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javassist.util.proxy.MethodHandler;

import org.rhq.bindings.util.ConfigurationClassBuilder;
import org.rhq.bindings.util.LazyLoadScenario;
import org.rhq.bindings.util.ResourceTypeFingerprint;
import org.rhq.bindings.util.ScriptUtil;
import org.rhq.bindings.util.ShortOutput;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PluginConfigurationUpdate;
import org.rhq.core.domain.configuration.ResourceConfigurationUpdate;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.content.InstalledPackage;
import org.rhq.core.domain.content.PackageType;
import org.rhq.core.domain.content.PackageVersion;
import org.rhq.core.domain.criteria.MeasurementDefinitionCriteria;
import org.rhq.core.domain.criteria.OperationDefinitionCriteria;
import org.rhq.core.domain.criteria.PackageVersionCriteria;
import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.criteria.ResourceOperationHistoryCriteria;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.MeasurementCategory;
import org.rhq.core.domain.measurement.MeasurementData;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.MeasurementUnits;
import org.rhq.core.domain.operation.OperationDefinition;
import org.rhq.core.domain.operation.OperationRequestStatus;
import org.rhq.core.domain.operation.ResourceOperationHistory;
import org.rhq.core.domain.operation.bean.ResourceOperationSchedule;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.core.domain.util.Summary;
import org.rhq.core.server.MeasurementConverter;
import org.rhq.enterprise.server.content.ContentManagerRemote;
import org.rhq.enterprise.server.resource.ResourceTypeNotFoundException;

/**
 * Implements a local object that exposes resource related data as
 * if it were local.
 *
 * @author Greg Hinkle
 * @author Lukas Krejci
 */
public class ResourceClientProxy {

    private static final Log LOG = LogFactory.getLog(ResourceClientProxy.class);
    
    private ResourceClientFactory proxyFactory;
    private RhqFacade remoteClient;
    private int resourceId;
    private Resource resource;

    Map<String, Object> allProperties = new HashMap<String, Object>();

    ResourceTypeFingerprint fingerprint;
    
    // Metadata
    private List<MeasurementDefinition> measurementDefinitions;
    private Map<String, Measurement> measurementMap = new HashMap<String, Measurement>();

    private List<OperationDefinition> operationDefinitions;
    private Map<String, Operation> operationMap = new HashMap<String, Operation>();

    private Map<String, ContentType> contentTypes = new HashMap<String, ContentType>();

    private List<ResourceClientProxy> children;
    ConfigurationDefinition resourceConfigurationDefinition;
    ConfigurationDefinition pluginConfigurationDefinition;

    public ResourceClientProxy() {
    }

    public ResourceClientProxy(ResourceClientProxy parentProxy) {
        this.proxyFactory = parentProxy.proxyFactory;
        this.remoteClient = parentProxy.remoteClient;
        this.resourceId = parentProxy.resourceId;
        this.resource = parentProxy.resource;
        this.allProperties = parentProxy.allProperties;
        this.measurementDefinitions = parentProxy.measurementDefinitions;
        this.measurementMap = parentProxy.measurementMap;
        this.children = parentProxy.children;

    }

    public ResourceClientProxy(ResourceClientFactory factory, int resourceId) {
        this.proxyFactory = factory;
        this.remoteClient = factory.getRemoteClient();
        this.resourceId = resourceId;

        init();
    }

    @Summary(index = 0)
    public int getId() {
        return resourceId;
    }

    @Summary(index = 1)
    public String getName() {
        return resource.getName();
    }

    public String getDescription() {
        return resource.getDescription();
    }

    @Summary(index = 2)
    public String getVersion() {
        return resource.getVersion();
    }

    @Summary(index = 3)
    public ResourceType getResourceType() {
        return resource.getResourceType();
    }

    public Date getCreatedDate() {
        return new Date(resource.getCtime());
    }

    public Date getModifiedDate() {
        return new Date(resource.getCtime());
    }

    public Measurement getMeasurement(String name) {
        return this.measurementMap.get(name);
    }

    public Measurement[] getMeasurements() {
        return this.measurementMap.values().toArray(new Measurement[this.measurementMap.size()]);
    }

    public Operation[] getOperations() {
        return this.operationMap.values().toArray(new Operation[this.operationMap.size()]);
    }

    public Map<String, ContentType> getContentTypes() {
        return contentTypes;
    }

    public ResourceClientProxy[] getChildren() {
        if (children == null && LazyLoadScenario.isShouldLoad()) {
            children = new ArrayList<ResourceClientProxy>();

            initChildren();

        }
        return children.toArray(new ResourceClientProxy[children.size()]);
    }

    public ResourceClientProxy getChild(String name) {
        for (ResourceClientProxy child : getChildren()) {
            if (name.equals(child.getName()))
                return child;
        }
        return null;
    }

    public String toString() {
        return "[" + resourceId + "] " + resource.getName() + " (" + resource.getResourceType().getName() + "::"
            + resource.getResourceType().getPlugin() + ")";
    }

    public void init() {

        this.resource = remoteClient.getResourceManager().getResource(remoteClient.getSubject(), resourceId);

        // Lazy init children, not here
        initMeasurements();
        initOperations();
        initConfigDefs();
        initContent();

        List<PackageType> packageTypes = new ArrayList<PackageType>();
        for(ContentType ct : contentTypes.values()) {
            packageTypes.add(ct.getPackageType());
        }
        
        fingerprint = new ResourceTypeFingerprint(resource.getResourceType(), measurementDefinitions,
            operationDefinitions, packageTypes, pluginConfigurationDefinition, resourceConfigurationDefinition);        
    }

    private void initConfigDefs() {
        this.resourceConfigurationDefinition = remoteClient.getConfigurationManager()
            .getResourceConfigurationDefinitionWithTemplatesForResourceType(remoteClient.getSubject(),
                resource.getResourceType().getId());
        this.pluginConfigurationDefinition = remoteClient.getConfigurationManager()
            .getPluginConfigurationDefinitionForResourceType(remoteClient.getSubject(),
                resource.getResourceType().getId());
    }

    private void initChildren() {
        ResourceCriteria criteria = new ResourceCriteria();
        criteria.addFilterParentResourceId(resourceId);
        PageList<Resource> childResources = remoteClient.getResourceManager().findResourcesByCriteria(
            remoteClient.getSubject(), criteria);

        for (Resource child : childResources) {
            this.children.add(proxyFactory.getResource(child.getId()));
        }
    }

    private void initMeasurements() {
        MeasurementDefinitionCriteria criteria = new MeasurementDefinitionCriteria();
        criteria.addFilterResourceTypeId(resource.getResourceType().getId());
//        criteria.addFilterResourceTypeName(resource.getResourceType().getName());
//        criteria.setStrict(true);

        this.measurementDefinitions = remoteClient.getMeasurementDefinitionManager()
            .findMeasurementDefinitionsByCriteria(remoteClient.getSubject(), criteria);

        this.measurementMap = new HashMap<String, Measurement>();
        for (MeasurementDefinition def : measurementDefinitions) {
            Measurement m = new Measurement(def);

            String name = def.getDisplayName().replaceAll("\\W", "");
            name = decapitalize(name);

            this.measurementMap.put(name, m);
            this.allProperties.put(name, m);
        }
    }

    public void initOperations() {
        OperationDefinitionCriteria criteria = new OperationDefinitionCriteria();
        criteria.addFilterResourceIds(resourceId);
        criteria.fetchParametersConfigurationDefinition(true);
        criteria.fetchResultsConfigurationDefinition(true);

        this.operationDefinitions = remoteClient.getOperationManager().findOperationDefinitionsByCriteria(
            remoteClient.getSubject(), criteria);

        for (OperationDefinition def : operationDefinitions) {
            Operation o = new Operation(def);
            this.operationMap.put(o.getName(), o);
            this.allProperties.put(o.getName(), o);
        }
    }

    private void initContent() {
        ContentManagerRemote contentManager = remoteClient.getContentManager();
        List<PackageType> types = null;
        try {
            types = contentManager.findPackageTypes(remoteClient.getSubject(), resource.getResourceType().getName(),
                resource.getResourceType().getPlugin());

            for (PackageType packageType : types) {
                contentTypes.put(packageType.getName(), new ContentType(packageType));
            }
        } catch (ResourceTypeNotFoundException e) {
            LOG.error("Could not find resource type while creating content mappings of the resource proxy for resource with id " + resourceId, e);
        }

    }

    public class ContentType {

        private PackageType packageType;

        public ContentType(PackageType packageType) {
            this.packageType = packageType;
        }

        public PackageType getPackageType() {
            return this.packageType;
        }

        public List<PackageVersion> getInstalledPackages() {
            ContentManagerRemote contentManager = remoteClient.getContentManager();

            PackageVersionCriteria criteria = new PackageVersionCriteria();
            criteria.addFilterResourceId(resourceId);
            // criteria.addFilterPackageTypeId()  TODO ADD this when the filter is added

            return contentManager.findPackageVersionsByCriteria(remoteClient.getSubject(), criteria);
        }

        public String toString() {
            return this.packageType.getDisplayName();
        }

    }

    public class Measurement implements ShortOutput {

        MeasurementDefinition definition;

        public Measurement(MeasurementDefinition definition) {
            this.definition = definition;
        }

        @Summary(index = 0)
        public String getName() {
            return definition.getDisplayName();
        }

        @Summary(index = 1)
        public String getDisplayValue() {
            Object val = getValue();
            if (val instanceof Number) {
                return MeasurementConverter.format(((Number) val).doubleValue(), getUnits(), true);
            } else {
                return String.valueOf(val);
            }
        }

        @Summary(index = 2)
        public String getDescription() {
            return definition.getDescription();
        }

        public DataType getDataType() {
            return definition.getDataType();
        }

        public MeasurementCategory getCategory() {
            return definition.getCategory();
        }

        public MeasurementUnits getUnits() {
            return definition.getUnits();
        }

        public Object getValue() {
            try {
                Set<MeasurementData> d = remoteClient.getMeasurementDataManager().findLiveData(
                    remoteClient.getSubject(), resourceId, new int[] { definition.getId() });
                MeasurementData data = d.iterator().next();
                return data.getValue();
            } catch (Exception e) {
                return "?";
            }
        }

        public String toString() {
            return getName();
        }
        public String getShortOutput() {
            return getDisplayValue();
        }
    }

    public class Operation {
        OperationDefinition definition;

        public Operation(OperationDefinition definition) {
            this.definition = definition;
        }

        @Summary(index = 0)
        public String getName() {
            return simpleName(this.definition.getDisplayName());
        }

        @Summary(index = 1)
        public String getDescription() {
            return this.definition.getDescription();
        }

        public OperationDefinition getDefinition() {
            return definition;
        }

        public Object invoke(Object[] args) throws Exception {
            if (!LazyLoadScenario.isShouldLoad())
                return null;

            Configuration parameters = ConfigurationClassBuilder.translateParametersToConfig(definition
                .getParametersConfigurationDefinition(), args);

            ResourceOperationSchedule schedule = remoteClient.getOperationManager().scheduleResourceOperation(
                remoteClient.getSubject(), resourceId, definition.getName(), 0, 0, 0, 30000, parameters,
                "Executed from commandline");

            ResourceOperationHistoryCriteria criteria = new ResourceOperationHistoryCriteria();
            criteria.addFilterJobId(schedule.getJobId());
            criteria.addFilterResourceIds(resourceId);
            criteria.addSortStartTime(PageOrdering.DESC); // put most recent at top of results
            criteria.setPaging(0, 1); // only return one result, in effect the latest
            criteria.fetchOperationDefinition(true);
            criteria.fetchParameters(true);
            criteria.fetchResults(true);

            int retries = 10;
            ResourceOperationHistory history = null;
            while (history == null && retries-- > 0) {
                Thread.sleep(1000);
                PageList<ResourceOperationHistory> histories = remoteClient.getOperationManager()
                    .findResourceOperationHistoriesByCriteria(remoteClient.getSubject(), criteria);
                if (histories.size() > 0 && histories.get(0).getStatus() != OperationRequestStatus.INPROGRESS) {
                    history = histories.get(0);
                }
            }

            Configuration result = (history != null ? history.getResults() : null);

            Object returnResults = ConfigurationClassBuilder.translateResults(definition
                .getResultsConfigurationDefinition(), result);

            return returnResults;
        }
        
        @Override
        public String toString() {
            return getName();
        }
    }

    public static class ClientProxyMethodHandler implements MethodHandler, ContentBackedResource, PluginConfigurable,
        ResourceConfigurable {

        ResourceClientProxy resourceClientProxy;
        RhqFacade remoteClient;

        public ClientProxyMethodHandler(ResourceClientProxy resourceClientProxy, RhqFacade remoteClient) {
            this.resourceClientProxy = resourceClientProxy;
            this.remoteClient = remoteClient;
        }

        // ------------------------------------------------------------------------------------------------------
        // Methods here are optional and only accessible if their declared on the custom resource interface class

        public Configuration getPluginConfiguration() {
            if (!LazyLoadScenario.isShouldLoad())
                return null;
            return remoteClient.getConfigurationManager().getPluginConfiguration(remoteClient.getSubject(),
                resourceClientProxy.resourceId);
        }

        public ConfigurationDefinition getPluginConfigurationDefinition() {
            return resourceClientProxy.pluginConfigurationDefinition;
        }

        public PluginConfigurationUpdate updatePluginConfiguration(Configuration configuration) {
            PluginConfigurationUpdate update =
                remoteClient.getConfigurationManager().updatePluginConfiguration(
                    remoteClient.getSubject(),
                    resourceClientProxy.getId(),
                    configuration);

            return update;
        }

        public Configuration getResourceConfiguration() {
            if (!LazyLoadScenario.isShouldLoad())
                return null;

            return remoteClient.getConfigurationManager().getResourceConfiguration(remoteClient.getSubject(),
                resourceClientProxy.resourceId);
        }

        public ConfigurationDefinition getResourceConfigurationDefinition() {
            return resourceClientProxy.resourceConfigurationDefinition;
        }

        public ResourceConfigurationUpdate updateResourceConfiguration(Configuration configuration) {
            ResourceConfigurationUpdate update =
                remoteClient.getConfigurationManager().updateResourceConfiguration(
                    remoteClient.getSubject(),
                    resourceClientProxy.getId(),
                    configuration);

            return update;

        }

        public InstalledPackage getBackingContent() {

            return remoteClient.getContentManager().getBackingPackageForResource(remoteClient.getSubject(), resourceClientProxy.resourceId);
        }

        public void updateBackingContent(String filename) {
            File file = new File(filename);
            if (!file.exists()) {
                throw new IllegalArgumentException("File not found: " + file.getAbsolutePath());
            }
            if (file.isDirectory()) {
                throw new IllegalArgumentException("File expected, found directory: " + file.getAbsolutePath());
            }


            InstalledPackage oldPackage = getBackingContent();


            String oldVersion = oldPackage.getPackageVersion().getVersion();
            String newVersion = "1.0";
            if (oldVersion != null && oldVersion.length() != 0) {
                String[] parts = oldVersion.split("[^a-zA-Z0-9]");
                String lastPart = parts[parts.length-1];
                try {
                    int lastNumber = Integer.parseInt(lastPart);
                    newVersion = oldVersion.substring(0, oldVersion.length() - lastPart.length()) + (lastNumber + 1);
                } catch (NumberFormatException nfe) {
                    newVersion = oldVersion + ".1";
                }
            }

            byte[] fileContents = new ScriptUtil(remoteClient).getFileBytes(filename);


            PackageVersion pv =
                    remoteClient.getContentManager().createPackageVersion(
                        remoteClient.getSubject(),
                        oldPackage.getPackageVersion().getGeneralPackage().getName(),
                        oldPackage.getPackageVersion().getGeneralPackage().getPackageType().getId(),
                        newVersion,
                        oldPackage.getPackageVersion().getArchitecture().getId(),
                        fileContents);

            remoteClient.getContentManager().deployPackages(
                    remoteClient.getSubject(),
                    new int[] { resourceClientProxy.getId()},
                    new int[] {pv.getId()});


        }

        public void retrieveBackingContent(String fileName) throws IOException {

            InstalledPackage installedPackage = getBackingContent();

            if (fileName == null )
                fileName = installedPackage.getPackageVersion().getFileName();
            
            File file = new File(fileName);

            byte[] data =
                    remoteClient.getContentManager().getPackageBytes(
                            remoteClient.getSubject(), resourceClientProxy.resourceId, installedPackage.getId());

            FileOutputStream fos = new FileOutputStream(file);
            fos.write(data);
            fos.close();

        }

        // ------------------------------------------------------------------------------------------------------



        public Object invoke(Object proxy, Method method, Method proceedMethod, Object[] args) throws Throwable {

            if (proceedMethod != null) {
                Method realMethod = getResourceClientProxyClass().getMethod(method.getName(), method.getParameterTypes());
                return realMethod.invoke(resourceClientProxy, args);
            } else {

                try {
                    Method localMethod = getClass().getMethod(method.getName(), method
                        .getParameterTypes());
                    return localMethod.invoke(this, args);
                } catch (NoSuchMethodException nsme) {

                    String name = method.getName();
                    Object key = resourceClientProxy.allProperties.get(name);
                    if (key == null) {
                        name = decapitalize(method.getName().substring(3, method.getName().length()));
                        key = resourceClientProxy.allProperties.get(name);
                    }

                    if (key != null) {
                        if (key instanceof Measurement) {
                            return key;
                        } else if (key instanceof Operation) {
                            resourceClientProxy.proxyFactory.getOutputWriter().println("Invoking operation " + ((Operation) key).getName());

                            return ((Operation) key).invoke(args);

                        }
                    }
                }

                throw new RuntimeException("Can't find custom method: " + method);
            }
        }
        
        protected Class<? extends ResourceClientProxy> getResourceClientProxyClass() {
            return ResourceClientProxy.class;
        }
    }


    static String simpleName(String name) {
        return decapitalize(name.replaceAll("\\W", ""));
    }

    private static String decapitalize(String name) {
        return Character.toLowerCase(name.charAt(0)) + name.substring(1, name.length());
    }

    static String getterName(String name) {
        return "get" + Character.toUpperCase(name.charAt(0)) + name.substring(1, name.length());
    }

    public static interface PluginConfigurable {
        public Configuration getPluginConfiguration();

        public ConfigurationDefinition getPluginConfigurationDefinition();

        public PluginConfigurationUpdate updatePluginConfiguration(Configuration configuration);
    }

    public static interface ResourceConfigurable {
        public Configuration getResourceConfiguration();

        public ConfigurationDefinition getResourceConfigurationDefinition();

        public ResourceConfigurationUpdate updateResourceConfiguration(Configuration configuration);
    }

    public static interface ContentBackedResource {

        public InstalledPackage getBackingContent();


        public void updateBackingContent(String fileName);


        public void retrieveBackingContent(String fileName) throws IOException;

    }
}
