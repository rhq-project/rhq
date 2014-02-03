package org.rhq.embeddedagent.extension;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIBE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_DESCRIPTION_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.msc.service.ServiceNotFoundException;

@Test
public class SubsystemParsingTestCase extends SubsystemBaseParsingTestCase {

    @Override
    @BeforeTest
    public void initializeParser() throws Exception {
        super.initializeParser();
    }

    @Override
    @AfterTest
    public void cleanup() throws Exception {
        super.cleanup();
    }

    /**
     * Tests that the xml is parsed into the correct operations
     */
    public void testParseSubsystem() throws Exception {
        // Parse the subsystem xml into operations
        String subsystemXml = getSubsystemXml();
        List<ModelNode> operations = super.parse(subsystemXml);

        // /Check that we have the expected number of operations
        Assert.assertEquals(operations.size(), 1);

        // Check that each operation has the correct content
        // The add subsystem operation will happen first
        ModelNode addSubsystem = operations.get(0);
        Assert.assertEquals(addSubsystem.get(OP).asString(), ADD);
        PathAddress addr = PathAddress.pathAddress(addSubsystem.get(OP_ADDR));
        Assert.assertEquals(addr.size(), 1);
        PathElement element = addr.getElement(0);
        Assert.assertEquals(element.getKey(), SUBSYSTEM);
        Assert.assertEquals(element.getValue(), AgentSubsystemExtension.SUBSYSTEM_NAME);
        Assert.assertEquals(addSubsystem.get(AgentSubsystemExtension.AGENT_ENABLED).asBoolean(), true);
        List<Property> plugins = addSubsystem.get(AgentSubsystemExtension.PLUGINS_ELEMENT).asPropertyList();
        Assert.assertEquals(plugins.size(), 2);
        Assert.assertEquals(plugins.get(0).getName(), "platform"); // platform plugin is first in the xml
        Assert.assertEquals(plugins.get(0).getValue().asBoolean(), true);
        Assert.assertEquals(plugins.get(1).getName(), "blah"); // blah plugin is first in the xml
        Assert.assertEquals(plugins.get(1).getValue().asBoolean(), false);
    }

    /**
     * Test that the model created from the xml looks as expected
     */
    public void testInstallIntoController() throws Exception {
        // Parse the subsystem xml and install into the controller
        String subsystemXml = getSubsystemXml();
        KernelServices services = super.installInController(subsystemXml);

        // Read the whole model and make sure it looks as expected
        ModelNode model = services.readWholeModel();
        // System.out.println(model);
        Assert.assertTrue(model.get(SUBSYSTEM).hasDefined(AgentSubsystemExtension.SUBSYSTEM_NAME));
        Assert.assertTrue(model.get(SUBSYSTEM, AgentSubsystemExtension.SUBSYSTEM_NAME).hasDefined(
            AgentSubsystemExtension.AGENT_ENABLED));
        Assert.assertTrue(model.get(SUBSYSTEM, AgentSubsystemExtension.SUBSYSTEM_NAME,
            AgentSubsystemExtension.AGENT_ENABLED).asBoolean());
        Assert.assertTrue(model.get(SUBSYSTEM, AgentSubsystemExtension.SUBSYSTEM_NAME).hasDefined(
            AgentSubsystemExtension.PLUGINS_ELEMENT));

        List<Property> plugins = model.get(SUBSYSTEM, AgentSubsystemExtension.SUBSYSTEM_NAME)
            .get(AgentSubsystemExtension.PLUGINS_ELEMENT).asPropertyList();
        Assert.assertEquals(plugins.size(), 2);
        Assert.assertEquals(plugins.get(0).getName(), "platform"); // platform plugin is first in the xml
        Assert.assertEquals(plugins.get(0).getValue().asBoolean(), true);
        Assert.assertEquals(plugins.get(1).getName(), "blah"); // blah plugin is first in the xml
        Assert.assertEquals(plugins.get(1).getValue().asBoolean(), false);

        // Sanity check to test the service was there
        AgentService agent = (AgentService) services.getContainer().getRequiredService(AgentService.SERVICE_NAME)
            .getValue();
        assert 2 == agent.getPlugins().size();
        Assert.assertTrue(agent.getPlugins().get("platform"));
        Assert.assertFalse(agent.getPlugins().get("blah"));
    }

    /**
     * Starts a controller with a given subsystem xml and then checks that a second controller started with the xml
     * marshalled from the first one results in the same model
     */
    public void testParseAndMarshalModel() throws Exception {
        // Parse the subsystem xml and install into the first controller
        String subsystemXml = getSubsystemXml();
        KernelServices servicesA = super.installInController(subsystemXml);
        // Get the model and the persisted xml from the first controller
        ModelNode modelA = servicesA.readWholeModel();
        String marshalled = servicesA.getPersistedSubsystemXml();

        // Install the persisted xml from the first controller into a second controller
        KernelServices servicesB = super.installInController(marshalled);
        ModelNode modelB = servicesB.readWholeModel();

        // Make sure the models from the two controllers are identical
        super.compare(modelA, modelB);
    }

    /**
     * Starts a controller with the given subsystem xml and then checks that a second controller started with the
     * operations from its describe action results in the same model
     */
    public void testDescribeHandler() throws Exception {
        String subsystemXml = getSubsystemXml();
        KernelServices servicesA = super.installInController(subsystemXml);
        // Get the model and the describe operations from the first controller
        ModelNode modelA = servicesA.readWholeModel();
        ModelNode describeOp = new ModelNode();
        describeOp.get(OP).set(DESCRIBE);
        describeOp.get(OP_ADDR).set(
            PathAddress.pathAddress(PathElement.pathElement(SUBSYSTEM, AgentSubsystemExtension.SUBSYSTEM_NAME))
                .toModelNode());
        ModelNode executeOperation = servicesA.executeOperation(describeOp);
        List<ModelNode> operations = super.checkResultAndGetContents(executeOperation).asList();

        // Install the describe options from the first controller into a second controller
        KernelServices servicesB = super.installInController(operations);
        ModelNode modelB = servicesB.readWholeModel();

        // Make sure the models from the two controllers are identical
        super.compare(modelA, modelB);
    }

    /**
     * Tests that the subsystem can be removed
     */
    public void testSubsystemRemoval() throws Exception {
        // Parse the subsystem xml and install into the first controller
        String subsystemXml = getSubsystemXml();
        KernelServices services = super.installInController(subsystemXml);

        // Sanity check to test the service was there
        AgentService agent = (AgentService) services.getContainer().getRequiredService(AgentService.SERVICE_NAME)
            .getValue();
        Assert.assertEquals(agent.getPlugins().size(), 2);

        // Checks that the subsystem was removed from the model
        super.assertRemoveSubsystemResources(services);

        // Check that the services that was installed was removed
        try {
            services.getContainer().getRequiredService(AgentService.SERVICE_NAME);
            assert false : "The service should have been removed along with the subsystem";
        } catch (ServiceNotFoundException expected) {
            // test passed!
        }
    }

    public void testResourceDescription() throws Exception {
        String subsystemXml = getSubsystemXml();
        KernelServices services = super.installInController(subsystemXml);

        PathAddress agentSubsystemPath = PathAddress.pathAddress(PathElement.pathElement(SUBSYSTEM,
            AgentSubsystemExtension.SUBSYSTEM_NAME));

        // ask for resource description: /subsystem=embeddedagent:read-resource-description
        ModelNode resourceDescriptionOp = new ModelNode();
        resourceDescriptionOp.get(OP).set(READ_RESOURCE_DESCRIPTION_OPERATION);
        resourceDescriptionOp.get(OP_ADDR).set(agentSubsystemPath.toModelNode());
        resourceDescriptionOp.get("operations").set(true); // we want to see the operations also
        ModelNode result = services.executeOperation(resourceDescriptionOp);
        ModelNode content = checkResultAndGetContents(result);

        // check the attributes
        Assert.assertTrue(content.get("attributes").isDefined());
        List<Property> attributes = content.get("attributes").asPropertyList();

        List<String> expectedAttributes = Arrays.asList( //
            AgentSubsystemExtension.ATTRIB_AGENT_TRANSPORT_PARAMS, //
            AgentSubsystemExtension.ATTRIB_AGENT_TRANSPORT, //
            AgentSubsystemExtension.ATTRIB_SOCKET_BINDING, //
            AgentSubsystemExtension.ATTRIB_SERVER_TRANSPORT, //
            AgentSubsystemExtension.ATTRIB_SERVER_BIND_PORT, //
            AgentSubsystemExtension.ATTRIB_SERVER_BIND_ADDRESS, //
            AgentSubsystemExtension.ATTRIB_SERVER_TRANSPORT_PARAMS, //
            AgentSubsystemExtension.ATTRIB_SERVER_ALIAS, //
            AgentSubsystemExtension.ATTRIB_DISABLE_NATIVE, //
            AgentSubsystemExtension.ATTRIB_AGENT_NAME, //
            AgentSubsystemExtension.AGENT_ENABLED, //
            AgentSubsystemExtension.PLUGINS_ELEMENT);
        Assert.assertEquals(attributes.size(), expectedAttributes.size());

        for (int i = 0 ; i < attributes.size(); i++) {
            String attrib = attributes.get(i).getName();
            Assert.assertTrue(expectedAttributes.contains(attrib), "missing attrib: " + attrib);
        }

        // check the operations
        Assert.assertTrue(content.get("operations").isDefined());
        List<Property> operations = content.get("operations").asPropertyList();
        List<String> operationNames = new ArrayList<String>();
        for (Property op : operations) {
            operationNames.add(op.getName());
        }
        Assert.assertTrue(operationNames.contains(AgentSubsystemExtension.AGENT_RESTART_OP));
        Assert.assertTrue(operationNames.contains(AgentSubsystemExtension.AGENT_STOP_OP));
        Assert.assertTrue(operationNames.contains(AgentSubsystemExtension.AGENT_STATUS_OP));
    }

    public void testExecuteOperations() throws Exception {
        String subsystemXml = getSubsystemXml();
        KernelServices services = super.installInController(subsystemXml);

        // status check - our service should be available and have 2 plugin definitions
        AgentService service = (AgentService) services.getContainer().getService(AgentService.SERVICE_NAME).getValue();
        Assert.assertEquals(service.getPlugins().size(), 2);

        PathAddress agentSubsystemPath = PathAddress.pathAddress(PathElement.pathElement(SUBSYSTEM,
            AgentSubsystemExtension.SUBSYSTEM_NAME));

        // get the startup model from subsystem xml
        ModelNode model = services.readWholeModel();

        // current list of plugins from the attribute
        ModelNode pluginsNode = model.get(SUBSYSTEM, AgentSubsystemExtension.SUBSYSTEM_NAME).get(
            AgentSubsystemExtension.PLUGINS_ELEMENT);

        // Add another plugin
        pluginsNode.add("foo", true);
        ModelNode addOp = new ModelNode();
        addOp.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
        addOp.get(OP_ADDR).set(agentSubsystemPath.toModelNode());
        addOp.get(NAME).set(AgentSubsystemExtension.PLUGINS_ELEMENT);
        addOp.get(VALUE).set(pluginsNode);
        ModelNode result = services.executeOperation(addOp);
        Assert.assertEquals(result.get(OUTCOME).asString(), SUCCESS);

        // now test that things are as they should be
        model = services.readWholeModel();
        Assert.assertTrue(model.get(SUBSYSTEM).hasDefined(AgentSubsystemExtension.SUBSYSTEM_NAME));
        Assert.assertTrue(model.get(SUBSYSTEM, AgentSubsystemExtension.SUBSYSTEM_NAME).hasDefined(
            AgentSubsystemExtension.AGENT_ENABLED));
        Assert.assertTrue(model.get(SUBSYSTEM, AgentSubsystemExtension.SUBSYSTEM_NAME,
            AgentSubsystemExtension.AGENT_ENABLED).asBoolean());
        Assert.assertTrue(model.get(SUBSYSTEM, AgentSubsystemExtension.SUBSYSTEM_NAME).hasDefined(
            AgentSubsystemExtension.PLUGINS_ELEMENT));

        List<Property> plugins = model.get(SUBSYSTEM, AgentSubsystemExtension.SUBSYSTEM_NAME)
            .get(AgentSubsystemExtension.PLUGINS_ELEMENT).asPropertyList();
        Assert.assertEquals(plugins.size(), 3); // there were 2, but we added "foo" above
        Assert.assertEquals(plugins.get(0).getName(), "platform"); // platform plugin is first in the xml
        Assert.assertEquals(plugins.get(0).getValue().asBoolean(), true);
        Assert.assertEquals(plugins.get(1).getName(), "blah"); // blah plugin is first in the xml
        Assert.assertEquals(plugins.get(1).getValue().asBoolean(), false);
        Assert.assertEquals(plugins.get(2).getName(), "foo"); // foo plugin we added above
        Assert.assertEquals(plugins.get(2).getValue().asBoolean(), true);

        // we enabled a new plugin - now should have 3 now
        Assert.assertEquals(service.getPlugins().size(), 3);
        Assert.assertTrue(service.getPlugins().get("platform"));
        Assert.assertFalse(service.getPlugins().get("blah"));
        Assert.assertTrue(service.getPlugins().get("foo"));

        // Use read-attribute instead of reading the whole model to get an attribute value
        ModelNode readOp = new ModelNode();
        readOp.get(OP).set(READ_ATTRIBUTE_OPERATION);
        readOp.get(OP_ADDR).set(agentSubsystemPath.toModelNode());
        readOp.get(NAME).set(AgentSubsystemExtension.AGENT_ENABLED);
        result = services.executeOperation(readOp);
        Assert.assertTrue(checkResultAndGetContents(result).asBoolean());

        readOp.get(NAME).set(AgentSubsystemExtension.PLUGINS_ELEMENT);
        result = services.executeOperation(readOp);
        ModelNode content = checkResultAndGetContents(result);
        plugins = content.asPropertyList();
        Assert.assertEquals(plugins.size(), 3); // there were 2, but we added "foo" above
        Assert.assertEquals(plugins.get(0).getName(), "platform"); // platform plugin is first in the xml
        Assert.assertEquals(plugins.get(0).getValue().asBoolean(), true);
        Assert.assertEquals(plugins.get(1).getName(), "blah"); // blah plugin is first in the xml
        Assert.assertEquals(plugins.get(1).getValue().asBoolean(), false);
        Assert.assertEquals(plugins.get(2).getName(), "foo"); // foo plugin we added above
        Assert.assertEquals(plugins.get(2).getValue().asBoolean(), true);

        /* TODO: I think we need to mock the ServerEnvironmentService dependency before we can do this
        // execute status
        ModelNode statusOp = new ModelNode();
        statusOp.get(OP).set(AgentSubsystemExtension.AGENT_STATUS_OP);
        statusOp.get(OP_ADDR).set(agentSubsystemPath.toModelNode());
        result = services.executeOperation(statusOp);
        Assert.assertTrue(checkResultAndGetContents(result).asBoolean());
        */
    }
}
