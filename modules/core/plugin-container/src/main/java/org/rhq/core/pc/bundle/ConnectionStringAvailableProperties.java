package org.rhq.core.pc.bundle;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.NotNull;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.pc.inventory.InventoryManager;
import org.rhq.core.pc.inventory.ResourceContainer;
import org.rhq.core.pc.measurement.MeasurementManager;

/**
 * A map of variables available in the "connection" string of a destination definition.
 *
 * Note that this caches the resource and plugin configuration as well as the list of enabled traits of the resource
 * so instances of this class should be short-lived.
 *
 * @author Lukas Krejci
 * @since 4.12
 */
final class ConnectionStringAvailableProperties extends AbstractMap<String, String> {

    private final ResourceContainer resourceContainer;
    private final Map<String, Property> pluginConfiguration;
    private final Map<String, Property> resourceConfiguration;
    private final Map<String, String> traits;
    private final Map<String, Property> deploymentConfiguration;
    private final MeasurementManager measurementManager;

    ConnectionStringAvailableProperties(ResourceContainer container, MeasurementManager measurementManager,
        Configuration deploymentConfiguration) {

        this.resourceContainer = container;
        this.measurementManager = measurementManager;
        this.pluginConfiguration = new HashMap<String, Property>();
        for (Property p : container.getResource().getPluginConfiguration().getProperties()) {
            pluginConfiguration.put("pluginConfiguration." + p.getName(), p);
        }

        this.resourceConfiguration = new HashMap<String, Property>();
        for (Property p : InventoryManager.getResourceConfiguration(container.getResource())
            .getProperties()) {

            resourceConfiguration.put("resourceConfiguration." + p.getName(), p);
        }

        this.traits = new HashMap<String, String>();

        for (MeasurementScheduleRequest r : container.getMeasurementSchedule()) {
            if (r.getDataType() == DataType.TRAIT) {
                traits.put("measurementTrait." + r.getName(), r.getName());
            }
        }

        this.deploymentConfiguration = new HashMap<String, Property>();
        for (Property p : deploymentConfiguration.getProperties()) {
            this.deploymentConfiguration.put("deploymentConfiguration." + p.getName(), p);
        }
    }

    @NotNull
    @Override
    public Set<Entry<String, String>> entrySet() {
        return new AbstractSet<Entry<String, String>>() {

            @NotNull
            @Override
            public Iterator<Entry<String, String>> iterator() {
                return new Iterator<Entry<String, String>>() {
                    Map<String, ?> currentMap = pluginConfiguration;
                    Iterator<? extends Map.Entry<String, ?>> currentIt = currentMap.entrySet().iterator();

                    @Override
                    public boolean hasNext() {
                        skipToNextMapIfNeeded();
                        return currentIt.hasNext();
                    }

                    @Override
                    public Entry<String, String> next() {
                        skipToNextMapIfNeeded();
                        final Entry<String, ?> entry = currentIt.next();
                        return new Entry<String, String>() {

                            @Override
                            public String getKey() {
                                return entry.getKey();
                            }

                            @Override
                            public String getValue() {
                                Object v = entry.getValue();

                                if (v == null) {
                                    return null;
                                } else if (v instanceof String) {
                                    //traits here...
                                    return measurementManager.getTraitValue(resourceContainer, (String) v);
                                } else if (v instanceof Property) {
                                    if (v instanceof PropertySimple) {
                                        return ((PropertySimple) v).getStringValue();
                                    } else {
                                        // I think it is more appropriate to just return null here instead of bombing
                                        // out...
                                        return null;
                                        //throw new IllegalArgumentException("Key '" + key +
                                        //    "' matches a non-simple property in the resource configuration. Cannot" +
                                        //    " get its string value.");
                                    }
                                } else {
                                    throw new AssertionError(
                                        "ResourceBackedValues instance contains a value of unexpected type: " +
                                            v.getClass());
                                }
                            }

                            @Override
                            public String setValue(String value) {
                                throw new UnsupportedOperationException();
                            }
                        };
                    }

                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException();
                    }

                    private void skipToNextMapIfNeeded() {
                        if (!currentIt.hasNext() && currentMap == pluginConfiguration) {
                            currentMap = resourceConfiguration;
                            currentIt = resourceConfiguration.entrySet().iterator();
                        }

                        if (!currentIt.hasNext() && currentMap == resourceConfiguration) {
                            currentMap = traits;
                            currentIt = traits.entrySet().iterator();
                        }

                        if (!currentIt.hasNext() && currentMap == traits) {
                            currentMap = deploymentConfiguration;
                            currentIt = deploymentConfiguration.entrySet().iterator();
                        }
                    }
                };
            }

            @Override
            public int size() {
                return pluginConfiguration.size() + resourceConfiguration.size() + traits.size() +
                    deploymentConfiguration.size();
            }
        };
    }
}
