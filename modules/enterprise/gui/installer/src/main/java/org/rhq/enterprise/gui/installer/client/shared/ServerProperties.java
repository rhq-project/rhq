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
package org.rhq.enterprise.gui.installer.client.shared;

/**
 * Settings found in the rhq-server.properties file that controls the startup configuration of the server.
 *
 * @author John Mazzitelli
 */
public class ServerProperties {
    public static final String PREFIX_PROP_DATABASE = "rhq.server.database.";
    public static final String PROP_DATABASE_TYPE = PREFIX_PROP_DATABASE + "type-mapping";
    public static final String PROP_DATABASE_CONNECTION_URL = PREFIX_PROP_DATABASE + "connection-url";
    public static final String PROP_DATABASE_USERNAME = PREFIX_PROP_DATABASE + "user-name";
    public static final String PROP_DATABASE_PASSWORD = PREFIX_PROP_DATABASE + "password";
    public static final String PROP_DATABASE_SERVER_NAME = PREFIX_PROP_DATABASE + "server-name";
    public static final String PROP_DATABASE_PORT = PREFIX_PROP_DATABASE + "port";
    public static final String PROP_DATABASE_DB_NAME = PREFIX_PROP_DATABASE + "db-name";
    public static final String PROP_DATABASE_HIBERNATE_DIALECT = "hibernate.dialect";
    public static final String PROP_QUARTZ_DRIVER_DELEGATE_CLASS = "rhq.server.quartz.driverDelegateClass";
    public static final String PROP_QUARTZ_SELECT_WITH_LOCK_SQL = "rhq.server.quartz.selectWithLockSQL";
    public static final String PROP_QUARTZ_LOCK_HANDLER_CLASS = "rhq.server.quartz.lockHandlerClass";

    public static final String PREFIX_PROP_WEB = "rhq.server.startup.web.";
    public static final String PROP_WEB_HTTP_PORT = PREFIX_PROP_WEB + "http.port";
    public static final String PROP_WEB_HTTPS_PORT = PREFIX_PROP_WEB + "https.port";

    public static final String PREFIX_PROP_EMBEDDED_AGENT = "rhq.server.embedded-agent.";
    public static final String PROP_EMBEDDED_AGENT_ENABLED = PREFIX_PROP_EMBEDDED_AGENT + "enabled";

    public static final String PREFIX_PROP_EMAIL = "rhq.server.email.";
    public static final String PROP_EMAIL_SMTP_HOST = PREFIX_PROP_EMAIL + "smtp-host";
    public static final String PROP_EMAIL_FROM_ADDRESS = PREFIX_PROP_EMAIL + "from-address";

    public static final String PROP_HIGH_AVAILABILITY_NAME = "rhq.server.high-availability.name";
    public static final String PROP_MM_AT_START = "rhq.server.maintenance-mode-at-startup";
}