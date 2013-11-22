/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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
package org.rhq.plugins.jmx;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.management.openmbean.CompositeData;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mc4j.ems.connection.EmsConnection;
import org.mc4j.ems.connection.bean.EmsBean;
import org.mc4j.ems.connection.bean.attribute.EmsAttribute;
import org.mc4j.ems.connection.bean.operation.EmsOperation;
import org.mc4j.ems.connection.bean.parameter.EmsParameter;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.ConfigurationUpdateStatus;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.pluginapi.configuration.ConfigurationFacet;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;
import org.rhq.core.pluginapi.operation.OperationFacet;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.core.util.exception.ThrowableUtil;

/**
 * A generic JMX MBean resource component that can be used to manage a JMX MBean. The resource's plugin configuration
 * will determine what MBean is to be managed by this component.
 *
 * @author Greg Hinkle
 * @author John Mazzitelli
 */
public class MBeanResourceComponent<T extends JMXComponent<?>> implements MeasurementFacet, OperationFacet,
    ConfigurationFacet, JMXComponent<T> {

    /**
     * Subclasses are free to use this directly as a way to log messages.
     */
    protected static Log log = LogFactory.getLog(MBeanResourceComponent.class);

    public static final String OBJECT_NAME_PROP = "objectName";
    public static final String PROPERTY_TRANSFORM = "propertyTransform";

    private static final Pattern PROPERTY_PATTERN = Pattern.compile("^\\{(?:\\{([^\\}]*)\\})?([^\\}]*)\\}$");
    private static final Pattern TEMPLATE_PATTERN = Pattern.compile("%([^%]+)%");

    private static final String CALCULATED_METRIC_HEAP_USAGE_PERCENTAGE = "Calculated.HeapUsagePercentage";

    // these two should be private - subclasses need to override the getter/setter/load methods to affect these
    /**
     * @deprecated do not use this - use {@link #getEmsBean()} instead
     */
    @Deprecated
    protected EmsBean bean;

    /**
     * @deprecated do not use this - use {@link #getResourceContext()} instead
     */
    @Deprecated
    protected ResourceContext<T> resourceContext;

    /**
     * Stores the context and loads the MBean.
     * @see ResourceComponent#start(ResourceContext)
     */
    public void start(ResourceContext<T> context) {
        setResourceContext(context);
        setEmsBean(loadBean());
    }

    /**
     * Cleans the old resource context and the old MBean.
     * @see ResourceComponent#stop()
     */
    public void stop() {
        setResourceContext(null);
        setEmsBean(null);
    }

    /**
     * Gets the loaded MBean. This will attempt to {@link #loadBean() load} the bean if it
     * is not yet loaded. This might still return <code>null</code> if the MBean could
     * not be loaded.
     *
     * @return the loaded MBean
     * @throws IllegalStateException if it could not be loaded
     *
     * @see #loadBean()
     */
    public EmsBean getEmsBean() {
        // make sure the connection used to cache the bean is still the current connection. if not, re-cache the bean
        EmsConnection beanConn = (null != this.bean) ? this.bean.getConnectionProvider().getExistingConnection() : null;
        EmsConnection currConn = (null != this.bean) ? getEmsConnection() : null;

        if ((null == this.bean) || !beanConn.equals(currConn)) {
            this.bean = loadBean();
            if (null == this.bean)
                throw new IllegalStateException("EMS bean was null for Resource with type ["
                    + this.resourceContext.getResourceType() + "] and key [" + this.resourceContext.getResourceKey()
                    + "].");
        }

        return this.bean;
    }

    /**
     * Sets the MBean that this component considers loaded.
     *
     * @param bean the new MBean representing the component resource
     */
    protected void setEmsBean(EmsBean bean) {
        this.bean = bean;
    }

    public ResourceContext<T> getResourceContext() {
        return this.resourceContext;
    }

    protected void setResourceContext(ResourceContext<T> resourceContext) {
        this.resourceContext = resourceContext;
    }

    /**
     * Loads the MBean in a default way. This default mechanism is to look in the
     * plugin configuration for a key of {@link #OBJECT_NAME_PROP} and uses that
     * as the object name to load via {@link #loadBean(String)}.
     *
     * Subclasses are free to override this method in order to provide its own
     * default loading mechanism.
     *
     * @return the bean that is loaded
     */
    protected EmsBean loadBean() {
        Configuration pluginConfig = this.resourceContext.getPluginConfiguration();
        String objectName = pluginConfig.getSimple(OBJECT_NAME_PROP).getStringValue();
        EmsBean loadedBean = loadBean(objectName);
        return loadedBean;
    }

    /**
     * Loads the bean with the given object name.
     *
     * Subclasses are free to override this method in order to load the bean.
     *
     * @param objectName the name of the bean to load
     * @return the bean that is loaded
     */
    protected EmsBean loadBean(String objectName) {
        EmsConnection emsConnection = getEmsConnection();

        if (emsConnection != null) {
            EmsBean bean = emsConnection.getBean(objectName);
            if (bean == null) {
                // In some cases, this resource component may have been discovered by some means other than querying its
                // parent's EMSConnection (e.g. ApplicationDiscoveryComponent uses a filesystem to discover EARs and
                // WARs that are not yet deployed). In such cases, getBean() will return null, since EMS won't have the
                // bean in its cache. To cover such cases, make an attempt to query the underlying MBeanServer for the
                // bean before giving up.
                emsConnection.queryBeans(objectName);
                bean = emsConnection.getBean(objectName);
            }

            return bean;
        }

        return null;
    }

    /**
     * Is this service alive?
     *
     * @return true if the service is running
     */
    public AvailabilityType getAvailability() {
        try {
            return isMBeanAvailable() ? AvailabilityType.UP : AvailabilityType.DOWN;

        } catch (RuntimeException e) {
            if (this.bean != null) {
                // Retry by connecting to a new parent connection (this bean might have been connected to by an old
                // provider that's since been recreated).
                this.bean = null;
                try {
                    return isMBeanAvailable() ? AvailabilityType.UP : AvailabilityType.DOWN;

                } catch (RuntimeException e2) {
                    if (log.isDebugEnabled() ) {
                        log.debug("Avail check retry failed, MBean not available", e2);
                    }
                    return AvailabilityType.DOWN;
                }
            } else {
                if (log.isDebugEnabled() ) {
                    log.debug("Avail check failed, MBean not available", e);
                }
                return AvailabilityType.DOWN;
            }
        }
    }

    private boolean isMBeanAvailable() {
        EmsBean emsBean = getEmsBean();
        boolean isAvailable = emsBean.isRegistered();
        if (isAvailable == false) {
            // in some buggy situations, a remote server might tell us an MBean isn't registered but it really is.
            // see JBPAPP-2031 for more
            String emsBeanName = emsBean.getBeanName().getCanonicalName();
            int size = emsBean.getConnectionProvider().getExistingConnection().queryBeans(emsBeanName).size();
            isAvailable = (size == 1);
        }
        return isAvailable;
    }

    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> requests) {
        this.getValues(report, requests, getEmsBean());
    }

    /**
     * Can be called from sub-classes to collect metrics on a bean other than the default bean for this resource
     * Supports {a.b} syntax for reading the b Java Bean property from the object value returned from the a jmx
     * property. For example,
     *
     * @param report
     * @param requests
     * @param bean     the EmsBean on which to collect the metrics
     */
    protected void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> requests, EmsBean bean) {
        // First we split the requests into their respective beans, and handle calculated values
        Set<MeasurementScheduleRequest> defaultBeanRequests = new HashSet<MeasurementScheduleRequest>();
        Map<String, Set<MeasurementScheduleRequest>> beansMap = new HashMap<String, Set<MeasurementScheduleRequest>>();
        for (MeasurementScheduleRequest request : requests) {
            if (getCalculatedProperty(report, request, bean)) {
                continue;
            }
            Matcher m = PROPERTY_PATTERN.matcher(request.getName());
            if (m.matches() && (m.group(1) != null)) {
                // Custom bean
                Set<MeasurementScheduleRequest> props = beansMap.get(m.group(1));
                if (props == null) {
                    props = new HashSet<MeasurementScheduleRequest>();
                    beansMap.put(m.group(1), props);
                }

                props.add(request);
            } else {
                defaultBeanRequests.add(request);
            }
        }

        // First do the default properties against this component's main bean
        getBeanProperties(report, bean, defaultBeanRequests);

        for (String beanNameTemplate : beansMap.keySet()) {
            String transformedbeanName = transformBeanName(beanNameTemplate);
            EmsBean otherBean = getEmsConnection().getBean(transformedbeanName);
            if (otherBean == null) {
                log.info("Unable to retrieve associated MBean: " + transformedbeanName);
            } else {
                getBeanProperties(report, otherBean, beansMap.get(beanNameTemplate));
            }
        }
    }

    private boolean getCalculatedProperty(MeasurementReport report, MeasurementScheduleRequest request, EmsBean bean) {
        boolean result = false;
        String metricName = request.getName();

        if (CALCULATED_METRIC_HEAP_USAGE_PERCENTAGE.equals(request.getName())) {
            result = true;

            MeasurementReport calculationPropsReport = new MeasurementReport();
            Set<MeasurementScheduleRequest> calculationPropsSchedules = new HashSet(2);

            MeasurementScheduleRequest heapUsedRequest = new MeasurementScheduleRequest(0, "{HeapMemoryUsage.used}",
                0L, true, DataType.MEASUREMENT);
            MeasurementScheduleRequest heapComittedRequest = new MeasurementScheduleRequest(0,
                "{HeapMemoryUsage.committed}", 0L, true, DataType.MEASUREMENT);
            calculationPropsSchedules.add(heapUsedRequest);
            calculationPropsSchedules.add(heapComittedRequest);

            getBeanProperties(calculationPropsReport, bean, calculationPropsSchedules);
            Set<MeasurementDataNumeric> values = calculationPropsReport.getNumericData();
            Double heapUsed = Double.NaN;
            Double heapCommitted = Double.NaN;
            if (null != values && values.size() == 2) {
                for (MeasurementDataNumeric v : values) {
                    if (v.getName().equals("{HeapMemoryUsage.used}")) {
                        heapUsed = v.getValue();
                    } else {
                        heapCommitted = v.getValue();
                    }
                }
            }

            Double value = Double.NaN;
            try {
                value = heapUsed / heapCommitted;
            } catch (Throwable t) {
                // leave as NaN
            }

            report.addData(new MeasurementDataNumeric(request, value));
        }

        return result;
    }

    protected String transformBeanName(String beanTemplate) {
        Matcher m = TEMPLATE_PATTERN.matcher(beanTemplate);

        while (m.find()) {
            String propName = m.group(1);

            String replacementValue = resourceContext.getPluginConfiguration().getSimpleValue(propName, null);
            beanTemplate = beanTemplate.replaceAll("%" + propName + "%", replacementValue);

            m = TEMPLATE_PATTERN.matcher(beanTemplate);
        }

        return beanTemplate;
    }

    protected void getBeanProperties(MeasurementReport report, EmsBean thisBean,
        Set<MeasurementScheduleRequest> requests) {
        List<String> props = new ArrayList<String>();
        for (MeasurementScheduleRequest request : requests) {
            Matcher m = PROPERTY_PATTERN.matcher(request.getName());
            if (m.matches()) {
                // Complex property
                props.add(getAttributeName(m.group(2)));
            } else {
                // Simple property
                props.add(request.getName());
            }
        }

        List<EmsAttribute> refreshedAttributes = thisBean.refreshAttributes(props);

        for (MeasurementScheduleRequest request : requests) {
            Matcher m = PROPERTY_PATTERN.matcher(request.getName());
            String fullProperty = null;
            String attributeName;
            if (m.matches()) {
                // Complex property
                fullProperty = m.group(2);
                attributeName = getAttributeName(fullProperty);
            } else {
                attributeName = request.getName();
            }

            EmsAttribute attribute = null;
            for (EmsAttribute refreshedAttribute : refreshedAttributes) {
                if (attributeName.equals(refreshedAttribute.getName())) {
                    attribute = refreshedAttribute;
                }
            }

            if (attribute == null) {
                log.debug("Unable to collect measurement, attribute [" + request.getName() + "] not found on ["
                    + this.resourceContext.getResourceKey() + "]");
                // TODO GH: report.addError
            } else {
                Object value = attribute.getValue();
                if ((value != null) && (fullProperty != null)) {
                    // we're meant to load a specific property of the returned value object
                    value = lookupAttributeProperty(value, fullProperty);
                }

                if (request.getDataType() == DataType.MEASUREMENT) {
                    if (value instanceof Number) {
                        report.addData(new MeasurementDataNumeric(request, ((Number) value).doubleValue()));
                    } else if ((value instanceof List<?>)) {
                        // add the number of elements
                        report.addData(new MeasurementDataNumeric(request, Double.valueOf(((List<?>) value).size())));
                    }
                } else if (request.getDataType() == DataType.TRAIT) {
                    String displayValue = null;
                    if ((value != null) && value.getClass().isArray()) {
                        displayValue = Arrays.deepToString((Object[]) value);
                    } else {
                        displayValue = String.valueOf(value);
                    }

                    report.addData(new MeasurementDataTrait(request, displayValue));
                }
            }
        }
    }

    protected Object lookupAttributeProperty(Object value, String property) {
        String[] ps = property.split("\\.", 2);

        String searchProperty = ps[0];

        // Values returned from EMS connections may be from JMX classes loaded in separate classloaders (for server compatibility)
        // so we use reflection to be able to handle any instance of the CompositeData class.
        Class[] interfaces = value.getClass().getInterfaces();
        boolean isCompositeData = false;
        for (Class intf : interfaces) {
            if (intf.getName().equals(CompositeData.class.getName())) {
                isCompositeData = true;
            }
        }

        if (value.getClass().getName().equals(CompositeData.class.getName()) || isCompositeData) {
            try {
                Method m = value.getClass().getMethod("get", String.class);
                value = m.invoke(value, ps[1]);
            } catch (NoSuchMethodException e) {
                /* Won't happen */
            } catch (Exception e) {
                log.info("Unable to read attribute property [" + property + "] from composite data value", e);
            }
        } else {
            // Try to use reflection
            try {
                PropertyDescriptor[] pds = Introspector.getBeanInfo(value.getClass()).getPropertyDescriptors();
                for (PropertyDescriptor pd : pds) {
                    if (pd.getName().equals(searchProperty)) {
                        value = pd.getReadMethod().invoke(value);
                    }
                }
            } catch (Exception e) {
                log.debug("Unable to read property from measurement attribute [" + searchProperty + "] not found on ["
                    + this.resourceContext.getResourceKey() + "]");
            }
        }

        if (ps.length > 1) {
            value = lookupAttributeProperty(value, ps[1]);
        }

        return value;
    }

    protected String getAttributeName(String property) {
        return property.split("\\.", 2)[0];
    }

    protected String getAttributeProperty(String property) {
        if (property.startsWith("{")) {
            return property.substring(property.indexOf('.') + 1, property.length() - 1);
        } else {
            return null;
        }
    }

    /**
     * This default setup of configuration properties can map to mbean attributes
     *
     * @return the configuration of the component
     */
    public Configuration loadResourceConfiguration() {
        Configuration configuration = new Configuration();
        ConfigurationDefinition configurationDefinition = this.resourceContext.getResourceType()
            .getResourceConfigurationDefinition();

        for (PropertyDefinition property : configurationDefinition.getPropertyDefinitions().values()) {
            if (property instanceof PropertyDefinitionSimple) {
                EmsAttribute attribute = getEmsBean().getAttribute(property.getName());
                if (attribute != null) {
                    configuration.put(new PropertySimple(property.getName(), attribute.refresh()));
                }
            }
        }

        return configuration;
    }

    /**
     * Equivalent to updateResourceConfiguration(report, false);
     */
    public void updateResourceConfiguration(ConfigurationUpdateReport report) {
        updateResourceConfiguration(report, false);
    }

    public void updateResourceConfiguration(ConfigurationUpdateReport report, boolean ignoreReadOnly) {
        ConfigurationDefinition configurationDefinition = this.getResourceContext().getResourceType()
            .getResourceConfigurationDefinition();

        // assume we succeed - we'll set to failure if we can't set all properties
        report.setStatus(ConfigurationUpdateStatus.SUCCESS);

        for (String key : report.getConfiguration().getSimpleProperties().keySet()) {
            PropertySimple property = report.getConfiguration().getSimple(key);
            if (property != null) {
                EmsAttribute attribute = this.bean.getAttribute(key);
                try {
                    PropertyDefinitionSimple def = configurationDefinition.getPropertyDefinitionSimple(property
                        .getName());
                    if (!(ignoreReadOnly && def.isReadOnly())) {
                        switch (def.getType()) {
                        case INTEGER: {
                            attribute.setValue(property.getIntegerValue());
                            break;
                        }

                        case LONG: {
                            attribute.setValue(property.getLongValue());
                            break;
                        }

                        case BOOLEAN: {
                            attribute.setValue(property.getBooleanValue());
                            break;
                        }

                        case FLOAT: {
                            attribute.setValue(property.getFloatValue());
                            break;
                        }

                        case DOUBLE: {
                            attribute.setValue(property.getDoubleValue());
                            break;
                        }

                        default: {
                            attribute.setValue(property.getStringValue());
                            break;
                        }
                        }
                    }
                } catch (Exception e) {
                    property.setErrorMessage(ThrowableUtil.getStackAsString(e));
                    report
                        .setErrorMessage("Failed setting resource configuration - see property error messages for details");
                    log.info("Failure setting MBean Resource configuration value for " + key, e);
                }
            }
        }
    }

    public EmsConnection getEmsConnection() {
        return this.resourceContext.getParentResourceComponent().getEmsConnection();
    }

    public OperationResult invokeOperation(String name, Configuration parameters) throws Exception {
        return invokeOperation(name, parameters, getEmsBean());
    }

    public OperationResult invokeOperation(String name, Configuration parameters, EmsBean emsBean) throws Exception {
        if (emsBean == null) {
            throw new Exception("Can not invoke operation [" + name
                + "], as we can't connect to the MBean - is it down?");
        }

        Map<String, PropertySimple> paramProps = parameters.getSimpleProperties();
        SortedSet<EmsOperation> emsOperations = emsBean.getOperations();
        EmsOperation operation = null;
        // There could be multiple operations with the same name but different parameters. Try to find one that has
        // the same # of parameters as the RHQ operation def.
        for (EmsOperation emsOperation : emsOperations) {
            if (emsOperation.getName().equals(name) && (emsOperation.getParameters().size() == paramProps.size())) {
                operation = emsOperation;
                break;
            }
        }
        if (operation == null) {
            // We couldn't find an operation with the expected name and # of parameters, so as as a last ditch effort,
            // see if there's an operation that at least has the expected name.
            operation = emsBean.getOperation(name);
        }
        if (operation == null) {
            throw new Exception("Operation [" + name + "] not found on MBean [" + emsBean.getBeanName() + "].");
        }

        List<EmsParameter> emsParams = operation.getParameters();
        Map<String, Integer> emsParamIndexesByName = new HashMap<String, Integer>();
        for (int i = 0, emsParamsSize = emsParams.size(); i < emsParamsSize; i++) {
            EmsParameter emsParam = emsParams.get(i);
            if (emsParam.getName() != null) {
                emsParamIndexesByName.put(emsParam.getName(), i);
            }
        }

        Object[] paramValues = new Object[operation.getParameters().size()];

        for (String propName : paramProps.keySet()) {
            Integer paramIndex;
            if (propName.matches("\\[\\d+\\]")) {
                paramIndex = Integer.valueOf(propName.substring(propName.indexOf('[') + 1, propName.indexOf(']')));
                if (paramIndex < 0 || paramIndex >= emsParams.size()) {
                    throw new IllegalStateException("Index [" + paramIndex + "] specified for parameter of operation ["
                        + name + "] on MBean [" + emsBean.getBeanName() + "] is invalid. The MBean operation takes "
                        + emsParams.size() + " parameters.");
                }
            } else {
                paramIndex = emsParamIndexesByName.get(propName);
                if (paramIndex == null) {
                    throw new IllegalStateException("Name [" + propName + "] specified for parameter of operation ["
                        + name + "] on MBean [" + emsBean.getBeanName()
                        + "] is invalid. The MBean operation does not take a parameter by that name.");
                }
            }
            EmsParameter emsParam = emsParams.get(paramIndex);

            PropertySimple paramProp = paramProps.get(propName);
            String emsParamType = emsParam.getType();
            Object paramValue = getPropertyValueAsType(paramProp, emsParamType);
            paramValues[paramIndex] = paramValue;
        }

        Object resultObject = operation.invoke(paramValues);

        boolean hasVoidReturnType = (operation.getReturnType() == null
            || Void.class.getName().equals(operation.getReturnType()) || void.class.getName().equals(
            operation.getReturnType()));

        OperationResult resultToReturn;
        if (resultObject == null && hasVoidReturnType) {
            resultToReturn = null;
        } else {
            // if the returned object is an array, put the elements in a list so we can stringify it later
            if (resultObject != null && resultObject.getClass().isArray()) {
                int len = Array.getLength(resultObject);
                ArrayList<Object> list = new ArrayList<Object>(len);
                for (int index = 0; index < len; index++) {
                    list.add(Array.get(resultObject, index));
                }
                resultObject = list;
            }

            // put the results object in an operation result if it isn't already one
            if (resultObject instanceof OperationResult) {
                resultToReturn = (OperationResult) resultObject;
            } else {
                resultToReturn = new OperationResult(String.valueOf(resultObject));
            }
        }

        return resultToReturn;
    }

    protected Object getPropertyValueAsType(PropertySimple propSimple, String typeName) {
        Object value;
        if (typeName.equals(String.class.getName())) {
            value = (propSimple == null) ? null : propSimple.getStringValue();
        } else if (typeName.equals(Boolean.class.getName()) || typeName.equals(boolean.class.getName())) {
            value = (propSimple == null) ? null : propSimple.getBooleanValue();
        } else if (typeName.equals(Integer.class.getName()) || typeName.equals(int.class.getName())) {
            value = (propSimple == null) ? null : propSimple.getIntegerValue();
        } else if (typeName.equals(Long.class.getName()) || typeName.equals(long.class.getName())) {
            value = (propSimple == null) ? null : propSimple.getLongValue();
        } else if (typeName.equals(Float.class.getName()) || typeName.equals(float.class.getName())) {
            value = (propSimple == null) ? null : propSimple.getFloatValue();
        } else if (typeName.equals(Double.class.getName()) || typeName.equals(double.class.getName())) {
            value = (propSimple == null) ? null : propSimple.getDoubleValue();
        } else {
            throw new IllegalStateException("Operation parameter maps to MBean parameter with an unsupported type ("
                + typeName + ").");
        }
        // TODO GH: Handle rest of types. (I think i have a mapper for this in mc4j
        return value;
    }
}
