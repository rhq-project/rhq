/*
 * RHQ Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
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
package org.rhq.enterprise.server.plugin.pc.alert;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.alert.Alert;
import org.rhq.core.domain.configuration.Configuration;

/**
 * Abstract base class for all Alert senders. In order to implement your
 * AlertSender, you need to at least overwrite #send(). When the AlertSenderManager
 * constructs this object, it will inject the preferences for your specific alert sender
 * type.
 * For each Alert a new instance of your implementation will be called and destroyed
 * afterwards.
 *
 * @author Heiko W. Rupp
 *
 */
public abstract class AlertSender {

    /** Configuration from the global per plugin type preferences */
    protected Configuration preferences;

    /** Configuration from the per alert definition parameters */
    protected Configuration alertParameters;


    /**
     * This method is called to actually send an alert notification.
     * This is where you implement all functionality.
     *
     * The return value is a SenderResult object, which encodes a log message,
     * success or failure and can contain email addresses that got computed by
     * your AlertSender and which will be sent by the system after *all* senders
     * have been run.
     * @param alert the Alert to operate on
     * @return result of sending - a ResultState and a message for auditing
     */
    public abstract SenderResult send(Alert alert) ;
}
