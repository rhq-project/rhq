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

/**
 * @author John Sanda
 */
public class CassandraNode {

    private String hostName;

    private int thriftPort;

    public CassandraNode(String hostName, int thriftPort) {
        this.hostName = hostName;
        this.thriftPort = thriftPort;
    }

    public String getHostName() {
        return hostName;
    }

    public int getThriftPort() {
        return thriftPort;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CassandraNode that = (CassandraNode) o;

        if (thriftPort != that.thriftPort) return false;
        if (!hostName.equals(that.hostName)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = hostName.hashCode();
        result = 41 * result + thriftPort;
        return result;
    }

    @Override
    public String toString() {
        return "CassandraNode[hostName: " + hostName + ", thriftPort: " + thriftPort + "]";
    }
}
