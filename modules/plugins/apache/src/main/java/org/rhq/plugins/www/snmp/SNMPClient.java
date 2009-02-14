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
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.snmp4j.smi.OID;

public class SNMPClient {
    static final int AUTH_MD5 = 0;
    static final int AUTH_SHA = 1;

    public static final String DEFAULT_HOST = "127.0.0.1";
    public static final int DEFAULT_PORT = 161;
    public static final String DEFAULT_COMMUNITY = "public";
    public static final String DEFAULT_USERNAME = "username";
    public static final String DEFAULT_PASSWORD = "password";
    public static final SNMPVersion DEFAULT_VERSION = SNMPVersion.V2C;

    public enum SNMPVersion {
        V1, V2C, V3
    }

    public static final String[] VALID_AUTHTYPES = { "md5", "sha" };

    private static Log log = LogFactory.getLog(SNMPClient.class);

    //XXX cache should be configurable by subclasses
    private static int CACHE_EXPIRE_DEFAULT = 60 * 1000; //60 seconds

    private static final Map<String, OID> MIB_OID_CACHE = new HashMap<String, OID>();
    private static final Map<Integer, SNMPSession> SESSION_CACHE = new HashMap<Integer, SNMPSession>();
    private static final Properties OIDS = new Properties();

    private int sessionCacheExpire = CACHE_EXPIRE_DEFAULT;

    private static final int SESSION_TIMEOUT = 500;
    private static final int SESSION_RETRIES = 1;

    private static final String OIDS_PROPERTIES_RESOURCE_PATH = "/org/rhq/plugins/apache/oids.properties";

    public SNMPClient() {
        if (OIDS.isEmpty()) {
            initOids();
        }
    }

    private static int parseAuthMethod(String authMethod) {
        if (authMethod == null) {
            throw new IllegalArgumentException("authMethod is null");
        }

        if (authMethod.equalsIgnoreCase("md5")) {
            return AUTH_MD5;
        } else if (authMethod.equalsIgnoreCase("sha")) {
            return AUTH_SHA;
        }

        throw new IllegalArgumentException("unknown authMethod: " + authMethod);
    }

    private void initOids() {
        InputStream stream = this.getClass().getResourceAsStream(OIDS_PROPERTIES_RESOURCE_PATH);
        Properties props = new Properties();
        try {
            props.load(stream);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load oids.properties file from classpath.", e);
        }

        Enumeration<?> propNames = props.propertyNames();
        while (propNames.hasMoreElements()) {
            String propName = (String) propNames.nextElement();
            OIDS.setProperty(propName, props.getProperty(propName).trim());
        }
    }

    static synchronized OID getMibOID(String mibName) throws MIBLookupException {
        if (mibName.charAt(0) == '.') {
            // snmp4j doesn't like OIDs with leading dots...
            mibName = mibName.substring(1);
        }

        if (Character.isDigit(mibName.charAt(0))) {
            // passed-in string is already in numeric form.
            return new OID(mibName);
        }

        OID oid = MIB_OID_CACHE.get(mibName);

        if (oid == null) {
            String oidString = OIDS.getProperty(mibName);
            if (oidString == null) {
                String msg = "Failed to lookup OID for name=" + mibName;
                throw new MIBLookupException(msg);
            }

            oid = new OID(oidString);
            if (oid.size() == 0) {
                throw new IllegalStateException("Failed to parse OID string [" + oidString
                    + "] while mapping MIB name [" + mibName + "].");
            }

            log.debug("MIB name [" + mibName + "] mapped to OID [" + oid + "].");
            MIB_OID_CACHE.put(mibName, oid);
        }

        return oid;
    }

    public static String getOID(String mibName) {
        try {
            return getMibOID(mibName).toString();
        } catch (MIBLookupException e) {
            return null;
        }
    }

    /**
     * Begins a "session" with an SNMP agent.
     *
     * @param  version The version of SNMP to use. Can be one of the following values: VERSION_1, VERSION_2C, VERSION_3
     *
     * @return A SNMPSession object to be used in all future communications with the SNMP agent.
     *
     * @throws SNMPException on error
     */
    static SNMPSession startSession(SNMPVersion version) throws SNMPException {
        switch (version) {
        case V1: {
            return new SNMPSession_v1();
        }

        case V2C: {
            return new SNMPSession_v2c();
        }

        case V3: {
            return new SNMPSession_v3();
        }

        default: {
            throw new SNMPException("Invalid SNMP Version: " + version);
        }
        }
    }

    public boolean init(Properties props) throws SNMPException {
        //this is mainly for debugging.
        final String prop = "snmp.sessionCacheExpire";

        String expire = props.getProperty(prop);

        if (expire != null) {
            this.sessionCacheExpire = Integer.parseInt(expire) * 1000;
        }

        return true;
    }

    public void close() {
        Collection<SNMPSession> sessions = SESSION_CACHE.values();
        for (SNMPSession session : sessions) {
            session.close();
        }
    }

    public SNMPSession getSession(String host, Integer port, String community, SNMPVersion version)
        throws SNMPException {
        SNMPSession session;

        if (host == null) {
            host = DEFAULT_HOST;
        }

        if (port == null) {
            port = DEFAULT_PORT;
        }

        if (community == null) {
            community = DEFAULT_COMMUNITY;
        }

        if (version == null) {
            version = DEFAULT_VERSION;
        }

        int id = host.hashCode() ^ port.hashCode() ^ community.hashCode() ^ version.hashCode();

        synchronized (SESSION_CACHE) {
            session = SESSION_CACHE.get(id);
        }

        if (session != null) {
            return session;
        }

        InetAddress ip;
        try {
            ip = InetAddress.getByName(host);
        } catch (UnknownHostException e) {
            throw new SNMPException("Invalid Host: '" + host + "': " + e);
        }

        if ((port < 1) || (port > 65535)) {
            throw new SNMPException("Invalid Port: " + port);
        }

        if (!community.trim().equals(community)) {
            throw new SNMPException("Invalid Community: '" + community + "': whitespace is not permitted");
        }

        try {
            session = startSession(version);

            switch (version) {
            case V1:
            case V2C: {
                ((SNMPSession_v1) session).init(host, port, community);
                break;
            }

            case V3: {
                // TODO
                String user = "TODO";
                String pass = "TODO";
                int authType = parseAuthMethod("TODO");
                ((SNMPSession_v3) session).init(host, port, user, pass, authType);
                break;
            }

            default: {
                throw new SNMPException("unsupported SNMP version");
            }
            }

            //should be enough for properly functioning SNMP
            //and not too long for one that is not functioning at all
            session.setTimeout(SESSION_TIMEOUT);
            session.setRetries(SESSION_RETRIES);
            log.info("Initialized SNMP session for agent at " + ip + ":" + port);
        } catch (SNMPException e) {
            String msg = "Failed to initialize snmp session";
            throw new SNMPException(msg, e);
        }

        session = SNMPSessionCache.newInstance(session, this.sessionCacheExpire);

        synchronized (SESSION_CACHE) {
            SESSION_CACHE.put(id, session);
        }

        return session;
    }
}