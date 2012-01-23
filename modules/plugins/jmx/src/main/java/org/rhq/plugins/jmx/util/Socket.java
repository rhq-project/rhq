/*
 * RHQ Management Platform
 * Copyright (C) 2011-2012 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */

package org.rhq.plugins.jmx.util;

/**
 * @author Ian Springer
 */
public class Socket implements Comparable<Socket> {
    
    private static final String STRING_DELIMITER = "/";

    private Protocol protocol;
    private String host; // either an IP address or a host name
    private long port;

    public static Socket valueOf(String string) {
        String[] values = string.split(STRING_DELIMITER);
        Protocol protocol = Protocol.valueOf(values[0]);
        String host = values[1];
        long port = Long.valueOf(values[2]);

        return new Socket(protocol, host, port);
    }

    public Socket(Protocol protocol, String host, long port) {
        if (protocol == null) {
            throw new IllegalArgumentException("protocol must be non-null.");
        }
        if (host == null) {
            throw new IllegalArgumentException("host must be non-null.");
        }
        if (port < 0 || port > 65535) {
            throw new IllegalArgumentException("invalid port: " + port);
        }

        this.protocol = protocol;
        this.host = host;
        this.port = port;
    }

    public Protocol getProtocol() {
        return protocol;
    }

    public String getHost() {
        return host;
    }

    public long getPort() {
        return port;
    }

    public enum Protocol { TCP, UDP }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Socket that = (Socket) o;

        if (port != that.port) return false;
        if (!host.equals(that.host)) return false;
        if (protocol != that.protocol) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = protocol.hashCode();
        result = 31 * result + host.hashCode();
        result = 31 * result + (int) (port ^ (port >>> 32));
        return result;
    }

    @Override
    public String toString() {
        return this.protocol + STRING_DELIMITER + this.host + STRING_DELIMITER + this.port;
    }

    @Override
    public int compareTo(Socket that) {
        if (this == that) {
            return 0;
        }

        int result = this.protocol.compareTo(that.protocol);
        if (result == 0) {
            result = this.host.compareTo(that.host);
            if (result == 0) {
                result = Long.valueOf(this.port).compareTo(that.port);

            }
        }
        return result;
    }

}
