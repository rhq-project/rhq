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

package org.rhq.plugins.mysql;

/**
 * A class to act as a key to a specific MySQL connection
 * @author Steve Millidge (C2B2 Consulting Limited)
 */
class MySqlConnectionInfo {

    private String host;
    private String port;
    private String db;
    private String user;
    private String password;
    private int hashCode;

    MySqlConnectionInfo(String host, String port, String db, String user, String password ) {
        this.host = host;
        this.port = port;
        this.db = db;
        this.user = user;
        this.password = password;
        this.hashCode = new StringBuilder().append(host).
                append(port).
                append(db).
                append(user).
                append(password).toString().hashCode();

    }

    public String getDb() {
        return db;
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    public String getHost() {
        return host;
    }

    public String getPassword() {
        return password;
    }

    public String getPort() {
        return port;
    }

    public String getUser() {
        return user;
    }

    public String buildURL() {
        return new StringBuilder().append("jdbc:mysql://")
                .append(host)
                .append(":")
                .append(port)
                .append("/")
                .append(db).toString();
    }

    @Override
    public boolean equals(Object other) {
        boolean result = false;
        if ((other instanceof MySqlConnectionInfo) && (other.hashCode() == this.hashCode())) {
            result = true;
        }
        return result;
    }

}
