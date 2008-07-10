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
package org.rhq.enterprise.server.alert;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
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
import org.rhq.core.domain.alert.notification.SnmpNotification;
import org.rhq.enterprise.server.legacy.common.shared.HQConstants;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * @author Ian Springer
 */
public class SnmpTrapSender implements PDUFactory {
    public static final int DEFAULT = 0;
    private static final String UDP_TRANSPORT = "udp";
    private static final String TCP_TRANSPORT = "tcp";

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

    private OID trapOID = SnmpConstants.coldStart;

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

    public SnmpTrapSender() {
        this.snmpEnabled = init();
    }

    private void checkTrapVariables(List<VariableBinding> vbs) {

        if ((pduType == PDU.INFORM) || (pduType == PDU.TRAP)) {
            if ((vbs.size() == 0) || ((vbs.size() >= 1) && (!(vbs.get(0)).getOid().equals(SnmpConstants.sysUpTime)))) {
                vbs.add(0, new VariableBinding(SnmpConstants.sysUpTime, sysUpTime));
            }

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

        snmp.close();
        return response;
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

    private static Address createAddress(SnmpNotification snmpNotification) {
        // TODO: Make transport configurable (ips, 09/12/07).
        final String transport = UDP_TRANSPORT;
        String address = snmpNotification.getHost() + "/" + snmpNotification.getPort();
        if (transport.equalsIgnoreCase(UDP_TRANSPORT)) {
            return new UdpAddress(address);
        } else if (transport.equalsIgnoreCase(TCP_TRANSPORT)) {
            return new TcpAddress(address);
        }

        throw new IllegalArgumentException("Unknown transport: " + transport);
    }

    protected String getVariableBindings(PDU response) {
        StringBuffer strBuf = new StringBuffer();
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
     * @param snmpNotification the notification data (target agent)
     * @param platformName the name of the platform the alert is on
     * @param conditions a string that shows the alert conditions
     * @param bootTime TODO
     * @return 'Error code' of the operation
     */
    public String sendSnmpTrap(Alert alert, SnmpNotification snmpNotification, String platformName, String conditions,
        Date bootTime) {
        if (!this.snmpEnabled) {
            return "SNMP is not enabled.";
        }

        // TODO add a request id and a timestamp

        this.address = createAddress(snmpNotification);
        String baseOid = snmpNotification.getOid();
        // bind the alert definitions name on the oid set in the alert 
        getVariableBindings(baseOid + ".1" + "={s}" + alert.getAlertDefinition().getName());
        // the resource the alert was defined on
        getVariableBindings(baseOid + ".2" + "={s}" + alert.getAlertDefinition().getResource().getName());
        // the platform this resource is on
        getVariableBindings(baseOid + ".3" + "={s}" + platformName);
        // the conditions of this alert
        getVariableBindings(baseOid + ".4" + "={s}" + conditions);
        // severity of the alert
        getVariableBindings(baseOid + ".5" + "={s}" + alert.getAlertDefinition().getPriority().toString().toLowerCase());

        setSysUpTimeFromBootTime(bootTime); // needs to be called before checkTrapVariables();
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
        setSysUpTime(new TimeTicks(delta / 1000)); // TT is 100th of a second TODO : fix this !!!

    }

    private boolean init() {
        Properties systemConfig = LookupUtil.getSystemManager().getSystemConfiguration();

        String snmpVersion = systemConfig.getProperty(HQConstants.SNMPVersion);
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

        String snmpAuthProtocol = systemConfig.getProperty(HQConstants.SNMPAuthProtocol);
        if ((snmpAuthProtocol != null) && (snmpAuthProtocol.length() > 0)) {
            if (snmpAuthProtocol.equals("MD5")) {
                this.authProtocol = AuthMD5.ID;
            } else if (snmpAuthProtocol.equals("SHA")) {
                this.authProtocol = AuthSHA.ID;
            } else {
                throw new IllegalStateException("SNMP authentication protocol unsupported: " + snmpAuthProtocol);
            }
        }

        snmpAuthProtocol = systemConfig.getProperty(HQConstants.SNMPAuthPassphrase);
        if ((snmpAuthProtocol != null) && (snmpAuthProtocol.length() > 0)) {
            this.authPassphrase = createOctetString(snmpAuthProtocol);
        }

        snmpAuthProtocol = systemConfig.getProperty(HQConstants.SNMPPrivacyPassphrase);
        if ((snmpAuthProtocol != null) && (snmpAuthProtocol.length() > 0)) {
            this.privPassphrase = createOctetString(snmpAuthProtocol);
        }

        snmpAuthProtocol = systemConfig.getProperty(HQConstants.SNMPCommunity);
        if ((snmpAuthProtocol != null) && (snmpAuthProtocol.length() > 0)) {
            this.community = createOctetString(snmpAuthProtocol);
        }

        snmpAuthProtocol = systemConfig.getProperty(HQConstants.SNMPEngineID);
        if ((snmpAuthProtocol != null) && (snmpAuthProtocol.length() > 0)) {
            this.contextEngineID = createOctetString(snmpAuthProtocol);
        }

        snmpAuthProtocol = systemConfig.getProperty(HQConstants.SNMPContextName);
        if ((snmpAuthProtocol != null) && (snmpAuthProtocol.length() > 0)) {
            this.contextName = createOctetString(snmpAuthProtocol);
        }

        snmpAuthProtocol = systemConfig.getProperty(HQConstants.SNMPSecurityName);
        if ((snmpAuthProtocol != null) && (snmpAuthProtocol.length() > 0)) {
            this.securityName = createOctetString(snmpAuthProtocol);
        }

        snmpAuthProtocol = systemConfig.getProperty(HQConstants.SNMPTrapOID);
        if ((snmpAuthProtocol != null) && (snmpAuthProtocol.length() > 0)) {
            this.trapOID = new OID(snmpAuthProtocol);
        }

        snmpAuthProtocol = systemConfig.getProperty(HQConstants.SNMPEnterpriseOID);
        if ((snmpAuthProtocol != null) && (snmpAuthProtocol.length() > 0)) {
            this.v1TrapPDU.setEnterprise(new OID(snmpAuthProtocol));
        }

        snmpAuthProtocol = systemConfig.getProperty(HQConstants.SNMPGenericID);
        if ((snmpAuthProtocol != null) && (snmpAuthProtocol.length() > 0)) {
            this.v1TrapPDU.setGenericTrap(Integer.parseInt(snmpAuthProtocol));
        }

        snmpAuthProtocol = systemConfig.getProperty(HQConstants.SNMPSpecificID);
        if ((snmpAuthProtocol != null) && (snmpAuthProtocol.length() > 0)) {
            this.v1TrapPDU.setSpecificTrap(Integer.parseInt(snmpAuthProtocol));
        }

        snmpAuthProtocol = systemConfig.getProperty(HQConstants.SNMPAgentAddress);
        if ((snmpAuthProtocol != null) && (snmpAuthProtocol.length() > 0)) {
            this.v1TrapPDU.setAgentAddress(new IpAddress(snmpAuthProtocol));
        }

        snmpAuthProtocol = systemConfig.getProperty(HQConstants.SNMPPrivacyProtocol);
        if ((snmpAuthProtocol != null) && (snmpAuthProtocol.length() > 0)) {
            if (snmpAuthProtocol.equals("DES")) {
                this.privProtocol = PrivDES.ID;
            } else if ((snmpAuthProtocol.equals("AES128")) || (snmpAuthProtocol.equals("AES"))) {
                this.privProtocol = PrivAES128.ID;
            } else if (snmpAuthProtocol.equals("AES192")) {
                this.privProtocol = PrivAES192.ID;
            } else if (snmpAuthProtocol.equals("AES256")) {
                this.privProtocol = PrivAES256.ID;
            } else {
                throw new IllegalArgumentException("Privacy protocol " + snmpAuthProtocol + " not supported");
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