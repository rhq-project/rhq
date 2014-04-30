/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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
package org.rhq.enterprise.server.plugins.alertUps;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.enterprise.server.plugin.pc.ServerPluginComponent;
import org.rhq.enterprise.server.plugin.pc.ServerPluginContext;

/**
 * Persistent component used to send alert notifications via Aerogear
 * Unified Push Server
 *
 * @author Heiko W. Rupp
 */
public class UpsAlertComponent implements ServerPluginComponent {

    private final Log log = LogFactory.getLog(UpsAlertComponent.class);

    String targetHost;
    String masterSecret;
    String pushId;
    int port;

    public void initialize(ServerPluginContext context) throws Exception {
        Configuration preferences = context.getPluginConfiguration();

        targetHost = preferences.getSimpleValue("server");
        masterSecret = preferences.getSimpleValue("masterSecret");
        pushId = preferences.getSimpleValue("pushId");
        String tmp = preferences.getSimpleValue("port","8080");
        port = Short.valueOf(tmp);
    }


    public void start() {
    }

    public void stop() {

    }

    public void shutdown() {
    }

}
