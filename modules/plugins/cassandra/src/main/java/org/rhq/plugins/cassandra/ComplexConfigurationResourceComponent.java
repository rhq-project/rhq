/*
 * RHQ Management Platform
 * Copyright 2011, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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

package org.rhq.plugins.cassandra;

import java.net.InetAddress;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.cassandra.net.MessagingService;
import org.mc4j.ems.connection.bean.attribute.EmsAttribute;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionList;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionMap;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.plugins.jmx.JMXComponent;
import org.rhq.plugins.jmx.MBeanResourceComponent;

/**
 * @author Stefan Negrea, Libor Zoubek
 *
 */
public class ComplexConfigurationResourceComponent extends MBeanResourceComponent<JMXComponent<?>> {


    /**
     * finds Cassandra node component
     * @return Cassandra component or null if it could not be found
     */
    protected CassandraNodeComponent getCassandraComponent() {
        JMXComponent<?> component = this;
        while ((component != null) && !(component instanceof CassandraNodeComponent)) {
            if (component instanceof MBeanResourceComponent<?>) {
                component = ((MBeanResourceComponent<?>) component).getResourceContext().getParentResourceComponent();
            } else {
                return null;
            }
        }
        return (CassandraNodeComponent) component;
    }

    /**
     * reads MBean attribute  value (result type of Map<String,String> is expected) as operation result. Parameters 'keyName' and 'valueName'
     * denote names of SimpleProperties within produced OperationResult should by same as defined in plugin descriptor
     * @param name operation name = attributeName to read
     * @param keyName name of key to be written to result map
     * @param valueName name of value  to be written to result map 
     * @return Operation result with complex result (List of Map<name,value>)
     * @see MessagingService or StorageService plugin descriptor
     */
    protected OperationResult invokeReadAttributeOperationComplexResult(String name, String keyName, String valueName) {
        OperationResult result = new OperationResult();
        EmsAttribute attribute = getEmsBean().getAttribute(name);
        Object valueObject = attribute.refresh();
        PropertyList resultList = new PropertyList("operationResult");
        result.getComplexResults().put(resultList);
        if (valueObject instanceof Map<?, ?>) {
            @SuppressWarnings("unchecked")
            Map<String, String> map = (Map<String, String>) valueObject;
            for (Map.Entry<String, String> entry : map.entrySet()) {
                PropertyMap entryMap = new PropertyMap("entry");
                resultList.add(entryMap);
                entryMap.put(new PropertySimple(keyName, entry.getKey()));
                entryMap.put(new PropertySimple(valueName, entry.getValue()));
            }
        } else {
            result.setErrorMessage("Failed to read response");
        }
        return result;
    }

    @Override
    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> requests) {
        // handle special metrics starting with "host:"
        // such MBeans are expected to return Map<Host,Value> and we'll have to find value of current host (current node)
        Set<MeasurementScheduleRequest> filtered = new HashSet<MeasurementScheduleRequest>(requests.size());
        for (MeasurementScheduleRequest request : requests) {
            if (!readHostMetric(request, report)) {
                filtered.add(request);
            }
        }
        super.getValues(report, filtered);
    }

    /**
     * reads host-related metric value. It is expected, that metric specified in `request` is prefixed
     * by "host:" in it's name and, returned MBean attribute value type is Map<Host,Value>. This method 
     * finds current {@link CassandraNodeComponent#getHost()} in map and adds it's value into report
     * @param request
     * @param report
     * @return true if given request name was prefixed with "host:", false otherwise
     */
    private boolean readHostMetric(MeasurementScheduleRequest request, MeasurementReport report) {
        if (!request.getName().startsWith("host:")) {
            return false;
        }
        CassandraNodeComponent cassandra = getCassandraComponent();
        if (cassandra == null || cassandra.getHostAddress() == null) {
            return true;
        }
        InetAddress host = cassandra.getHostAddress();
        String metricName = request.getName().substring(5); // strip out "host:"
        EmsAttribute attribute = getEmsBean().getAttribute(metricName);
        Object valueObject = attribute.refresh();
        if (valueObject instanceof Map<?, ?>) {

            Float value = null;
            // Could be hostname also
            Iterator iter = ((Map) valueObject).keySet().iterator();
            if(iter.hasNext()) {
                Object firstKey = iter.next();
                if(firstKey instanceof String) {
                    // Hostname parsing
                    @SuppressWarnings("unchecked")
                    Map<String, Float> hostnameMetric = (Map<String, Float>) valueObject;
                    value = hostnameMetric.get(host.getHostAddress());
                    if (value == null) {
                        // the inet address wasn't probably resolved, scan the map
                        for (Map.Entry<String, Float> entry : hostnameMetric.entrySet()) {
                            if (entry.getKey().equals(host.getHostAddress())) {
                                value = entry.getValue();
                                break;
                            }
                        }
                    }
                } else {
                    @SuppressWarnings("unchecked")
                    Map<InetAddress, Float> hostMetric = (Map<InetAddress, Float>) valueObject;
                    value = hostMetric.get(host);
                    if (value == null) {
                        // the inet address wasn't probably resolved, scan the map
                        for (Map.Entry<InetAddress, Float> entry : hostMetric.entrySet()) {
                            if (entry.getKey().getHostAddress().equals(host.getHostAddress())) {
                                value = entry.getValue();
                                break;
                            }
                        }
                    }
                }
            }
            if (value != null) {
                report.addData(new MeasurementDataNumeric(request, value.doubleValue()));
            }

        }
        return true;
    }

    @SuppressWarnings({ "unchecked", "deprecation" })
    @Override
    public Configuration loadResourceConfiguration() {
        Configuration configuration =  super.loadResourceConfiguration();

        ConfigurationDefinition resourceConfigurationDefinition = this.resourceContext.getResourceType()
            .getResourceConfigurationDefinition();

        for (PropertyDefinition propertyDefinition : resourceConfigurationDefinition.getPropertyDefinitions().values()) {
            if (propertyDefinition instanceof PropertyDefinitionList) {

                EmsAttribute attribute = getEmsBean().getAttribute(propertyDefinition.getName());

                if (attribute != null) {
                    Object result = attribute.refresh();

                    PropertyList propertyList = new PropertyList(propertyDefinition.getName());

                    if (result instanceof Map) {
                        PropertyDefinitionMap propertyDefinitionMap = (PropertyDefinitionMap) ((PropertyDefinitionList) propertyDefinition)
                            .getMemberDefinition();

                        String mapName = propertyDefinitionMap.getName();

                        Collection<PropertyDefinition> subPropertyDefinitions = propertyDefinitionMap
                            .getOrderedPropertyDefinitions();
                        Iterator<PropertyDefinition> iterator = subPropertyDefinitions.iterator();
                        String keyName = ((PropertyDefinitionSimple) iterator.next()).getName();
                        String valueName = ((PropertyDefinitionSimple) iterator.next()).getName();

                        Map<Object, Object> mapValue = (Map<Object, Object>) result;

                        PropertyMap propertyMap;
                        for (Entry<Object, Object> entry : mapValue.entrySet()) {
                            propertyMap = new PropertyMap(mapName);
                            propertyMap.put(new PropertySimple(keyName, entry.getKey().toString()));
                            propertyMap.put(new PropertySimple(valueName, entry.getValue().toString()));

                            propertyList.add(propertyMap);
                        }
                    } else if (result instanceof Iterable<?>) {
                        String entryName = ((PropertyDefinitionSimple) ((PropertyDefinitionList) propertyDefinition)
                            .getMemberDefinition()).getName();

                        Iterable<?> iterable = (Iterable<?>) result;
                        for (Object entry : iterable) {
                            propertyList.add(new PropertySimple(entryName, entry.toString()));
                        }
                    } else if (result instanceof Object[]) {
                        String entryName = ((PropertyDefinitionSimple) ((PropertyDefinitionList) propertyDefinition)
                            .getMemberDefinition()).getName();

                        Object[] arrayValue = (Object[]) result;

                        for (Object entry : arrayValue) {
                            propertyList.add(new PropertySimple(entryName, entry.toString()));
                        }
                    }

                    if (propertyList.getList().size() != 0) {
                        configuration.put(propertyList);
                    }
                }
            }
        }

        return configuration;
    }

    @Override
    public void updateResourceConfiguration(ConfigurationUpdateReport report) {
        // don't try to update the read only properties, it will fail
        super.updateResourceConfiguration(report, true);
    }
}
