/*
 * RHQ Management Platform
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

/**
 * A generic JMX MBean resource component that can be used to manage a JMX MBean. The resource's plugin configuration
 * will determine what MBean is to be managed by this component.
 *
 * @author Greg Hinkle
 * @author John Mazzitelli
 */
public class MBeanResourceComponent<T extends JMXComponent> implements MeasurementFacet, OperationFacet,
    ConfigurationFacet, JMXComponent<T> {

    /**
     * Subclasses are free to use this directly as a way to log messages.
     */
    protected static Log log = LogFactory.getLog(MBeanResourceComponent.class);

    public static final String OBJECT_NAME_PROP = "objectName";
    public static final String PROPERTY_TRANSFORM = "propertyTransform";

    private static final Pattern PROPERTY_PATTERN = Pattern.compile("^\\{(?:\\{([^\\}]*)\\})?([^\\}]*)\\}$");
    private static final Pattern TEMPLATE_PATTERN = Pattern.compile("%([^%]+)%");

    // these two should be private - subclasses need to override the getter/setter/load methods to affect these
    /**
     * @deprecated do not use this - use {@link #getEmsBean()} instead
     */
    protected EmsBean bean;

    /**
     * @deprecated do not use this - use {@link #getResourceContext()} instead
     */
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
     * @return the loaded MBean or <code>null</code> if it could not be loaded
     * 
     * @see #loadBean()
     */
    public EmsBean getEmsBean() {
        if (this.bean == null) {
            this.bean = loadBean();
            if (this.bean == null)
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

    /**
     * Is this service alive?
     *
     * @return true if the service is running
     */
    public AvailabilityType getAvailability() {
        try {
            if (isMBeanAvailable()) {
                return AvailabilityType.UP;
            } else {
                return AvailabilityType.DOWN;
            }
        } catch (RuntimeException e) {
            if (this.bean != null) {
                // Retry by connecting to a new parent connection (this bean might have been connected to by an old
                // provider that's since been recreated).
                this.bean = null;
                if (isMBeanAvailable()) {
                    return AvailabilityType.UP;
                } else {
                    return AvailabilityType.DOWN;
                }
            } else {
                throw e;
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
        // First we split the requests into their respective beans
        Set<MeasurementScheduleRequest> defaultBeanRequests = new HashSet<MeasurementScheduleRequest>();
        Map<String, Set<MeasurementScheduleRequest>> beansMap = new HashMap<String, Set<MeasurementScheduleRequest>>();
        for (MeasurementScheduleRequest request : requests) {
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

                if ((request.getDataType() == DataType.MEASUREMENT) && (value instanceof Number)) {
                    report.addData(new MeasurementDataNumeric(request, ((Number) value).doubleValue()));
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

    public void updateResourceConfiguration(ConfigurationUpdateReport report) {
        ConfigurationDefinition configurationDefinition = this.resourceContext.getResourceType()
            .getResourceConfigurationDefinition();

        // assume we succeed - we'll set to failure if we can't set all properties
        report.setStatus(ConfigurationUpdateStatus.SUCCESS);

        for (String key : report.getConfiguration().getSimpleProperties().keySet()) {
            PropertySimple property = report.getConfiguration().getSimple(key);
            if (property != null) {
                EmsAttribute attribute = this.bean.getAttribute(key);
                try {
                    switch (configurationDefinition.getPropertyDefinitionSimple(property.getName()).getType()) {
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
                } catch (Exception e) {
                    property.setErrorMessageFromThrowable(e);
                    report
                        .setErrorMessage("Failed setting resource configuration - see property error messages for details");
                    log.info("Failure setting MBean Resource configuration value", e);
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
                + "], as we can't connect to the bean - is it down?");
        }

        EmsOperation operation = emsBean.getOperation(name);
        if (operation == null) {
            throw new Exception("Operation [" + name + "] not found on bean [" + emsBean.getBeanName() + "]");
        }

        Object[] parameterValues = new Object[operation.getParameters().size()];

        int i = 0;
        for (EmsParameter param : operation.getParameters()) {
            PropertySimple ps = parameters.getSimple(param.getName());
            if (param.getType().equals(String.class.getName())) {
                parameterValues[i] = (ps == null) ? null : ps.getStringValue();
            } else if (param.getType().equals(boolean.class.getName())) {
                parameterValues[i] = (ps == null) ? null : ps.getBooleanValue();
            }

            // TODO GH: Handle rest of types. (I think i have a mapper for this in mc4j
            i++;
        }

        Object resultObject = operation.invoke(parameterValues);

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

            // put the results object in an operation result
            resultToReturn = new OperationResult(String.valueOf(resultObject));
        }

        return resultToReturn;
    }
}