/*
 * RHQ Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
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
package org.rhq.enterprise.server.plugins.alertEmail;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.enterprise.server.plugin.pc.ServerPluginComponent;
import org.rhq.enterprise.server.plugin.pc.ServerPluginContext;

/**
 * Stateful component class of this plugin
 * @author Heiko W. Rupp
 */
public class EmailAlertComponent implements ServerPluginComponent {

    private final Log log = LogFactory.getLog(EmailAlertComponent.class);

    public void start() {
        log.info("Alert:Email - start()");
    }

    public void initialize(ServerPluginContext context) throws Exception {
        log.info("Alert:Email - initialize()");
    }

    public void shutdown() {
        log.info("Alert:Email - shutdown");
    }

    public void stop() {
        log.info("Alert:Email - stop");
    }
}
