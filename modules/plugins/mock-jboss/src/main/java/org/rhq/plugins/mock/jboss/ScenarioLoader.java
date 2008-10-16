/*
 * JBoss, a division of Red Hat.
 * Copyright 2007, Red Hat Middleware, LLC. All rights reserved.
 */

package org.rhq.plugins.mock.jboss;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.util.ValidationEventCollector;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.plugins.mock.jboss.metrics.IncrementMetricCalculator;
import org.rhq.plugins.mock.jboss.metrics.MetricValueCalculator;
import org.rhq.plugins.mock.jboss.metrics.RandomDoubleMetricCalculator;
import org.rhq.plugins.mock.jboss.metrics.RandomIntegerMetricCalculator;
import org.rhq.plugins.mock.jboss.metrics.StaticMetricCalculator;
import org.rhq.plugins.mock.jboss.operations.FailureHandler;
import org.rhq.plugins.mock.jboss.operations.OperationHandler;
import org.rhq.plugins.mock.jboss.operations.SuccessHandler;
import org.rhq.plugins.mock.jboss.operations.TimeOutHandler;
import org.rhq.plugins.mock.jboss.scenario.MetricIncrement;
import org.rhq.plugins.mock.jboss.scenario.MetricRandomDouble;
import org.rhq.plugins.mock.jboss.scenario.MetricRandomInteger;
import org.rhq.plugins.mock.jboss.scenario.MetricStatic;
import org.rhq.plugins.mock.jboss.scenario.OperationFailure;
import org.rhq.plugins.mock.jboss.scenario.OperationSuccess;
import org.rhq.plugins.mock.jboss.scenario.OperationTimedOut;
import org.rhq.plugins.mock.jboss.scenario.Scenario;
import org.rhq.plugins.mock.jboss.scenario.ScenarioAvailability;
import org.rhq.plugins.mock.jboss.scenario.ScenarioMetric;
import org.rhq.plugins.mock.jboss.scenario.ScenarioMetricPolicy;
import org.rhq.plugins.mock.jboss.scenario.ScenarioOperationResult;
import org.rhq.plugins.mock.jboss.scenario.ScenarioOperationResultPolicy;
import org.rhq.plugins.mock.jboss.scenario.ScenarioProperty;
import org.rhq.plugins.mock.jboss.scenario.ScenarioResource;
import org.rhq.plugins.mock.jboss.scenario.ScenarioServer;

/**
 * Loads a mock scenario and caches the scenario data. The scenario data is returned by the plugin to simulate different
 * environments discovered by the plugin.
 *
 * The default scenario file loaded is named scenario1.xml and will be loaded from the classpath. This may be overridden
 * by specifying a system property named "on.mock.jboss.scenario". The value of this property should be the name of the
 * scenario XML file to load. Scenario files should adhere to the mock-jboss-scenario.xsd schema.
 *
 * Author: Jason Dobies
 */
public class ScenarioLoader {
    // Constants  --------------------------------------------

    private static final ScenarioLoader INSTANCE = new ScenarioLoader();

    private final Log LOG = LogFactory.getLog(ScenarioLoader.class);

    // Attributes  --------------------------------------------

    private String scenarioFileName = "scenario1.xml";

    private Scenario scenario;

    // Constructors  --------------------------------------------

    private ScenarioLoader() {
        loadScenario();
    }

    // Public  --------------------------------------------

    public static ScenarioLoader getInstance() {
        return INSTANCE;
    }

    /**
     * Returns all resources of the specified type that are found in the given server.
     *
     * @param serverName       server we're looking for resources under
     * @param resourceTypeName type of resources being retrieved
     *
     * @return list of <code>ScenarioResource</code> that match the type; empty list if there are none or if the server
     *         is not found in the scenario.
     */
    public List<ScenarioResource> getResources(String serverName, String resourceTypeName) {
        // Find the server we're looking in
        ScenarioServer server = null;

        List<ScenarioServer> servers = scenario.getServer();
        for (ScenarioServer s : servers) {
            if (s.getInstallPath().equals(serverName)) {
                server = s;
                break;
            }
        }

        // Make sure we found the server
        if (server == null) {
            return Collections.EMPTY_LIST;
        }

        // Find all resources in this server that match the type
        List<ScenarioResource> matchingResources = new ArrayList<ScenarioResource>();
        for (ScenarioResource scenarioResource : server.getResource()) {
            if (scenarioResource.getType().equals(resourceTypeName))
                matchingResources.add(scenarioResource);
        }

        return matchingResources;
    }

    /**
     * Return the JBoss servers defined in the scenario.
     *
     * @return list of servers
     */
    public List<ScenarioServer> getServers() {
        List<ScenarioServer> servers = scenario.getServer();
        return new ArrayList<ScenarioServer>(servers);
    }

    // Public Static  --------------------------------------------

    /**
     * Converts a scenario property list into a configuration domain object.
     *
     * @param properties list of scenario properties
     *
     * @return domain object representation of the properties
     */
    public static Configuration propertiesToConfiguration(List<ScenarioProperty> properties) {
        Configuration config = new Configuration();

        for (ScenarioProperty property : properties) {
            PropertySimple propertySimple = new PropertySimple(property.getName(), property.getValue());
            config.put(propertySimple);
        }

        return config;
    }

    /**
     * Converts a scenario availability into a domain availability type.
     *
     * @param scenarioAvailability scenario availability
     *
     * @return domain object for the availability
     */
    public static AvailabilityType convertAvailability(ScenarioAvailability scenarioAvailability) {
        if (scenarioAvailability == ScenarioAvailability.UP)
            return AvailabilityType.UP;
        else
            return AvailabilityType.DOWN;
    }

    /**
     * Instantiates the correct <code>MetricValueCalculator</code> implementation for each metric in the list. The
     * calculator instance will be populated with the metric's policy to configure its behavior.
     *
     * @param metrics list of metrics for which to create calculators.
     *
     * @return map of metric names to <code>MetricValueCalculator</code> instances. Empty map if the metrics parameter is
     *         <code>null</code>.
     */
    public static Map<String, MetricValueCalculator> createMetricValueCalculators(List<ScenarioMetric> metrics) {
        Map<String, MetricValueCalculator> calculators = new HashMap<String, MetricValueCalculator>();

        if (metrics == null)
            return calculators;

        for (ScenarioMetric metric : metrics) {
            String name = metric.getName();
            ScenarioMetricPolicy policy = metric.getMetricPolicy().getValue();

            MetricValueCalculator calculator = null;

            if (policy instanceof MetricStatic)
                calculator = new StaticMetricCalculator((MetricStatic) policy);
            else if (policy instanceof MetricRandomDouble)
                calculator = new RandomDoubleMetricCalculator((MetricRandomDouble) policy);
            else if (policy instanceof MetricRandomInteger)
                calculator = new RandomIntegerMetricCalculator((MetricRandomInteger) policy);
            else if (policy instanceof MetricIncrement)
                calculator = new IncrementMetricCalculator((MetricIncrement) policy);

            calculators.put(name, calculator);
        }

        return calculators;
    }

    /**
     * Instantiates the correct <code>OperationHandler</code> implementation for each operation result in the list. The
     * handler instance will be populated with the result policy to configure its behavior.
     *
     * @param results list of operation results for which to create handlers.
     *
     * @return map of operation names to results handlers. Empty map if the operations parameter is <code>null</code>.
     */
    public static Map<String, OperationHandler> createOperationHandlers(List<ScenarioOperationResult> results) {
        Map<String, OperationHandler> handlers = new HashMap<String, OperationHandler>();

        if (results == null)
            return handlers;

        for (ScenarioOperationResult operation : results) {
            String name = operation.getName();
            ScenarioOperationResultPolicy policy = operation.getOperationResultPolicy().getValue();

            OperationHandler handler = null;

            if (policy instanceof OperationSuccess)
                handler = new SuccessHandler((OperationSuccess) policy);
            else if (policy instanceof OperationFailure)
                handler = new FailureHandler((OperationFailure) policy);
            else if (policy instanceof OperationTimedOut)
                handler = new TimeOutHandler((OperationTimedOut) policy);

            handlers.put(name, handler);
        }

        return handlers;
    }

    /**
     * Packages each scenario artifact into a mock usable series of objects. This will take care of parsing out both the
     * revision details object as well as the revision policy.
     *
     * @param scenarioArtifacts list of scenario artifact; may be <code>null</code>
     *
     * @return set of size equal to scenarioArtifacts.size(); set of size 0 in the case that scenarioArtifacts is null.
     */
    /*public static Map<String, ArtifactHolder> createArtifacts(List<ScenarioArtifact> scenarioArtifacts)
    {
       Map<String, ArtifactHolder> artifacts = new HashMap<String, ArtifactHolder>();

       if (scenarioArtifacts == null)
          return artifacts;

       for (ScenarioArtifact scenarioArtifact : scenarioArtifacts)
       {
          String key = scenarioArtifact.getArtifactKey();
          String name = scenarioArtifact.getName();
          String type = scenarioArtifact.getType();

          ScenarioArtifactRevisionPolicy policy = scenarioArtifact.getArtifactRevisionPolicy().getValue();

          ArtifactRevisionFactory factory = null;

          if (policy instanceof RevisionOneTime)
             factory = new OneTimeArtifactRevisionFactory((RevisionOneTime)policy);
          else if (policy instanceof RevisionIdentifierIncrement)
             factory = new RevisionIdentifierArtifactRevisionFactory((RevisionIdentifierIncrement)policy);

          ArtifactHolder holder = new ArtifactHolder(key, name, type, factory);
          artifacts.put(key, holder);
       }

       return artifacts;
    }*/

    /**
     * Creates a domain representation of a revision from the provided scenario data. If the data is <code>null</code>
     * an empty revision details (that is associated with the specified artifact key) is created.
     *
     * @param data        revision data from the scenario; may be <code>null</code>
     * @param artifactKey artifact against which the revision applies
     *
     * @return populated revision details instance
     *
     * @throws IllegalArgumentException if <code>artifactKey</code> is <code>null</code>
     */
    /*public static ArtifactRevisionDetails createArtifactRevision(RevisionData data, String artifactKey)
    {
       if (artifactKey == null)
          throw new IllegalArgumentException("artifactKey cannot be null");

       ArtifactRevisionDetails details;

       if (data != null)
       {
          details = new ArtifactRevisionDetails(artifactKey, data.getMimeType());
          details.setCharset(data.getCharacterSet());
          details.setContentSize(data.getContentSize().longValue());
          details.setMd5(data.getMd5());
          details.setOwner(details.getOwner());
          details.setRevisionIdentifier(details.getRevisionIdentifier());

          details.setCreatedDate(convertXmlDate(data.getCreatedDate()));
          details.setLastAccessedDate(convertXmlDate(data.getLastAccessedDate()));
          details.setLastModifiedDate(convertXmlDate(data.getLastModifiedDate()));
       }
       else
       {
          details = new ArtifactRevisionDetails(artifactKey, null);
       }

       return details;
    }*/

    // Private  --------------------------------------------
    /**
     * Reads the scenario from the scenario descriptor.
     */
    private void loadScenario() {
        URL scenarioUrl = determineScenario();

        ClassLoader loader = this.getClass().getClassLoader();
        Class objectFactoryClass = null;
        try {
            objectFactoryClass = loader.loadClass("org.rhq.plugins.mock.jboss.scenario.ObjectFactory");
        } catch (ClassNotFoundException e) {
            LOG.error("Error finding class ObjectFactory", e);
        }

        JAXBContext jaxbContext;
        try {
            /* This version of newInstance worked in all cases except when the mock was packaged
               inside of the embedded console WAR.
                   jaxbContext = JAXBContext.newInstance("org.rhq.plugins.mock.jboss.scenario");
               jdobies, Jan 24, 2007
             */

            jaxbContext = JAXBContext.newInstance(objectFactoryClass);
        } catch (JAXBException e) {
            throw new RuntimeException("Could not instantiate JAXB context", e);
        }

        try {
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            ValidationEventCollector vec = new ValidationEventCollector();
            unmarshaller.setEventHandler(vec);
            this.scenario = (Scenario) unmarshaller.unmarshal(scenarioUrl);
        } catch (JAXBException e) {
            throw new RuntimeException("Scenario could not be loaded", e);
        }

    }

    /**
     * Determines which scenario file to use, reading from the system property on.mock.jboss.scenario if it is present.
     * Otherwise, the default scenaro1.xml will be used.
     *
     * @return URL pointing to the scenario file.
     */
    private URL determineScenario() {
        // Check for overriding by system property
        String systemValue = System.getProperty("on.mock.jboss.scenario");
        if (systemValue != null) {
            scenarioFileName = systemValue;
        }

        LOG.info("Loading scenario from file: " + scenarioFileName);

        URL scenarioUrl = this.getClass().getClassLoader().getResource(scenarioFileName);
        return scenarioUrl;
    }

    /**
     * Converts a date as read in by the XML into a Java <code>Date</code> object.
     *
     * @param xmlCalendar date as read in from the XML; may be <code>null</code>
     *
     * @return converted date instance if the date was specified in the XML; <code>null</code> otherwise
     */
    private static Date convertXmlDate(XMLGregorianCalendar xmlCalendar) {
        if (xmlCalendar == null) {
            return null;
        } else {
            return xmlCalendar.toGregorianCalendar().getTime();
        }
    }
}
