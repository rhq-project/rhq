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
package org.rhq.enterprise.communications.command.impl.identify.server;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import mazz.i18n.Logger;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.transport.ConnectorMBean;
import org.rhq.enterprise.communications.ServiceContainerConfigurationConstants;
import org.rhq.enterprise.communications.command.Command;
import org.rhq.enterprise.communications.command.CommandExecutor;
import org.rhq.enterprise.communications.command.CommandResponse;
import org.rhq.enterprise.communications.command.CommandType;
import org.rhq.enterprise.communications.command.impl.identify.AgentIdentification;
import org.rhq.enterprise.communications.command.impl.identify.Identification;
import org.rhq.enterprise.communications.command.impl.identify.IdentifyCommand;
import org.rhq.enterprise.communications.command.impl.identify.IdentifyCommandResponse;
import org.rhq.enterprise.communications.command.impl.identify.ServerIdentification;
import org.rhq.enterprise.communications.command.server.CommandService;
import org.rhq.enterprise.communications.command.server.CommandServiceMBean;
import org.rhq.enterprise.communications.i18n.CommI18NFactory;
import org.rhq.enterprise.communications.i18n.CommI18NResourceKeys;

/**
 * Processes a client request to identify this server.
 *
 * @author John Mazzitelli
 */
public class IdentifyCommandService extends CommandService {
    /**
     * Logger
     */
    private static final Logger LOG = CommI18NFactory.getLogger(IdentifyCommandService.class);

    /**
     * @see CommandExecutor#execute(Command, java.io.InputStream, java.io.OutputStream)
     */
    public CommandResponse execute(Command command, InputStream in, OutputStream out) {
        IdentifyCommand identifyCommand = new IdentifyCommand(command);

        String our_type = null;
        InvokerLocator locator = null;
        Identification our_ident;

        try {
            // from the connector, get the locator URI and find the type parameter in that URI
            ConnectorMBean connector = getConnector();
            locator = connector.getLocator();
            Map locator_params = locator.getParameters();
            our_type = (String) locator_params.get(ServiceContainerConfigurationConstants.CONNECTOR_RHQTYPE);
        } catch (Exception e) {
            return new IdentifyCommandResponse(identifyCommand, e);
        }

        if (our_type.equals(Identification.TYPE_AGENT)) {
            our_ident = new AgentIdentification(our_type, locator.getLocatorURI());
        } else if (our_type.equals(Identification.TYPE_SERVER)) {
            our_ident = new ServerIdentification(our_type, locator.getLocatorURI());
        } else {
            return new IdentifyCommandResponse(identifyCommand, new Exception(LOG
                .getMsgString(CommI18NResourceKeys.IDENTIFY_COMMAND_SERVICE_UNKNOWN_ENDPOINT)));
        }

        return new IdentifyCommandResponse(identifyCommand, our_ident);
    }

    /**
     * Supports {@link IdentifyCommand#COMMAND_TYPE}.
     *
     * @see CommandServiceMBean#getSupportedCommandTypes()
     */
    public CommandType[] getSupportedCommandTypes() {
        return new CommandType[] { IdentifyCommand.COMMAND_TYPE };
    }
}