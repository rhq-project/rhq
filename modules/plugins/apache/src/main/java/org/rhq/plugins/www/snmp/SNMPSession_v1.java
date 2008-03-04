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
package org.rhq.plugins.www.snmp;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jetbrains.annotations.NotNull;
import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.GenericAddress;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.DefaultUdpTransportMapping;
import org.snmp4j.util.DefaultPDUFactory;
import org.snmp4j.util.TreeEvent;
import org.snmp4j.util.TreeUtils;

/**
 * Implements a the SNMPSession interface for SNMPv1 sessions.
 *
 * @author Ian Springer
 */
class SNMPSession_v1 implements SNMPSession {
    static final int DEFAULT_RETRIES = 1;
    static final int DEFAULT_TIMEOUT = 10000;

    protected static Snmp session;

    protected int version;
    protected CommunityTarget target;

    private final Log log = LogFactory.getLog(this.getClass());

    private static final String ANY_LOCAL_ADDRESS = "0.0.0.0";
    private static final int ANY_FREE_PORT = 0;

    private static final int GETBULK_MAX_REPETITIONS = 10;
    private static final int GETBULK_NON_REPEATERS = 0;

    private static final String PING_OID = "1";
    private static final String PING_MIB_NAME = "iso";

    /**
     * Should only be called by SNMPClient. To get an instance of this class, use SNMPClient.startSession().
     *
     * @see SNMPClient#startSession
     */
    SNMPSession_v1() {
        this.version = SnmpConstants.version1;
    }

    /**
     * Initializes the SNMP session.
     *
     * @param  host      host name or IP address of SNMP agent
     * @param  port      TCP port of SNMP agent
     * @param  community SNMP community of SNMP agent
     *
     * @throws SNMPException on error
     */
    void init(String host, int port, String community) throws SNMPException {
        if (session == null) {
            session = initSession();
        }

        Address address = GenericAddress.parse("udp:" + host + "/" + port);
        this.target = new CommunityTarget();
        this.target.setAddress(address);
        this.target.setCommunity(new OctetString(community));
        this.target.setVersion(this.version);
        this.target.setRetries(DEFAULT_RETRIES);
        this.target.setTimeout(DEFAULT_TIMEOUT);
    }

    @NotNull
    public SNMPValue getSingleValue(String name) throws SNMPException {
        return getValue(name, PDU.GET);
    }

    @NotNull
    public SNMPValue getNextValue(String name) throws SNMPException {
        return getValue(name, PDU.GETNEXT);
    }

    @NotNull
    public List<SNMPValue> getColumn(String mibName) throws SNMPException {
        List<SNMPValue> values = new ArrayList<SNMPValue>();
        OID oid = SNMPClient.getMibOID(mibName);
        TreeUtils treeUtils = new TreeUtils(session, new DefaultPDUFactory());
        treeUtils.setMaxRepetitions(GETBULK_MAX_REPETITIONS);
        List<TreeEvent> events = treeUtils.getSubtree(this.target, oid);
        for (TreeEvent event : events) {
            if (event.isError()) {
                throw new SNMPException("Error occurred while retrieving column " + mibName + "(" + oid + "): "
                    + event.getErrorMessage(), event.getException());
            }

            VariableBinding[] varBindings = event.getVariableBindings();
            if (varBindings != null) {
                for (VariableBinding varBinding : varBindings) {
                    values.add(new SNMPValue(varBinding));
                }
            }
        }

        return values;
    }

    @NotNull
    public Map<String, SNMPValue> getTable(String mibName, int index) throws SNMPException {
        Map<String, SNMPValue> map = new LinkedHashMap<String, SNMPValue>();
        OID rootOid = new OID(SNMPClient.getMibOID(mibName)); // copy before modifying, since the OIDs are cached by SNMPClient
        rootOid.append(index);
        List<SNMPValue> values = getColumn(rootOid.toString());
        for (SNMPValue value : values) {
            OID leafOid = new OID(value.getOID());
            OID suffixOid = new OID(leafOid.getValue(), rootOid.size(), leafOid.size() - rootOid.size());
            map.put(suffixOid.toString(), value);
        }

        return map;
    }

    @NotNull
    public List<SNMPValue> getBulk(String mibName) throws SNMPException {
        List<SNMPValue> values = new ArrayList<SNMPValue>();
        PDU request = createPDU(mibName, PDU.GETBULK);
        request.setMaxRepetitions(GETBULK_MAX_REPETITIONS);
        request.setNonRepeaters(GETBULK_NON_REPEATERS);
        PDU response = sendRequest(request, mibName);
        Vector<VariableBinding> varBindings = response.getVariableBindings();
        for (VariableBinding varBinding : varBindings) {
            values.add(new SNMPValue(varBinding));
        }

        return values;
    }

    public boolean ping() {
        PDU response;
        try {
            PDU pdu = createPDU(PING_OID, PDU.GETNEXT);
            response = sendRequest(pdu, PING_MIB_NAME);
        } catch (SNMPException e) {
            log.debug("Error while pinging SNMP " + this.version + " agent at " + this
                + ". SNMP GETNEXT request for iso(1) failed - " + e);
            return false;
        }

        boolean errorOccurred = (response.getErrorStatus() != PDU.noError);
        if (errorOccurred) {
            log.error("Error while pinging SNMP " + this.version + " agent at " + this
                + ". SNMP GETNEXT request for iso(1) failed - " + response.getErrorStatusText());
        }

        return !errorOccurred;
    }

    public long getTimeout() {
        return GETBULK_MAX_REPETITIONS * this.target.getTimeout();
    }

    public void setTimeout(long timeout) {
        this.target.setTimeout(timeout / GETBULK_MAX_REPETITIONS);
    }

    public int getRetries() {
        return this.target.getRetries();
    }

    public void setRetries(int retries) {
        this.target.setRetries(retries);
    }

    @Override
    public String toString() {
        return target.getAddress() + "/" + this.target.getCommunity();
    }

    protected PDU createPDU(String mibName, int type) throws MIBLookupException {
        PDU pdu = new DefaultPDUFactory().createPDU(this.target);
        pdu.setType(type);
        OID oid = SNMPClient.getMibOID(mibName);
        pdu.add(new VariableBinding(oid));
        return pdu;
    }

    private Snmp initSession() throws SNMPException {
        try {
            InetAddress host = InetAddress.getByName(ANY_LOCAL_ADDRESS);
            int port = ANY_FREE_PORT;
            UdpAddress addr = new UdpAddress(host, port);
            session = new Snmp(new DefaultUdpTransportMapping(addr));
            session.listen();
        } catch (IOException e) {
            throw new SNMPException("Failed to initialize SNMP session.", e);
        }

        return session;
    }

    private SNMPValue getValue(String mibName, int pduType) throws SNMPException {
        PDU request = createPDU(mibName, pduType);
        PDU response = sendRequest(request, mibName);
        return new SNMPValue(response.get(GETBULK_NON_REPEATERS));
    }

    @NotNull
    private PDU sendRequest(PDU request, String mibName) throws SNMPException {
        ResponseEvent responseEvent;
        try {
            responseEvent = session.send(request, this.target);
        } catch (IOException e) {
            throw new SNMPException("Failed to send request for [" + mibName + "]", e);
        }

        if (responseEvent == null) {
            throw new SNMPException("No response to request for [" + mibName + "].");
        }

        PDU response = responseEvent.getResponse();
        if (response == null) {
            throw new SNMPException("Request for [" + mibName + "] timed out.");
        }

        return response;
    }
}