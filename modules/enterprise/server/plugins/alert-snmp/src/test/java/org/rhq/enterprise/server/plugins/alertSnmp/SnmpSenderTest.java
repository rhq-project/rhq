/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package org.rhq.enterprise.server.plugins.alertSnmp;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;
import static org.rhq.core.domain.alert.AlertPriority.HIGH;
import static org.rhq.core.domain.alert.notification.ResultState.FAILURE;
import static org.rhq.core.domain.alert.notification.ResultState.SUCCESS;
import static org.rhq.enterprise.server.plugins.alertSnmp.SnmpInfo.PARAM_HOST;
import static org.rhq.enterprise.server.plugins.alertSnmp.SnmpInfo.PARAM_PORT;
import static org.rhq.enterprise.server.plugins.alertSnmp.SnmpInfo.PARAM_TRAP_OID;
import static org.rhq.enterprise.server.plugins.alertSnmp.SnmpInfo.PARAM_VARIABLE_BINDING_PREFIX;
import static org.testng.Assert.*;

import java.util.Arrays;
import java.util.ListIterator;
import java.util.Vector;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.snmp4j.CommandResponder;
import org.snmp4j.CommandResponderEvent;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.SMIConstants;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.DefaultUdpTransportMapping;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.rhq.core.domain.alert.Alert;
import org.rhq.core.domain.alert.AlertDefinition;
import org.rhq.core.domain.alert.AlertPriority;
import org.rhq.core.domain.alert.notification.SenderResult;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.util.StringUtil;
import org.rhq.enterprise.server.alert.AlertManagerLocal;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;

/**
 * @author Thomas Segismont
 */
public class SnmpSenderTest {

    private static final Log LOG = LogFactory.getLog(SnmpSenderTest.class);

    private static final String TEST_TRAP_OID_PLUGIN_CONFIG = "1.3.6.1.4.1.18017";

    private static final String TEST_TRAP_OID_ALERT_PARAM = "1.3.6.1.4.1.18018";

    private static final String TEST_VARIABLE_BINDING_PREFIX = "1.3.6.1.4.1.18019";

    private static final String TEST_HOST = "127.0.0.1";

    private static final int TEST_PORT = 35162;

    private static final String TEST_PORT_VARIABLE = "alert.snmp.test.port";

    private ConcurrentLinkedQueue<PDU> receivedTraps;

    private Snmp snmp;

    @Mock
    private ResourceManagerLocal resourceManager;

    @Mock
    private AlertManagerLocal alertManager;

    private TestSnmpSender snmpSender;

    @BeforeMethod
    public void setUp() throws Exception {
        receivedTraps = new ConcurrentLinkedQueue<PDU>();

        snmp = new Snmp(new DefaultUdpTransportMapping());
        Address targetAddress = new UdpAddress(getTestPort());
        boolean installedTrapListener = snmp.addNotificationListener(targetAddress, new CommandResponder() {
            @Override
            public void processPdu(CommandResponderEvent event) {
                receivedTraps.offer(event.getPDU());
            }
        });
        if (!installedTrapListener) {
            throw new RuntimeException("Could not install trap listener");
        }

        MockitoAnnotations.initMocks(this);

        Configuration pluginConfiguration = new Configuration();
        pluginConfiguration.setSimpleValue("snmpVersion", "2c");
        pluginConfiguration.setSimpleValue("trapOid", TEST_TRAP_OID_PLUGIN_CONFIG);
        pluginConfiguration.setSimpleValue("community", "public");
        snmpSender = new TestSnmpSender(resourceManager, alertManager, pluginConfiguration);
    }

    private static int getTestPort() {
        String testPortVariable = System.getProperty(TEST_PORT_VARIABLE);
        if (StringUtil.isNotBlank(testPortVariable)) {
            try {
                int port = Integer.parseInt(testPortVariable);
                LOG.info("Using port " + testPortVariable + " for SNMP traps");
                return port;
            } catch (NumberFormatException e) {
                LOG.warn("Invalid port variable: " + testPortVariable);
            }
        }
        LOG.info("Using default port " + TEST_PORT + " for SNMP traps");
        return TEST_PORT;
    }

    @AfterMethod
    public void tearDown() throws Exception {
        if (snmp != null) {
            snmp.close();
        }
    }

    @Test
    public void shouldReturnSimpleFailureForInvalidNotificationParameters() {
        Configuration alertParameters = new Configuration();
        snmpSender.setAlertParameters(alertParameters);

        SenderResult result = snmpSender.send(createAlertForResourceWithId(13004, "", "", HIGH));

        assertNotNull(result);
        assertEquals(result.getState(), FAILURE);
        assertEquals(result.getFailureMessages().size(), 1);
        String expectedError = SnmpInfo.load(alertParameters).error;
        assertNotNull(expectedError);
        assertEquals(result.getFailureMessages().get(0), expectedError);
    }

    @Test
    public void shouldReturnSimpleFailureWhenErrorOccurs() {
        Configuration alertParameters = new Configuration();
        alertParameters.setSimpleValue(PARAM_HOST, TEST_HOST);
        alertParameters.setSimpleValue(PARAM_VARIABLE_BINDING_PREFIX, TEST_VARIABLE_BINDING_PREFIX);
        alertParameters.setSimpleValue(PARAM_PORT, String.valueOf(getTestPort()));
        snmpSender.setAlertParameters(alertParameters);

        int resourceId = 13004;
        Alert alert = createAlertForResourceWithId(resourceId, "", "", HIGH);
        String exceptionMessage = "Test Error";
        when(resourceManager.getResourceLineage(eq(resourceId))).thenThrow(new RuntimeException(exceptionMessage));

        SenderResult result = snmpSender.send(alert);

        assertNotNull(result);
        assertEquals(result.getState(), FAILURE);
        assertEquals(result.getFailureMessages().size(), 1);
        String actualErrorMessage = result.getFailureMessages().get(0);
        assertTrue(actualErrorMessage.endsWith(exceptionMessage), "Unexpected error message: " + actualErrorMessage);
    }

    @Test(timeOut = 1000 * 60)
    public void testSendWithDefaultSnmpTrapOid() throws Exception {
        Configuration alertParameters = new Configuration();
        alertParameters.setSimpleValue(PARAM_HOST, TEST_HOST);
        alertParameters.setSimpleValue(PARAM_VARIABLE_BINDING_PREFIX, TEST_VARIABLE_BINDING_PREFIX);
        alertParameters.setSimpleValue(PARAM_PORT, String.valueOf(getTestPort()));
        snmpSender.setAlertParameters(alertParameters);

        testSendWithSnmpTrapOid(new OID(TEST_TRAP_OID_PLUGIN_CONFIG));
    }

    @Test(timeOut = 1000 * 60)
    public void testSendWithSpecificSnmpTrapOid() throws Exception {
        Configuration alertParameters = new Configuration();
        alertParameters.setSimpleValue(PARAM_HOST, TEST_HOST);
        alertParameters.setSimpleValue(PARAM_VARIABLE_BINDING_PREFIX, TEST_VARIABLE_BINDING_PREFIX);
        alertParameters.setSimpleValue(PARAM_PORT, String.valueOf(getTestPort()));
        alertParameters.setSimpleValue(PARAM_TRAP_OID, TEST_TRAP_OID_ALERT_PARAM);
        snmpSender.setAlertParameters(alertParameters);

        testSendWithSnmpTrapOid(new OID(TEST_TRAP_OID_ALERT_PARAM));
    }

    private void testSendWithSnmpTrapOid(OID snmpTrapOid) throws InterruptedException {
        int resourceId = 13004;
        String resourceName = "Resource " + resourceId;
        String alertDefinitionName = "Alert Definition " + resourceId;
        AlertPriority alertPriority = HIGH;
        Alert alert = createAlertForResourceWithId(resourceId, resourceName, alertDefinitionName, alertPriority);
        Resource platformResouce = new Resource();
        String platformName = "Platform Resource " + resourceId;
        platformResouce.setName(platformName);

        when(resourceManager.getResourceLineage(eq(resourceId))).thenReturn(
            Arrays.asList(platformResouce, alert.getAlertDefinition().getResource()));
        String alertConditions = "Alert Conditions " + resourceId;
        when(alertManager.prettyPrintAlertConditions(eq(alert), eq(false))).thenReturn(alertConditions);
        String alertUrl = "https://www.rhq.com/alert/" + resourceId;
        when(alertManager.prettyPrintAlertURL(eq(alert))).thenReturn(alertUrl);

        assertNull(receivedTraps.peek(), "Something sent a trap before on our test port");

        SenderResult result = snmpSender.send(alert);

        assertEquals(result.getState(), SUCCESS, result.getFailureMessages().toString());
        while (receivedTraps.peek() == null) {
            Thread.sleep(1000);
        }
        PDU pdu = receivedTraps.poll();
        assertNull(receivedTraps.peek(), "Only one trap should have been received");

        assertExpectedPdu(pdu, new PduExpectedValues(snmpTrapOid, resourceName, alertDefinitionName, alertPriority,
            platformName, alertConditions, alertUrl));
    }

    private void assertExpectedPdu(PDU pdu, PduExpectedValues expectedValues) {
        Vector variableBindings = pdu.getVariableBindings();

        assertTrue(variableBindings.size() == 9, "Variable bindings should contain 9 variable bindings and not "
            + variableBindings.size() + ": " + variableBindings);

        @SuppressWarnings("unchecked")
        ListIterator<VariableBinding> variableBindingsIterator = variableBindings.listIterator();

        VariableBinding variableBinding = variableBindingsIterator.next();
        assertEquals(variableBinding.getOid(), SnmpConstants.sysUpTime);

        variableBinding = variableBindingsIterator.next();
        assertEquals(variableBinding.getOid(), SnmpConstants.snmpTrapOID);
        assertEquals(variableBinding.getVariable(), expectedValues.getSnmpTrapOid());

        OID oidPrefix = new OID(TEST_VARIABLE_BINDING_PREFIX);
        while (variableBindingsIterator.hasNext()) {
            variableBinding = variableBindingsIterator.next();

            assertVariableBindingIsPrefixed(variableBinding, oidPrefix);
            assertVariableBindingHasStringValue(variableBinding);

            switch (variableBindingsIterator.previousIndex()) {
            case 2:
                assertEquals(variableBinding.getVariable(), new OctetString(expectedValues.getAlertDefinitionName()));
                break;
            case 3:
                assertEquals(variableBinding.getVariable(), new OctetString(expectedValues.getResourceName()));
                break;
            case 4:
                assertEquals(variableBinding.getVariable(), new OctetString(expectedValues.getPlatformName()));
                break;
            case 5:
                assertEquals(variableBinding.getVariable(), new OctetString(expectedValues.getAlertConditions()));
                break;
            case 6:
                assertEquals(variableBinding.getVariable(), new OctetString(expectedValues.getAlertPriority()
                    .toString().toLowerCase()));
                break;
            case 7:
                assertEquals(variableBinding.getVariable(), new OctetString(expectedValues.getAlertUrl()));
                break;
            case 8:
                assertEquals(variableBinding.getVariable(), new OctetString(expectedValues.getPlatformName() + "::"
                    + expectedValues.getResourceName() + "::"));
                break;
            default:
                throw new RuntimeException("Unexpected index: " + variableBindingsIterator.previousIndex());
            }
        }
    }

    private void assertVariableBindingHasStringValue(VariableBinding variableBinding) {
        assertEquals(variableBinding.getVariable().getSyntax(), SMIConstants.SYNTAX_OCTET_STRING,
            "Variable binding value [" + variableBinding.getVariable() + "] has wrong type");
    }

    private void assertVariableBindingIsPrefixed(VariableBinding variableBinding, OID oidPrefix) {
        assertTrue(variableBinding.getOid().startsWith(oidPrefix), "Variable binding OID [" + variableBinding.getOid()
            + "] has wrong prefix");
    }

    private Alert createAlertForResourceWithId(int resourceId, String resourceName, String alertDefinitionName,
        AlertPriority alertPriority) {
        Resource resource = new Resource();
        resource.setId(resourceId);
        resource.setName(resourceName);
        AlertDefinition alertDefinition = new AlertDefinition();
        alertDefinition.setName(alertDefinitionName);
        alertDefinition.setResource(resource);
        alertDefinition.setPriority(alertPriority);
        Alert alert = new Alert();
        alert.setAlertDefinition(alertDefinition);
        return alert;
    }

    private static final class TestSnmpSender extends SnmpSender {

        TestSnmpSender(ResourceManagerLocal resourceManager, AlertManagerLocal alertManager,
            Configuration pluginConfiguration) {
            super(resourceManager, alertManager);
            this.preferences = pluginConfiguration;
        }

        void setAlertParameters(Configuration alertParameters) {
            this.alertParameters = alertParameters;
        }

    }

    private static final class PduExpectedValues {

        private final OID snmpTrapOid;
        private final String resourceName;
        private final String alertDefinitionName;
        private final AlertPriority alertPriority;
        private final String platformName;
        private final String alertConditions;
        private final String alertUrl;

        private PduExpectedValues(OID snmpTrapOid, String resourceName, String alertDefinitionName,
            AlertPriority alertPriority, String platformName, String alertConditions, String alertUrl) {
            this.snmpTrapOid = snmpTrapOid;
            this.resourceName = resourceName;
            this.alertDefinitionName = alertDefinitionName;
            this.alertPriority = alertPriority;
            this.platformName = platformName;
            this.alertConditions = alertConditions;
            this.alertUrl = alertUrl;
        }

        public OID getSnmpTrapOid() {
            return snmpTrapOid;
        }

        public String getResourceName() {
            return resourceName;
        }

        public String getAlertDefinitionName() {
            return alertDefinitionName;
        }

        public AlertPriority getAlertPriority() {
            return alertPriority;
        }

        public String getPlatformName() {
            return platformName;
        }

        public String getAlertConditions() {
            return alertConditions;
        }

        public String getAlertUrl() {
            return alertUrl;
        }
    }
}
