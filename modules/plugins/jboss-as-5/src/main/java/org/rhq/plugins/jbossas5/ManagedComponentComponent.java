 /*
  * Jopr Management Platform
  * Copyright (C) 2005-2008 Red Hat, Inc.
  * All rights reserved.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of the GNU General Public License, version 2, as
  * published by the Free Software Foundation, and/or the GNU Lesser
  * General Public License, version 2.1, also as published by the Free
  * Software Foundation.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
  * GNU General Public License and the GNU Lesser General Public License
  * for more details.
  *
  * You should have received a copy of the GNU General Public License
  * and the GNU Lesser General Public License along with this program;
  * if not, write to the Free Software Foundation, Inc.,
  * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
  */
package org.rhq.plugins.jbossas5;

 import java.util.Map;
 import java.util.Set;

 import org.apache.commons.logging.Log;
 import org.apache.commons.logging.LogFactory;

 import org.jboss.deployers.spi.management.ManagementView;
 import org.jboss.managed.api.ComponentType;
 import org.jboss.managed.api.ManagedComponent;
 import org.jboss.managed.api.ManagedDeployment;
 import org.jboss.managed.api.ManagedOperation;
 import org.jboss.managed.api.ManagedProperty;
 import org.jboss.managed.api.RunState;
 import org.jboss.metatype.api.values.CompositeValue;
 import org.jboss.metatype.api.values.MetaValue;
 import org.jboss.metatype.api.values.SimpleValue;

 import org.rhq.core.domain.configuration.Configuration;
 import org.rhq.core.domain.measurement.AvailabilityType;
 import org.rhq.core.domain.measurement.DataType;
 import org.rhq.core.domain.measurement.MeasurementDataNumeric;
 import org.rhq.core.domain.measurement.MeasurementDataTrait;
 import org.rhq.core.domain.measurement.MeasurementReport;
 import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
 import org.rhq.core.pluginapi.configuration.ConfigurationFacet;
 import org.rhq.core.pluginapi.inventory.DeleteResourceFacet;
 import org.rhq.core.pluginapi.inventory.ResourceComponent;
 import org.rhq.core.pluginapi.inventory.ResourceContext;
 import org.rhq.core.pluginapi.measurement.MeasurementFacet;
 import org.rhq.core.pluginapi.operation.OperationFacet;
 import org.rhq.core.pluginapi.operation.OperationResult;
 import org.rhq.plugins.jbossas5.factory.ProfileServiceFactory;
 import org.rhq.plugins.jbossas5.util.ConversionUtils;

 /**
 * Service ResourceComponent for all {@link ManagedComponent}s in a Profile.
 *
 * @author Jason Dobies
 * @author Mark Spritzler
 * @author Ian Springer
 */
public class ManagedComponentComponent extends AbstractManagedComponent
        implements ResourceComponent, ConfigurationFacet, DeleteResourceFacet, OperationFacet, MeasurementFacet
{
    public static final String RESOURCE_TYPE_EAR = "Enterprise Application (EAR)";
    public static final String RESOURCE_TYPE_WAR = "Web Application (WAR)";

    public static final String COMPONENT_TYPE_PROPERTY = "componentType";
    public static final String COMPONENT_SUBTYPE_PROPERTY = "componentSubtype";
    public static final String COMPONENT_NAME_PROPERTY = "componentName";

    private final Log log = LogFactory.getLog(this.getClass());

    private String componentName;
    private ComponentType componentType;
    private static final MetaValue[] EMPTY_META_VALUE_ARRAY = new MetaValue[0];

    // ResourceComponent Implementation  --------------------------------------------

    public AvailabilityType getAvailability()
    {
        RunState runState = getManagedComponent().getRunState();
        return (runState == RunState.RUNNING) ? AvailabilityType.UP :
                AvailabilityType.DOWN;
    }

    public void start(ResourceContext resourceContext) throws Exception {
        super.start(resourceContext);
        this.componentType = ConversionUtils.getComponentType(getResourceContext().getResourceType());
        Configuration pluginConfig = resourceContext.getPluginConfiguration();
        this.componentName = pluginConfig.getSimple(COMPONENT_NAME_PROPERTY).getStringValue();
        return;
    }

    public void stop()
    {
        return;
    }

    // DeleteResourceFacet Implementation  --------------------------------------------

    public void deleteResource() throws Exception
    {
        log.debug("Deleting ManagedComponent [" + this.componentName + "]...");
        ManagementView managementView = ProfileServiceFactory.getCurrentProfileView();
        ManagedComponent managedComponent = getManagedComponent();
        ManagedDeployment deployment = managedComponent.getDeployment();
        deployment.removeComponent(this.componentName);
        managementView.process();
    }

    // OperationFacet Implementation  --------------------------------------------

    public OperationResult invokeOperation(String name, Configuration parameters) throws Exception
    {
        ManagedOperation operation = getManagedOperation(name);
        if (operation == null)
            throw new IllegalStateException("ManagedOperation named [" + name + "] not found on ManagedComponent ["
                    + getManagedComponent() + "].");
        // Convert parameters into MetaValue array.
        MetaValue[] parameterMetaValues = ConversionUtils.convertOperationsParametersToMetaValues(operation, parameters,
                getResourceContext().getResourceType());
        // invoke() takes a varargs, so we must pass an empty array, rather than null.
        if (parameterMetaValues == null)
            parameterMetaValues = EMPTY_META_VALUE_ARRAY;
        MetaValue resultMetaValue = operation.invoke(parameterMetaValues);
        // Convert result to Correct Property type.
        OperationResult results = new OperationResult();
        ConversionUtils.convertManagedOperationResults(operation, resultMetaValue, results.getComplexResults(),
                getResourceContext().getResourceType());
        
        return results;
    }

    // MeasurementFacet Implementation  --------------------------------------------

    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> metrics) throws Exception
    {
        ManagedComponent managedComponent = getManagedComponent();
        for (MeasurementScheduleRequest request : metrics)
        {
            try {
                if (request.getName().equals("runState")) {
                    String runState = managedComponent.getRunState().name();
                    report.addData(new MeasurementDataTrait(request, runState));
                } else {
                    SimpleValue simpleValue = getSimpleValue(managedComponent, request);
                    if (simpleValue != null)
                        addSimpleValueToMeasurementReport(report, request, simpleValue);
                }
            } catch (Exception e) {
                log.error("Failed to collect metric for " + request, e);
            }
        }
    }

    // ------------ AbstractManagedComponent implementation -------------

    protected Map<String, ManagedProperty> getManagedProperties() {
        return getManagedComponent().getProperties();
    }

    protected Log getLog() {
        return this.log;
    }

    protected void updateComponent() throws Exception {
        ManagementView managementView = ProfileServiceFactory.getCurrentProfileView();
        ManagedComponent managedComponent = ProfileServiceFactory.getManagedComponent(managementView,
                this.componentType, this.componentName);
        managementView.updateComponent(managedComponent);
        managementView.process();
    }

    // ------------------------------------------------------------------------------

    private SimpleValue getSimpleValue(ManagedComponent managedComponent, MeasurementScheduleRequest request) {
        String metricName = request.getName();
        int dotIndex = metricName.indexOf('.');
        String metricPropName = (dotIndex == -1) ? metricName : metricName.substring(0, dotIndex);
        ManagedProperty metricProp = managedComponent.getProperty(metricPropName);
        SimpleValue simpleValue;
        if (dotIndex == -1)
            simpleValue = (SimpleValue)metricProp.getValue();
        else {
            CompositeValue compositeValue = (CompositeValue)metricProp.getValue();
            String key = metricName.substring(dotIndex + 1);
            simpleValue = (SimpleValue)compositeValue.get(key);
        }
        if (simpleValue == null)
            log.debug("Profile service returned null value for metric [" + request.getName() + "].");
        return simpleValue;
    }

    private void addSimpleValueToMeasurementReport(MeasurementReport report, MeasurementScheduleRequest request, SimpleValue simpleValue) {
        DataType dataType = request.getDataType();
        switch (dataType) {
            case MEASUREMENT:
                try {
                    MeasurementDataNumeric dataNumeric = new MeasurementDataNumeric(request,
                            Double.valueOf(simpleValue.getValue().toString()));
                    report.addData(dataNumeric);
                }
                catch (NumberFormatException e) {
                    log.error("Profile service did not return a numeric value as expected for metric ["
                            + request.getName() + "] - value returned was " + simpleValue + ".", e);
                }
                break;
            case TRAIT:
                MeasurementDataTrait dataTrait = new MeasurementDataTrait(request,
                        String.valueOf(simpleValue.getValue()));
                report.addData(dataTrait);
                break;
            default:
                throw new IllegalStateException("Unsupported measurement data type: " + dataType);
        }
    }

    private ManagedComponent getManagedComponent()
    {
        try {
            //ProfileServiceFactory.refreshCurrentProfileView();
            ManagementView managementView = ProfileServiceFactory.getCurrentProfileView();
            return ProfileServiceFactory.getManagedComponent(managementView, this.componentType, this.componentName);
        }
        catch (Exception e) {
            throw new RuntimeException("Failed to load ManagedComponent [" + this.componentName + "].", e);
        }
    }

    private ManagedOperation getManagedOperation(String name)
    {
        ManagedComponent managedComponent = getManagedComponent();
        Set<ManagedOperation> operations = managedComponent.getOperations();
        for (ManagedOperation operation : operations)
        {
            String operationName = operation.getName();
            if (operationName.equals(name))
                return operation;
        }
        return null;
    }
}
