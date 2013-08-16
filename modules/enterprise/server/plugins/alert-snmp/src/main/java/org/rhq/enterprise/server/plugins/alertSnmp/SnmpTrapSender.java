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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.snmp4j.CommandResponderEvent;
import org.snmp4j.CommunityTarget;
import org.snmp4j.MessageException;
import org.snmp4j.PDU;
import org.snmp4j.PDUv1;
import org.snmp4j.ScopedPDU;
import org.snmp4j.Snmp;
import org.snmp4j.Target;
import org.snmp4j.UserTarget;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.mp.CounterSupport;
import org.snmp4j.mp.DefaultCounterListener;
import org.snmp4j.mp.MPv3;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.mp.StateReference;
import org.snmp4j.mp.StatusInformation;
import org.snmp4j.security.AuthMD5;
import org.snmp4j.security.AuthSHA;
import org.snmp4j.security.PrivAES128;
import org.snmp4j.security.PrivAES192;
import org.snmp4j.security.PrivAES256;
import org.snmp4j.security.PrivDES;
import org.snmp4j.security.SecurityLevel;
import org.snmp4j.security.SecurityModels;
import org.snmp4j.security.SecurityProtocols;
import org.snmp4j.security.USM;
import org.snmp4j.security.UsmUser;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.Integer32;
import org.snmp4j.smi.IpAddress;
import org.snmp4j.smi.Null;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.TcpAddress;
import org.snmp4j.smi.TimeTicks;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.smi.UnsignedInteger32;
import org.snmp4j.smi.Variable;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.AbstractTransportMapping;
import org.snmp4j.transport.DefaultTcpTransportMapping;
import org.snmp4j.transport.DefaultUdpTransportMapping;
import org.snmp4j.util.PDUFactory;

import org.rhq.core.domain.alert.Alert;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.util.StringUtil;

/**
 * @author Ian Springer
 * @author Heiko W. Rupp
 */
@SuppressWarnings("unused")
public class SnmpTrapSender implements PDUFactory {
    public static final int DEFAULT = 0;
    private static final String UDP_TRANSPORT = "udp";
    private static final String TCP_TRANSPORT = "tcp";
    private static final String DEFAULT_RHQ_BINDING = "1.3.6.1.4.1.18016.2.1";

    private Log log = LogFactory.getLog(SnmpTrapSender.class);

    private Target target;

    private OID authProtocol;

    private OID privProtocol;

    private OctetString privPassphrase;

    private OctetString authPassphrase;

    private OctetString community = new OctetString("public");

    private OctetString contextEngineID;

    private OctetString contextName = new OctetString();

    private OctetString securityName = new OctetString();

    private TimeTicks sysUpTime = new TimeTicks(0);

    public static final OID enterpriseSpecificTrap =
        new OID(new int[] { 1,3,6,1,6,3,1,1,5,6 });

    private OID trapOID = enterpriseSpecificTrap;

    private PDUv1 v1TrapPDU = new PDUv1();

    private int version = SnmpConstants.version3;

    private int retries = 1;

    private int timeout = 1000;

    private int pduType = PDU.TRAP;

    private int maxRepetitions = 10;

    private int nonRepeaters = 0;

    private List<VariableBinding> vbs = new ArrayList<VariableBinding>();

    private Address address;

    private int operation = DEFAULT;

    private boolean snmpEnabled;

    Configuration systemConfig;

    public SnmpTrapSender(Configuration preferences) {
        this.systemConfig = preferences;
        this.snmpEnabled = init();
    }

    private void checkTrapVariables(List<VariableBinding> vbs) {
        // Only on SNMP v2c or v3 trap
        if ((pduType == PDU.INFORM) || (pduType == PDU.TRAP)) {
            // Insert sysUpTime OID if not already present
            if ((vbs.size() == 0) || ((vbs.size() >= 1) && (!(vbs.get(0)).getOid().equals(SnmpConstants.sysUpTime)))) {
                vbs.add(0, new VariableBinding(SnmpConstants.sysUpTime, sysUpTime));
            }
            // Insert trap OID if not already present
            if ((vbs.size() == 1) || ((vbs.size() >= 2) && (!(vbs.get(1)).getOid().equals(SnmpConstants.snmpTrapOID)))) {
                vbs.add(1, new VariableBinding(SnmpConstants.snmpTrapOID, trapOID));
            }
        }
    }

    private void addUsmUser(Snmp snmp) {
        snmp.getUSM().addUser(securityName,
            new UsmUser(securityName, authProtocol, authPassphrase, privProtocol, privPassphrase));
    }

    private Snmp createSnmpSession() throws IOException {
        AbstractTransportMapping transport;
        if (address instanceof TcpAddress) {
            transport = new DefaultTcpTransportMapping();
        } else {
            transport = new DefaultUdpTransportMapping();
        }

        // Could save some CPU cycles:
        // transport.setAsyncMsgProcessingSupported(false);
        Snmp snmp = new Snmp(transport);

        if (version == SnmpConstants.version3) {
            USM usm = new USM(SecurityProtocols.getInstance(), new OctetString(MPv3.createLocalEngineID()), 0);
            SecurityModels.getInstance().addSecurityModel(usm);
            addUsmUser(snmp);
        }

        return snmp;
    }

    private Target createTarget() {
        if (version == SnmpConstants.version3) {
            UserTarget target = new UserTarget();
            if (authPassphrase != null) {
                if (privPassphrase != null) {
                    target.setSecurityLevel(SecurityLevel.AUTH_PRIV);
                } else {
                    target.setSecurityLevel(SecurityLevel.AUTH_NOPRIV);
                }
            } else {
                target.setSecurityLevel(SecurityLevel.NOAUTH_NOPRIV);
            }

            target.setSecurityName(securityName);
            return target;
        } else {
            CommunityTarget target = new CommunityTarget();
            target.setCommunity(community);
            return target;
        }
    }

    public PDU send() throws IOException {
        Snmp snmp = createSnmpSession();
        try {
            this.target = createTarget();
            target.setVersion(version);
            target.setAddress(address);
            target.setRetries(retries);
            target.setTimeout(timeout);
            snmp.listen();

            PDU request = createPDU(target);
            if (request.getType() == PDU.GETBULK) {
                request.setMaxRepetitions(maxRepetitions);
                request.setNonRepeaters(nonRepeaters);
            }

            for (Object vb : vbs) {
                request.add((VariableBinding) vb);
            }

            PDU response = null;
            ResponseEvent responseEvent;
            long startTime = System.currentTimeMillis();
            responseEvent = snmp.send(request, target);
            if (responseEvent != null) {
                response = responseEvent.getResponse();
                if (log.isDebugEnabled())
                    log.debug("Received response after " + (System.currentTimeMillis() - startTime) + " millis");
            }
            return response;
        } finally {
            snmp.close();
        }
    }

    private void getVariableBindings(String args) {
        String oid = args;
        char type = 'i';
        String value = null;
        int equal = oid.indexOf("={");
        if (equal > 0) {
            oid = args.substring(0, equal);
            type = args.charAt(equal + 2);
            value = args.substring(args.indexOf('}') + 1);
        } else if (oid.indexOf('-') > 0) {
            StringTokenizer st = new StringTokenizer(oid, "-");
            if (st.countTokens() != 2) {
                throw new IllegalArgumentException("Illegal OID range specified: '" + oid);
            }

            oid = st.nextToken();
            VariableBinding vbLower = new VariableBinding(new OID(oid));
            vbs.add(vbLower);
            long last = Long.parseLong(st.nextToken());
            long first = vbLower.getOid().lastUnsigned();
            for (long k = first + 1; k <= last; k++) {
                OID nextOID = new OID(vbLower.getOid().getValue(), 0, vbLower.getOid().size() - 1);
                nextOID.appendUnsigned(k);
                VariableBinding next = new VariableBinding(nextOID);
                vbs.add(next);
            }

            return;
        }

        VariableBinding vb = new VariableBinding(new OID(oid));
        if (value != null) {
            Variable variable;
            switch (type) {
            case 'i': {
                variable = new Integer32(Integer.parseInt(value));
                break;
            }

            case 'u': {
                variable = new UnsignedInteger32(Long.parseLong(value));
                break;
            }

            case 's': {
                variable = new OctetString(value);
                break;
            }

            case 'x': {
                variable = OctetString.fromString(value, ':', 16);
                break;
            }

            case 'd': {
                variable = OctetString.fromString(value, '.', 10);
                break;
            }

            case 'b': {
                variable = OctetString.fromString(value, ' ', 2);
                break;
            }

            case 'n': {
                variable = new Null();
                break;
            }

            case 'o': {
                variable = new OID(value);
                break;
            }

            case 't': {
                variable = new TimeTicks(Long.parseLong(value));
                break;
            }

            case 'a': {
                variable = new IpAddress(value);
                break;
            }

            default: {
                throw new IllegalArgumentException("Variable type " + type + " not supported");
            }
            }

            vb.setVariable(variable);
        }

        vbs.add(vb);
    }

    private static OctetString createOctetString(String s) {
        OctetString octetString;
        if (s.startsWith("0x")) {
            octetString = OctetString.fromHexString(s.substring(2), ':');
        } else {
            octetString = new OctetString(s);
        }

        return octetString;
    }

    private Address createAddress(Configuration properties) {

        String host = properties.getSimpleValue("host",null);
        String portS = properties.getSimpleValue("port",null);


        if (host==null) {
            String tmp = systemConfig.getSimpleValue("defaultTargetHost",null);
            if ((tmp != null) && (tmp.length() > 0)) {
                host=tmp;
            }
        }

        if (portS==null) {
            String tmp = systemConfig.getSimpleValue("defaultPort","162");
            if ((tmp != null) && (tmp.length() > 0)) {
                portS = tmp;
            }
        }
        Integer port = Integer.valueOf(portS);
        if (port==0) {
            port = 162; // just to make sure
        }

        String transport = systemConfig.getSimpleValue("transport","UDP");


        String address = host + "/" + port;
        if (transport.equalsIgnoreCase(UDP_TRANSPORT)) {
            return new UdpAddress(address);
        } else if (transport.equalsIgnoreCase(TCP_TRANSPORT)) {
            return new TcpAddress(address);
        }

        throw new IllegalArgumentException("Unknown transport: " + transport);
    }

    protected String getVariableBindings(PDU response) {
        StringBuilder strBuf = new StringBuilder();
        for (int i = 0; i < response.size(); i++) {
            VariableBinding vb = response.get(i);
            strBuf.append(vb.toString());
        }

        return strBuf.toString();
    }

    protected String getReport(PDU response) {
        if (response.size() < 1) {
            return "REPORT PDU does not contain a variable binding.";
        }

        VariableBinding vb = response.get(0);
        OID oid = vb.getOid();
        log.debug(" Current counter value is " + vb.getVariable() + ".");

        if (SnmpConstants.usmStatsUnsupportedSecLevels.equals(oid)) {
            return "REPORT: Unsupported Security Level.";
        } else if (SnmpConstants.usmStatsNotInTimeWindows.equals(oid)) {
            return "REPORT: Message not within time window.";
        } else if (SnmpConstants.usmStatsUnknownUserNames.equals(oid)) {
            return "REPORT: Unknown user name.";
        } else if (SnmpConstants.usmStatsUnknownEngineIDs.equals(oid)) {
            return "REPORT: Unknown engine id.";
        } else if (SnmpConstants.usmStatsWrongDigests.equals(oid)) {
            return "REPORT: Wrong digest.";
        } else if (SnmpConstants.usmStatsDecryptionErrors.equals(oid)) {
            return "REPORT: Decryption error.";
        } else if (SnmpConstants.snmpUnknownSecurityModels.equals(oid)) {
            return "REPORT: Unknown security model.";
        } else if (SnmpConstants.snmpInvalidMsgs.equals(oid)) {
            return "REPORT: Invalid message.";
        } else if (SnmpConstants.snmpUnknownPDUHandlers.equals(oid)) {
            return "REPORT: Unknown PDU handler.";
        } else if (SnmpConstants.snmpUnavailableContexts.equals(oid)) {
            return "REPORT: Unavailable context.";
        } else if (SnmpConstants.snmpUnknownContexts.equals(oid)) {
            return "REPORT: Unknown context.";
        } else {
            return "REPORT contains unknown OID (" + oid.toString() + ").";
        }
    }

    /**
     * processPdu
     *
     * @param e CommandResponderEvent
     */
    public synchronized void processPdu(CommandResponderEvent e) {
        PDU command = e.getPDU();
        if (command != null) {
            if (log.isDebugEnabled())
                log.debug(command.toString());
            if ((command.getType() != PDU.TRAP) && (command.getType() != PDU.V1TRAP)
                && (command.getType() != PDU.REPORT) && (command.getType() != PDU.RESPONSE)) {
                command.setErrorIndex(0);
                command.setErrorStatus(0);
                command.setType(PDU.RESPONSE);
                StatusInformation statusInformation = new StatusInformation();
                StateReference ref = e.getStateReference();
                try {
                    e.getMessageDispatcher().returnResponsePdu(e.getMessageProcessingModel(), e.getSecurityModel(),
                        e.getSecurityName(), e.getSecurityLevel(), command, e.getMaxSizeResponsePDU(), ref,
                        statusInformation);
                } catch (MessageException ex) {
                    log.error("Error while sending response: " + ex.getMessage());
                }
            }
        }
    }

    public PDU createPDU(Target target) {
        PDU request;
        if (target.getVersion() == SnmpConstants.version3) {
            request = new ScopedPDU();
            ScopedPDU scopedPDU = (ScopedPDU) request;
            if (contextEngineID != null) {
                scopedPDU.setContextEngineID(contextEngineID);
            }

            if (contextName != null) {
                scopedPDU.setContextName(contextName);
            }
        } else {
            if (pduType == PDU.V1TRAP) {
                v1TrapPDU.setTimestamp(sysUpTime.toLong());
                request = v1TrapPDU;
            } else {
                request = new PDU();
            }
        }

        request.setType(pduType);
        return request;
    }

    /**
     * This method sends the actual trap
     * @param alert The alert to send
     * @param alertParameters the notification data (target agent)
     * @param platformName the name of the platform the alert is on
     * @param conditions a string that shows the alert conditions
     * @param bootTime TODO
     * @param alertUrl TODO
     * @return 'Error code' of the operation
     */
    public String sendSnmpTrap(Alert alert, Configuration alertParameters, String platformName, String conditions,
        Date bootTime, String alertUrl, String hierarchy) {
        if (!this.snmpEnabled) {
            return "SNMP is not enabled.";
        }
        String variableBindingPrefix = alertParameters.getSimpleValue(SnmpInfo.PARAM_VARIABLE_BINDING_PREFIX,
            DEFAULT_RHQ_BINDING);

        // request id and a timestamp are added below in setSysUpTime..

        this.address = createAddress(alertParameters);
        // bind the alert definitions name on the oid set in the alert
        getVariableBindings(variableBindingPrefix + ".1" + "={s}" + alert.getAlertDefinition().getName());
        // the resource the alert was defined on
        getVariableBindings(variableBindingPrefix + ".2" + "={s}" + alert.getAlertDefinition().getResource().getName());
        // the platform this resource is on
        getVariableBindings(variableBindingPrefix + ".3" + "={s}" + platformName);
        // the conditions of this alert
        getVariableBindings(variableBindingPrefix + ".4" + "={s}" + conditions);
        // severity of the alert
        getVariableBindings(variableBindingPrefix + ".5" + "={s}" + alert.getAlertDefinition().getPriority().toString().toLowerCase());
        // url of the alert detail
        getVariableBindings(variableBindingPrefix + ".6" + "={s}" + alertUrl);
        // hierarchy of the resource on alert
        getVariableBindings(variableBindingPrefix + ".7" + "={s}" + hierarchy);

        setSysUpTimeFromBootTime(bootTime); // needs to be called before checkTrapVariables();
        setTrapOIDFromAlertParameters(alertParameters); // needs to be called before checkTrapVariables();
        checkTrapVariables(this.vbs);
        try {
            PDU response = send();
            if ((getPduType() == PDU.TRAP) || (getPduType() == PDU.V1TRAP)) {
                return (PDU.getTypeString(getPduType()) + " sent successfully");
            } else if (response == null) {
                return ("Request timed out.");
            } else if (response.getType() == PDU.REPORT) {
                return getReport(response);
            } else if (this.operation == DEFAULT) {
                return "Response received with requestID=" + response.getRequestID() + ", errorIndex="
                    + response.getErrorIndex() + ", " + "errorStatus=" + response.getErrorStatusText() + "("
                    + response.getErrorStatus() + ")" + "\n" + getVariableBindings(response);
            } else {
                return "Received something strange: requestID=" + response.getRequestID() + ", errorIndex="
                    + response.getErrorIndex() + ", " + "errorStatus=" + response.getErrorStatusText() + "("
                    + response.getErrorStatus() + ")" + "\n" + getVariableBindings(response);
            }
        } catch (IOException ex) {
            log.error(ex.getMessage());
            return "Error while trying to send request: " + ex.getMessage();
        } catch (IllegalArgumentException ex) {
            log.error(ex.getMessage());
            return "SNMPAction configured incorrectly: " + ex.getMessage();
        }
    }

    /**
     * Calculate the time diff between system boot time and now and
     * set the uptime variable from it.
     * @param bootTime
     */
    private void setSysUpTimeFromBootTime(Date bootTime) {
        long now = System.currentTimeMillis();
        long delta;
        if (bootTime != null) {
            delta = now - bootTime.getTime();
        } else
            delta = 0;
        setSysUpTime(new TimeTicks(delta / 100)); // TT is 100th of a second

    }

    private void setTrapOIDFromAlertParameters(Configuration alertParameters) {
        String trapOid = alertParameters.getSimpleValue(SnmpInfo.PARAM_TRAP_OID, null);
        if (StringUtil.isNotBlank(trapOid)) {
            setTrapOID(new OID(trapOid));
        }
    }

    private boolean init() {

        String snmpVersion = systemConfig.getSimpleValue("snmpVersion",null);
        if ((snmpVersion != null) && (snmpVersion.length() > 0)) {
            if (snmpVersion.equals("1")) {
                this.version = SnmpConstants.version1;
                this.pduType = PDU.V1TRAP;
            } else if (snmpVersion.equals("2c")) {
                this.version = SnmpConstants.version2c;
            } else if (snmpVersion.equals("3")) {
                this.version = SnmpConstants.version3;
            } else {
                throw new IllegalStateException("SNMP version " + snmpVersion + " is not supported.");
            }
        } else {
            return false;
        }

        if ((this.pduType == PDU.V1TRAP) && (this.version != SnmpConstants.version1)) {
            throw new IllegalStateException("V1TRAP PDU type is only available for SNMP version 1");
        }

        String tmp = systemConfig.getSimpleValue("authProtocol","MD5");
        if ((tmp != null) && (tmp.length() > 0)) {
            if (tmp.equals("MD5")) {
                this.authProtocol = AuthMD5.ID;
            } else if (tmp.equals("SHA")) {
                this.authProtocol = AuthSHA.ID;
            } else if (tmp.equals("none")) {
                this.authProtocol=null;
            } else {
                throw new IllegalStateException("SNMP authentication protocol unsupported: " + tmp);
            }
        }

        tmp = systemConfig.getSimpleValue("authPassphrase",null);
        if ((tmp != null) && (tmp.length() > 0)) {
            this.authPassphrase = createOctetString(tmp);
        }

        tmp = systemConfig.getSimpleValue("privacyPassphrase",null);
        if ((tmp != null) && (tmp.length() > 0)) {
            this.privPassphrase = createOctetString(tmp);
        }

        tmp = systemConfig.getSimpleValue("community",null);
        if ((tmp != null) && (tmp.length() > 0)) {
            this.community = createOctetString(tmp);
        }

        tmp = systemConfig.getSimpleValue("engineId",null);
        if ((tmp != null) && (tmp.length() > 0)) {
            this.contextEngineID = createOctetString(tmp);
        }

        tmp = systemConfig.getSimpleValue("targetContext",null);
        if ((tmp != null) && (tmp.length() > 0)) {
            this.contextName = createOctetString(tmp);
        }

        tmp = systemConfig.getSimpleValue("securityName",null);
        if ((tmp != null) && (tmp.length() > 0)) {
            this.securityName = createOctetString(tmp);
        }

        tmp = systemConfig.getSimpleValue("trapOid",null);
        if ((tmp != null) && (tmp.length() > 0)) {
            this.trapOID = new OID(tmp);
        }

        tmp = systemConfig.getSimpleValue("enterpriseOid",null);
        if ((tmp != null) && (tmp.length() > 0)) {
            this.v1TrapPDU.setEnterprise(new OID(tmp));
        }

        tmp = systemConfig.getSimpleValue("genericId",null);
        if ((tmp != null) && (tmp.length() > 0)) {
            this.v1TrapPDU.setGenericTrap(Integer.parseInt(tmp));
        }

        tmp = systemConfig.getSimpleValue("specificId",null);
        if ((tmp != null) && (tmp.length() > 0)) {
            this.v1TrapPDU.setSpecificTrap(Integer.parseInt(tmp));
        }

        tmp = systemConfig.getSimpleValue("agentAddress",null);
        if ((tmp != null) && (tmp.length() > 0)) {
            this.v1TrapPDU.setAgentAddress(new IpAddress(tmp));
        }

        tmp = systemConfig.getSimpleValue("privacyProtocol","AES");
        if ((tmp != null) && (tmp.length() > 0)) {
            if (tmp.equals("DES")) {
                this.privProtocol = PrivDES.ID;
            } else if ((tmp.equals("AES128")) || (tmp.equals("AES"))) {
                this.privProtocol = PrivAES128.ID;
            } else if (tmp.equals("AES192")) {
                this.privProtocol = PrivAES192.ID;
            } else if (tmp.equals("AES256")) {
                this.privProtocol = PrivAES256.ID;
            } else {
                throw new IllegalArgumentException("Privacy protocol " + tmp + " not supported");
            }
        }

        // Set the default counter listener to return proper USM and MP error counters.
        CounterSupport.getInstance().addCounterListener(new DefaultCounterListener());

        return true;
    }

    /*=== Getters and Setters ==============================================*/

    public OctetString getAuthPassphrase() {
        return authPassphrase;
    }

    public void setAuthPassphrase(OctetString authPassphrase) {
        this.authPassphrase = authPassphrase;
    }

    public OID getAuthProtocol() {
        return authProtocol;
    }

    public void setAuthProtocol(OID authProtocol) {
        this.authProtocol = authProtocol;
    }

    public OctetString getCommunity() {
        return community;
    }

    public void setCommunity(OctetString community) {
        this.community = community;
    }

    public OctetString getContextEngineID() {
        return contextEngineID;
    }

    public void setContextEngineID(OctetString contextEngineID) {
        this.contextEngineID = contextEngineID;
    }

    public void setContextName(OctetString contextName) {
        this.contextName = contextName;
    }

    public OctetString getContextName() {
        return contextName;
    }

    public int getMaxRepetitions() {
        return maxRepetitions;
    }

    public void setMaxRepetitions(int maxRepetitions) {
        this.maxRepetitions = maxRepetitions;
    }

    public int getNonRepeaters() {
        return nonRepeaters;
    }

    public void setNonRepeaters(int nonRepeaters) {
        this.nonRepeaters = nonRepeaters;
    }

    public int getOperation() {
        return operation;
    }

    public void setOperation(int operation) {
        this.operation = operation;
    }

    public OctetString getPrivPassphrase() {
        return privPassphrase;
    }

    public void setPrivPassphrase(OctetString privPassphrase) {
        this.privPassphrase = privPassphrase;
    }

    public OID getPrivProtocol() {
        return privProtocol;
    }

    public void setPrivProtocol(OID privProtocol) {
        this.privProtocol = privProtocol;
    }

    public int getRetries() {
        return retries;
    }

    public void setRetries(int retries) {
        this.retries = retries;
    }

    public OctetString getSecurityName() {
        return securityName;
    }

    public void setSecurityName(OctetString securityName) {
        this.securityName = securityName;
    }

    public TimeTicks getSysUpTime() {
        return sysUpTime;
    }

    public void setSysUpTime(TimeTicks sysUpTime) {
        this.sysUpTime = sysUpTime;
    }

    public Target getTarget() {
        return target;
    }

    public void setTarget(Target target) {
        this.target = target;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public OID getTrapOID() {
        return trapOID;
    }

    public void setTrapOID(OID trapOID) {
        this.trapOID = trapOID;
    }

    public List getVbs() {
        return vbs;
    }

    public void setVbs(ArrayList vbs) {
        this.vbs = vbs;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public int getPduType() {
        return pduType;
    }

    public void setPduType(int pduType) {
        this.pduType = pduType;
    }
}
