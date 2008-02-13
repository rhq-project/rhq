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
package org.rhq.enterprise.communications;

import org.rhq.enterprise.communications.i18n.CommI18NFactory;
import org.rhq.enterprise.communications.i18n.CommI18NResourceKeys;

/**
 * Implements the Ping POJO which simply echoes back a message to the caller. This enables a caller to ping this object
 * to ensure connectivity. This POJO is meant to be remoted so remote clients can ping the server where this POJO lives.
 *
 * @author John Mazzitelli
 */
public class PingImpl implements Ping {
    /**
     * @see Ping#ping(String, String)
     */
    public String ping(String echoMessage, String prefix) {
        if (echoMessage == null) {
            echoMessage = CommI18NFactory.getMsg().getMsg(CommI18NResourceKeys.PING_ACK);
        }

        if (prefix != null) {
            echoMessage = prefix + echoMessage;
        }

        return echoMessage;
    }
}