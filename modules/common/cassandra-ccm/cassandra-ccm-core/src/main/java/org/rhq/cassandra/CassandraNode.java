/*
 *
 *  * RHQ Management Platform
 *  * Copyright (C) 2005-2012 Red Hat, Inc.
 *  * All rights reserved.
 *  *
 *  * This program is free software; you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License, version 2, as
 *  * published by the Free Software Foundation, and/or the GNU Lesser
 *  * General Public License, version 2.1, also as published by the Free
 *  * Software Foundation.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  * GNU General Public License and the GNU Lesser General Public License
 *  * for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * and the GNU Lesser General Public License along with this program;
 *  * if not, write to the Free Software Foundation, Inc.,
 *  * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 */

package org.rhq.cassandra;

import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransportException;

/**
 * @author John Sanda
 */
public class CassandraNode {

    private String hostName;

    private int thriftPort;

    private int nativeTransportPort;

    public CassandraNode(String hostName, int thriftPort, int nativeTransportPort) {
        this.hostName = hostName;
        this.thriftPort = thriftPort;
        this.nativeTransportPort = nativeTransportPort;
    }

    public String getHostName() {
        return hostName;
    }

    public int getThriftPort() {
        return thriftPort;
    }

    public int getNativeTransportPort() {
        return nativeTransportPort;
    }

    public boolean isThrifPortOpen() {
        TSocket socket = new TSocket(this.hostName, this.thriftPort, 100);
        try {
            socket.open();
            socket.close();
            return true;
        } catch (TTransportException e) {
            return false;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CassandraNode that = (CassandraNode) o;

        if (thriftPort != that.thriftPort) return false;
        if (nativeTransportPort != that.nativeTransportPort) return false;
        if (!hostName.equals(that.hostName)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = hostName.hashCode();
        result = 41 * result + thriftPort + nativeTransportPort;
        return result;
    }

    @Override
    public String toString() {
        return "CassandraNode[hostName: " + hostName + ", thriftPort: " + thriftPort + ", nativeTransportPort: " +
            nativeTransportPort + "]";
    }

    public static CassandraNode parseNode(String s) {
        String[] params = s.split("\\|");
        if (params.length != 3) {
            throw new IllegalArgumentException("Expected string of the form, hostname|thriftPort|nativeTransportPort");
        }
        return new CassandraNode(params[0], Integer.parseInt(params[1]), Integer.parseInt(params[2]));
    }
}
