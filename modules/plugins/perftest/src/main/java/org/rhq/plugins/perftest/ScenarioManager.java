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
package org.rhq.plugins.perftest;

import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.util.ValidationEventCollector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.plugins.perftest.calltime.CalltimeFactory;
import org.rhq.plugins.perftest.calltime.ConfigurableCallTimeDataFactory;
import org.rhq.plugins.perftest.calltime.EmptyCalltimeFactory;
import org.rhq.plugins.perftest.calltime.SimpleCallTimeDataFactory;
import org.rhq.plugins.perftest.configuration.ConfigurationFactory;
import org.rhq.plugins.perftest.configuration.SimpleConfigurationFactory;
import org.rhq.plugins.perftest.content.ContentFactory;
import org.rhq.plugins.perftest.content.SimpleContentFactory;
import org.rhq.plugins.perftest.measurement.EmptyMeasurementFactory;
import org.rhq.plugins.perftest.measurement.MeasurementFactory;
import org.rhq.plugins.perftest.measurement.OOBNumericMeasurementFactory;
import org.rhq.plugins.perftest.measurement.SimpleNumericMeasurementFactory;
import org.rhq.plugins.perftest.resource.EmptyResourceFactory;
import org.rhq.plugins.perftest.resource.ResourceFactory;
import org.rhq.plugins.perftest.resource.SimpleResourceFactory;
import org.rhq.plugins.perftest.scenario.CalltimeGenerator;
import org.rhq.plugins.perftest.scenario.ConfigurableCallTimeDataGenerator;
import org.rhq.plugins.perftest.scenario.ConfigurationGenerator;
import org.rhq.plugins.perftest.scenario.ContentGenerator;
import org.rhq.plugins.perftest.scenario.MeasurementGenerator;
import org.rhq.plugins.perftest.scenario.OOBNumericMeasurementGenerator;
import org.rhq.plugins.perftest.scenario.Resource;
import org.rhq.plugins.perftest.scenario.ResourceGenerator;
import org.rhq.plugins.perftest.scenario.Scenario;
import org.rhq.plugins.perftest.scenario.SimpleCallTimeDataGenerator;
import org.rhq.plugins.perftest.scenario.SimpleConfigurationGenerator;
import org.rhq.plugins.perftest.scenario.SimpleContentGenerator;
import org.rhq.plugins.perftest.scenario.SimpleNumericMeasurementGenerator;
import org.rhq.plugins.perftest.scenario.SimpleResourceGenerator;
import org.rhq.plugins.perftest.scenario.SimpleTraitMeasurementGenerator;
import org.rhq.plugins.perftest.scenario.TraitGenerator;
import org.rhq.plugins.perftest.trait.EmptyTraitFactory;
import org.rhq.plugins.perftest.trait.SimpleTraitFactory;
import org.rhq.plugins.perftest.trait.TraitFactory;

/**
 * Loads performance testing scenarios and parses into usable components by the RHQ resource components.
 *
 * @author Jason Dobies
 */
public class ScenarioManager {
    // Constants  --------------------------------------------

    private static final ScenarioManager INSTANCE = new ScenarioManager();

    /**
     * System property that must be set to indicate what scenario to use. The value of this property should be the file
     * name of the scenario, minus the ".xml". For example, to use the scenario defined in high-servers.xml, this
     * property should be set to "high-servers".
     */
    public static final String SCENARIO_PROPERTY = "rhq.perftest.scenario";

    /**
     * Resource factory used when a scenario doesn't define any resources of a particular type.
     */
    private static final EmptyResourceFactory EMPTY_RESOURCE_FACTORY = new EmptyResourceFactory();

    /**
     * Measurement factory used when a scenario doesn't define any metrics for a particular resource type.
     */
    private static final EmptyMeasurementFactory EMPTY_MEASUREMENT_FACTORY = new EmptyMeasurementFactory();

    /**
     * Calltime factory used when a scenario doesn't define any metrics for a particular resource type.
     */
    private static final EmptyCalltimeFactory EMPTY_CALLTIME_FACTORY = new EmptyCalltimeFactory();

    /**
     * Trait factory used when a scenario doesn't define any metrics for a particular resource type.
     */
    private static final TraitFactory EMPTY_TRAIT_FACTORY = new EmptyTraitFactory();


    // Attributes  --------------------------------------------

    private final Log log = LogFactory.getLog(ScenarioManager.class);

    /**
     * JAXB representation of the loaded scenario.
     */
    private Scenario scenario;

    /**
     * Mapping of resource type name to resource factory used in discovery.
     */
    private Map<String, ResourceFactory> resourceFactories = new HashMap<String, ResourceFactory>();

    /**
     * Mapping of resource type name to measurement factory to field all of its configured measurements. If we
     * eventually want to be able to configure the value for each metric on a resource type, this will need to be
     * changed.
     */
    private Map<String, MeasurementFactory> measurementFactories = new HashMap<String, MeasurementFactory>();
    private Map<String, CalltimeFactory> calltimeFactories = new HashMap<String, CalltimeFactory>();
    private Map<String, TraitFactory> traitFactories = new HashMap<String, TraitFactory>();

    /**
     * Mapping of resource type name to configuration factory to populate the plugin configuration for newly discovered
     * resources. If we eventually want to customize the plugin configuration for each resource of this type, this will
     * need to be changed.
     */
    private Map<String, ConfigurationFactory> pluginConfigurationFactories = new HashMap<String, ConfigurationFactory>();

    /**
     * Mapping of package type name to content factory to field requests to discover content. This implementation has
     * the limitation that each type in the plugin descriptor has a unique name.
     */
    private Map<String, ContentFactory> contentFactories = new HashMap<String, ContentFactory>();

    // Constructors  --------------------------------------------

    /**
     * Singleton constructor.
     */
    private ScenarioManager() {
        loadScenario();
    }

    // Public  --------------------------------------------

    /**
     * Returns an instance of this class.
     *
     * @return instance of this class
     */
    public static ScenarioManager getInstance() {
        return INSTANCE;
    }

    public boolean isEnabled() {
        return this.scenario != null;
    }

    /**
     * Returns the resource factory defined in the scenario for the specified resource type.
     *
     * @param  resourceTypeName indicates which resource factory to return
     *
     * @return resource factory instance to use to generate the discovery for resources of this type; this will never be
     *         <code>null</code>.
     */
    public ResourceFactory getResourceFactory(String resourceTypeName) {
        ResourceFactory resourceFactory = resourceFactories.get(resourceTypeName);

        // Lazy load the factory
        if (resourceFactory == null) {
            Resource resource = findResource(resourceTypeName);
            if (resource == null) {
                resourceFactory = EMPTY_RESOURCE_FACTORY;
            } else {
                ResourceGenerator generator = resource.getResourceGenerator().getValue();
                resourceFactory = createResourceFactory(generator);
            }

            resourceFactories.put(resourceTypeName, resourceFactory);
        }

        return resourceFactory;
    }

    /**
     * Returns the measurement factory defined in the scenario for the specified resource type.
     *
     * @param  resourceTypeName indicates the resource for which we're retrieving metrics
     *
     * @return measurement factory instance used to generate measurement values; this will never be <code>null</code>.
     */
    public MeasurementFactory getMeasurementFactory(String resourceTypeName) {
        MeasurementFactory measurementFactory = measurementFactories.get(resourceTypeName);

        // Lazy load the factory
        if (measurementFactory == null) {
            Resource resource = findResource(resourceTypeName);
            if (resource == null) {
                measurementFactory = EMPTY_MEASUREMENT_FACTORY;
            } else {
                JAXBElement<? extends MeasurementGenerator> element = resource.getMeasurementGenerator();
                if (element == null) {
                    measurementFactory = EMPTY_MEASUREMENT_FACTORY;
                } else {
                    MeasurementGenerator generator = element.getValue();
                    measurementFactory = createMeasurementFactory(generator);
                }
            }

            measurementFactories.put(resourceTypeName, measurementFactory);
        }

        return measurementFactory;
    }

    public CalltimeFactory getCalltimeFactory(String resourceTypeName) {
        CalltimeFactory calltimeFactory= calltimeFactories.get(resourceTypeName);

        if (calltimeFactory == null ) {
            Resource resource = findResource(resourceTypeName);
            if (resource == null) {
                calltimeFactory = EMPTY_CALLTIME_FACTORY;
            } else {
                JAXBElement<? extends CalltimeGenerator> element = resource.getCalltimeGenerator();
                if (element == null) {
                    calltimeFactory = EMPTY_CALLTIME_FACTORY;
                } else {
                    CalltimeGenerator generator = element.getValue();
                    calltimeFactory = createCalltimeFactory(generator);
                }
            }

            calltimeFactories.put(resourceTypeName, calltimeFactory);
        }

        return calltimeFactory;
    }

    public TraitFactory getTraitFactory(String resourceTypeName) {
        TraitFactory traitFactory = traitFactories.get(resourceTypeName);

        if (traitFactory == null) {
            Resource resource = findResource(resourceTypeName);
            if (resource == null) {
                traitFactory = EMPTY_TRAIT_FACTORY;
            } else {
                JAXBElement<? extends TraitGenerator> element = resource.getTraitGenerator();
                if (element == null) {
                    traitFactory = EMPTY_TRAIT_FACTORY;
                } else {
                    TraitGenerator generator = element.getValue();
                    traitFactory = createTraitFactory(generator);
                }
            }

            traitFactories.put(resourceTypeName,traitFactory);
        }
        return traitFactory;
    }

    /**
     * Returns the configuration factory defined in the scenario for creating plugin configurations for the specified
     * resource type.
     *
     * @param  resourceTypeName indicates the resource for which we're retriving the plugin configuration
     *
     * @return configuration factory instance used to generate the plugin configurations; this may be <code>null</code>
     *         if the resource does not define a generator for its plugin configuration
     */
    public ConfigurationFactory getPluginConfigurationFactory(String resourceTypeName) {
        ConfigurationFactory configurationFactory = pluginConfigurationFactories.get(resourceTypeName);

        // Lazy load the factory
        if (configurationFactory == null) {
            Resource resource = findResource(resourceTypeName);
            if (resource != null) {
                JAXBElement<? extends ConfigurationGenerator> element = resource.getPluginConfigurationGenerator();
                if (element != null) {
                    ConfigurationGenerator generator = element.getValue();
                    configurationFactory = createConfigurationFactory(generator);
                    pluginConfigurationFactories.put(resourceTypeName, configurationFactory);
                }
            }
        }

        return configurationFactory;
    }

    /**
     * Returns the content factory defined in the scenario for discovering packages of the specified type.
     *
     * @param  resourceTypeName indicates the resource in which the type is defined
     * @param  packageTypeName  indicated the type being discovered
     *
     * @return factory used for discovery; this may be <code>null</code> if the scenario does not define a generator for
     *         a particular type
     */
    public ContentFactory getContentFactory(String resourceTypeName, String packageTypeName) {
        ContentFactory contentFactory = contentFactories.get(packageTypeName);

        // Lazy load the factory
        if (contentFactory == null) {
            Resource resource = findResource(resourceTypeName);
            if (resource != null) {
                List<JAXBElement<? extends ContentGenerator>> elements = resource.getContentGenerator();

                if (elements != null) {
                    for (JAXBElement<? extends ContentGenerator> element : elements) {
                        ContentGenerator generator = element.getValue();

                        if (generator.getPackageType().equals(packageTypeName)) {
                            contentFactory = createContentFactory(generator);
                            contentFactories.put(packageTypeName, contentFactory);
                        }
                    }
                }
            }
        }

        return contentFactory;
    }

    // Private  --------------------------------------------

    /**
     * Creates the appropriate resource factory instance based on the provided generator.
     *
     * @param  generator read from the scenario
     *
     * @return resource factory instance
     */
    private ResourceFactory createResourceFactory(ResourceGenerator generator) {
        if (generator instanceof SimpleResourceGenerator) {
            return new SimpleResourceFactory((SimpleResourceGenerator) generator);
        }

        return null;
    }

    /**
     * Creates the appropriate measurement factory instance based on the provided generator.
     *
     * @param  generator read from the scenario
     *
     * @return measurement factory instance
     */
    private MeasurementFactory createMeasurementFactory(MeasurementGenerator generator) {
        if (generator instanceof SimpleNumericMeasurementGenerator) {
            return new SimpleNumericMeasurementFactory();
        } else if (generator instanceof OOBNumericMeasurementGenerator) {
            return new OOBNumericMeasurementFactory();
        }

        return null;
    }

    /**
     * Creates the appropriate calltime factory instance based on the provided generator.
     *
     * @param  generator read from the scenario
     *
     * @return calltime factory instance
     */
    private CalltimeFactory createCalltimeFactory(CalltimeGenerator generator) {
        if (generator instanceof SimpleCallTimeDataGenerator) {
            return new SimpleCallTimeDataFactory();
        } else if (generator instanceof ConfigurableCallTimeDataGenerator) {
            return new ConfigurableCallTimeDataFactory((ConfigurableCallTimeDataGenerator)generator);
        }

        return null;
    }

    /**
     * Creates an appropriate Traits factory based on the provided generator.
     * @param generator read from the scenario
     * @return TraitFactory instance
     */
    private TraitFactory createTraitFactory(TraitGenerator generator) {
        if (generator instanceof SimpleTraitMeasurementGenerator) {
            return new SimpleTraitFactory();
        }

        return null;
    }


    /**
     * Creates the appropriate configuration factory instance based on the provided generator.
     *
     * @param  generator read from the scenario
     *
     * @return configuration factory instance
     */
    private ConfigurationFactory createConfigurationFactory(ConfigurationGenerator generator) {
        if (generator instanceof SimpleConfigurationGenerator) {
            return new SimpleConfigurationFactory();
        }

        return null;
    }

    /**
     * Creates the appropriate content factory instance based on the provided generator.
     *
     * @param  generator read from the scenario
     *
     * @return factory instance
     */
    private ContentFactory createContentFactory(ContentGenerator generator) {
        if (generator instanceof SimpleContentGenerator) {
            SimpleContentGenerator simpleGenerator = (SimpleContentGenerator) generator;
            return new SimpleContentFactory(simpleGenerator);
        }

        return null;
    }

    /**
     * Searches through the scenario for the resource type indicated.
     *
     * @param  resourceType being loaded from the scenario
     *
     * @return scenario resource object describing how to generate resources of this type
     */
    private Resource findResource(String resourceType) {
        List<Resource> allResources = scenario.getResource();
        for (Resource resource : allResources) {
            if (resource.getType().equals(resourceType)) {
                return resource;
            }
        }

        return null;
    }

    /**
     * Determines what scenario to load and loads the JAXB objects representing the scenario.
     */
    private void loadScenario() {
        // Determine scenario
        String scenarioName = System.getProperty(SCENARIO_PROPERTY);

        if (scenarioName == null) {
            log.info("Cannot find scenario name. Make sure the system property " + SCENARIO_PROPERTY + " is set.");
            return;
        }

        log.info("Loading Performance Testing Scenario [" + scenarioName + "]...");

        ClassLoader loader = this.getClass().getClassLoader();
        URL scenarioUrl = loader.getResource(scenarioName + ".xml");

        // Load the JAXB stuff
        Class objectFactoryClass = null;
        try {
            objectFactoryClass = loader.loadClass("org.rhq.plugins.perftest.scenario.ObjectFactory");
        } catch (ClassNotFoundException e) {
            log.error("Error finding class ObjectFactory", e);
            return;
        }

        JAXBContext jaxbContext;
        try {
            jaxbContext = JAXBContext.newInstance(objectFactoryClass);
        } catch (JAXBException e) {
            log.error("Could not instantiate JAXB context", e);
            return;
        }

        // Load the scenario
        try {
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            ValidationEventCollector vec = new ValidationEventCollector();
            unmarshaller.setEventHandler(vec);
            this.scenario = (Scenario) unmarshaller.unmarshal(scenarioUrl);
        } catch (JAXBException e) {
            log.error("Scenario [" + scenarioName + "] could not be loaded from [" + scenarioUrl + "]." , e);
            return;
        }
    }
}