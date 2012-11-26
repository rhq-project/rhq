/*
 * RHQ Management Platform
 * Copyright 2012, Red Hat Middleware LLC, and individual contributors
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

package org.rhq.plugins.apache.util;

import java.io.Closeable;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.SocketAddress;
import java.util.HashSet;
import java.util.Set;

/**
 * Tries to find a series of free ports.
 * 
 * The ports remain open until the {@link #close()} method is called.
 * 
 * @author Lukas Krejci
 */
public class PortScout implements Closeable {

    private Set<ServerSocket> activeSockets = new HashSet<ServerSocket>();

    public int getNextFreePort() throws IOException {
        ServerSocket s = new ServerSocket(0);
        activeSockets.add(s);
        return s.getLocalPort();
    }

    @Override
    public void close() throws IOException {
        String message = null;
        for(ServerSocket s : activeSockets) {
            SocketAddress addr = s.getLocalSocketAddress();
            try {
                s.close();
            } catch (IOException e) {
                if (message == null) {
                    message = "The following test sockets failed to close while looking for free ports:\n";
                }
                
                message += addr.toString() + ": " + e.getMessage() + ",\n";
            }
        }
        
        if (message != null) {
            message = message.substring(0, message.length() - 2);
            
            throw new IOException(message);
        }
    }
}
