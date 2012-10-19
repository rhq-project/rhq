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

import org.snmp4j.mp.SnmpConstants;

/**
 * Implements the SNMPSession interface for SNMPv3 sessions by extending the SNMPSession_v2c implementation. SNMPv3 is
 * only different from v1 or v2c inthe way that a session is initialized.
 */
class SNMPSession_v3 extends SNMPSession_v2c {
    /**
     * Should only be called by SNMPClient. To get an instance of this class, use SNMPClient.startSession().
     *
     * @see SNMPClient#startSession
     */
    SNMPSession_v3() {
        this.version = SnmpConstants.version3;
    }

    /**
     * Initializes the SNMP session using the specified agent connection and authentication info.
     */
    void init(String address, int port, String user, String password, int authMethod) throws SNMPException {
        // TODO
    }
}