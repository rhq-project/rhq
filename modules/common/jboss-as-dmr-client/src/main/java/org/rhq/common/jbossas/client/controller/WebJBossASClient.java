/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
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
package org.rhq.common.jbossas.client.controller;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.dmr.ModelNode;

/**
 * Convenience methods to access the web management API.
 *
 * @author John Mazzitelli
 */
public class WebJBossASClient extends JBossASClient {

    public static final String SUBSYSTEM_WEB = "web";
    public static final String VIRTUAL_SERVER = "virtual-server";
    public static final String DEFAULT_HOST = "default-host";

    public WebJBossASClient(ModelControllerClient client) {
        super(client);
    }

    /**
     * The enable-welcome-root setting controls whether or not to deploy JBoss' welcome-content application at root context.
     * If you want to deploy your own app at the root context, you need to disable the enable-welcome-root setting
     * on the default host virtual server. If you want to show the JBoss' welcome screen, you need to enable this setting.
     *
     * @param enableFlag true if the welcome screen at the root context should be enabled; false otherwise
     * @throws Exception 
     */
    public void setEnableWelcomeRoot(boolean enableFlag) throws Exception {
        Address address = Address.root().add(SUBSYSTEM, SUBSYSTEM_WEB, VIRTUAL_SERVER, DEFAULT_HOST);
        ModelNode req = createWriteAttributeRequest("enable-welcome-root", Boolean.toString(enableFlag), address);
        ModelNode response = execute(req);
        if (!isSuccess(response)) {
            throw new FailureException(response);
        }
        return;
    }
}
