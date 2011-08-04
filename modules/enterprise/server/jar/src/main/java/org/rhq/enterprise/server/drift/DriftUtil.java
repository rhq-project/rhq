/*
 * RHQ Management Platform
 * Copyright 2011, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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
package org.rhq.enterprise.server.drift;

import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.logging.LogFactory;

import org.rhq.enterprise.communications.ServiceContainer;
import org.rhq.enterprise.communications.command.client.ClientCommandSender;
import org.rhq.enterprise.communications.command.client.ClientCommandSenderConfiguration;
import org.rhq.enterprise.communications.command.client.RemoteInputStream;
import org.rhq.enterprise.server.core.comm.ServerCommunicationsServiceUtil;

/**
 * @author Jay Shaughnessy
 * @author John Sanda
 */
public class DriftUtil {

    static InputStream remoteStream(InputStream stream) {
        RemoteInputStream remoteStream = (RemoteInputStream) stream;
        ServiceContainer serviceContainer = ServerCommunicationsServiceUtil.getService().getServiceContainer();
        ClientCommandSenderConfiguration config = serviceContainer.getClientConfiguration();
        ClientCommandSender sender = serviceContainer.createClientCommandSender(remoteStream.getServerEndpoint(),
            config);
        sender.startSending();
        remoteStream.setClientCommandSender(sender);

        return stream;
    }

    static void safeClose(OutputStream os) {
        if (null != os) {
            try {
                os.close();
            } catch (Exception e) {
                LogFactory.getLog(DriftUtil.class).warn("Failed to close OutputStream", e);
            }
        }
    }

    static void safeClose(InputStream is) {
        if (null != is) {
            try {
                is.close();
            } catch (Exception e) {
                LogFactory.getLog(DriftUtil.class).warn("Failed to close InputStream", e);
            }
        }
    }

}
