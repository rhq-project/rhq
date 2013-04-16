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

import org.rhq.core.domain.configuration.Configuration;

/**
 * Utility class giving a view of SNMP alert notification parameters.
 *
 * @deprecated as of 4.7. It's an internal utility class and should not have been made public.
 */
@Deprecated
public class SnmpInfo {

    // SNMP Alert Notification parameter names
    static final String PARAM_HOST = "host";
    static final String PARAM_PORT = "port";
    static final String PARAM_VARIABLE_BINDING_PREFIX = "oid";
    static final String PARAM_TRAP_OID = "trapOid";

    // Default remote port for SNMP trap
    static final String DEFAULT_PORT = "162";

    final public String host;
    final public String port;
    final public String oid;
    final public String trapOid;

    final public String error;

    private SnmpInfo(String host, String port, String oid, String trapOid) {
        this.host = host;
        this.port = port;
        this.oid = oid;
        this.trapOid = trapOid;

        String error = null;
        if (oid == null) {
            error = "Missing: OID";
        }
        if (host == null) {
            if (error == null) {
                error = "Missing: host";
            } else {
                error += ", host";
            }
        }
        this.error = error;
    }

    /**
     * Creates an instance of {@link SnmpInfo} from alert definition parameters.
     *
     * @param configuration SNMP notification parameters in a {@link Configuration} object
     * @return instance of {@link SnmpInfo}
     */
    public static SnmpInfo load(Configuration configuration) {
        String host = configuration.getSimpleValue(PARAM_HOST, null); // required
        String port = configuration.getSimpleValue(PARAM_PORT, DEFAULT_PORT);
        String oid = configuration.getSimpleValue(PARAM_VARIABLE_BINDING_PREFIX, null); // required
        String trapOid = configuration.getSimpleValue(PARAM_TRAP_OID, null);
        return new SnmpInfo(host, port, oid, trapOid);
    }

    @Override
    public String toString() {
        String hostString = (host == null ? "UnknownHost" : host);
        String oidString = (oid == null ? "UnknownOID" : oid);
        String trapOidString = (trapOid == null ? "DefaultTrapOID" : trapOid);
        return hostString + ":" + port + " (" + oidString + ") (" + trapOidString + ")";
    }

}
