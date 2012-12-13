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

package org.rhq.cassandra.auth;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.EnumSet;
import java.util.List;
import java.util.Properties;

import org.apache.cassandra.auth.AuthenticatedUser;
import org.apache.cassandra.auth.LegacyAuthorizer;
import org.apache.cassandra.auth.Permission;
import org.apache.cassandra.auth.Resources;
import org.apache.cassandra.exceptions.ConfigurationException;
import org.apache.cassandra.io.util.FileUtils;

/**
 * @author John Sanda
 */
public class SimpleAuthorizer extends LegacyAuthorizer {

    public final static String ACCESS_FILENAME_PROPERTY = "access.properties";
    // magical property for WRITE permissions to the keyspaces list
    public final static String KEYSPACES_WRITE_PROPERTY = "<modify-keyspaces>";

    public EnumSet<Permission> authorize(AuthenticatedUser user, List<Object> resource) {
        if (resource.size() < 2 || !Resources.ROOT.equals(resource.get(0)) ||
            !Resources.KEYSPACES.equals(resource.get(1)))
            return EnumSet.copyOf(Permission.NONE);

        String keyspace, columnFamily = null;
//        EnumSet<Permission> authorized = EnumSet.copyOf(Permission.NONE);
        EnumSet<Permission> authorized = EnumSet.noneOf(Permission.class);

        // /cassandra/keyspaces
        if (resource.size() == 2) {
            keyspace = KEYSPACES_WRITE_PROPERTY;
            authorized = EnumSet.of(Permission.READ);
        }
        // /cassandra/keyspaces/<keyspace name>
        else if (resource.size() == 3) {
            keyspace = (String) resource.get(2);
        }
        // /cassandra/keyspaces/<keyspace name>/<cf name>
        else if (resource.size() == 4) {
            keyspace = (String) resource.get(2);
            columnFamily = (String) resource.get(3);
        } else {
            // We don't currently descend any lower in the hierarchy.
            throw new UnsupportedOperationException();
        }

        String accessFilename = System.getProperty(ACCESS_FILENAME_PROPERTY);
        InputStream in = null;
        try {
            in = new BufferedInputStream(new FileInputStream(accessFilename));
            Properties accessProperties = new Properties();
            accessProperties.load(in);

            // Special case access to the keyspace list
            if (keyspace == KEYSPACES_WRITE_PROPERTY) {
                String kspAdmins = accessProperties.getProperty(KEYSPACES_WRITE_PROPERTY);
                for (String admin : kspAdmins.split(","))
                    if (admin.equals(user.username))
                        return EnumSet.copyOf(Permission.ALL);
            }

            boolean canRead = false, canWrite = false;
            String readers = null, writers = null;

            if (columnFamily == null) {
                readers = accessProperties.getProperty(keyspace + ".<ro>");
                writers = accessProperties.getProperty(keyspace + ".<rw>");
            } else {
                readers = accessProperties.getProperty(keyspace + "." + columnFamily + ".<ro>");
                writers = accessProperties.getProperty(keyspace + "." + columnFamily + ".<rw>");
            }

            if (readers != null) {
                for (String reader : readers.split(",")) {
                    if (reader.equals(user.username)) {
                        canRead = true;
                        break;
                    }
                }
            }

            if (writers != null) {
                for (String writer : writers.split(",")) {
                    if (writer.equals(user.username)) {
                        canWrite = true;
                        break;
                    }
                }
            }

            if (canWrite)
                authorized = EnumSet.copyOf(Permission.ALL);
            else if (canRead)
                authorized = EnumSet.of(Permission.READ);

        } catch (IOException e) {
            throw new RuntimeException(String.format("Authorization table file '%s' could not be opened: %s",
                accessFilename,
                e.getMessage()));
        } finally {
            FileUtils.closeQuietly(in);
        }

        return authorized;
    }

    public void validateConfiguration() throws ConfigurationException {
        String afilename = System.getProperty(ACCESS_FILENAME_PROPERTY);
        if (afilename == null) {
            throw new ConfigurationException(String.format("When using %s, '%s' property must be defined.",
                this.getClass().getCanonicalName(),
                ACCESS_FILENAME_PROPERTY));
        }
    }
}
