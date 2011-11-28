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
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.drift.Drift;
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

    private static Set<String> binaryFileTypes = new HashSet<String>();

    static {
        binaryFileTypes.add("jar");
        binaryFileTypes.add("war");
        binaryFileTypes.add("ear");
        binaryFileTypes.add("sar"); // jboss service
        binaryFileTypes.add("har"); // hibernate archive
        binaryFileTypes.add("rar"); // resource adapter
        binaryFileTypes.add("wsr"); // jboss web service archive
        binaryFileTypes.add("zip");
        binaryFileTypes.add("tar");
        binaryFileTypes.add("bz2");
        binaryFileTypes.add("gz");
        binaryFileTypes.add("rpm");
        binaryFileTypes.add("so");
        binaryFileTypes.add("dll");
        binaryFileTypes.add("exe");
        binaryFileTypes.add("jpg");
        binaryFileTypes.add("png");
        binaryFileTypes.add("jpeg");
        binaryFileTypes.add("gif");
        binaryFileTypes.add("pdf");
        binaryFileTypes.add("swf");
        binaryFileTypes.add("bpm");
        binaryFileTypes.add("tiff");
        binaryFileTypes.add("svg");
        binaryFileTypes.add("doc");
        binaryFileTypes.add("mp3");
        binaryFileTypes.add("wav");
        binaryFileTypes.add("m4a");
        binaryFileTypes.add("mov");
        binaryFileTypes.add("mpeg");
        binaryFileTypes.add("avi");
        binaryFileTypes.add("mp4");
        binaryFileTypes.add("wmv");
        binaryFileTypes.add("deb");
        binaryFileTypes.add("sit");
        binaryFileTypes.add("iso");
        binaryFileTypes.add("dmg");
    }

    static boolean isBinaryFile(Drift<?, ?> drift) {
        return isBinaryFile(drift.getPath());
    }

    static boolean isBinaryFile(String path) {
        int index = path.lastIndexOf('.');

        if (index == -1 || index == path.length() - 1) {
            return false;
        }

        return binaryFileTypes.contains(path.substring(index + 1, path.length()));
    }

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
