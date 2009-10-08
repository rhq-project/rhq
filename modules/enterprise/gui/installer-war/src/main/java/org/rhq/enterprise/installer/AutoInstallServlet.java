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
package org.rhq.enterprise.installer;

import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

/**
 * This checks to see if the server is already preconfigured. If it is, it kicks off an auto-install, 
 * thus allowing the server to complete its installation without human intervention.
 * 
 * This assumes rhq-server.properties is already preconfigured.
 * 
 * @author John Mazzitelli
 */
public class AutoInstallServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    public void init() throws ServletException {
        ServerInformation serverInfo = new ServerInformation();
        Properties serverProperties = serverInfo.getServerProperties();
        String enabledString = serverProperties.getProperty(ServerProperties.PROP_AUTOINSTALL_ENABLE);
        boolean enabled = Boolean.parseBoolean(enabledString);
        if (enabled) {
            log("Server is preconfigured - performing auto-install immediately...");
            try {
                // this server has been preconfigured. We need to run through the auto-installation now
                ConfigurationBean configBean = new ConfigurationBean();
                configBean.setHaServerFromPropertiesOnly();
                StartPageResults results = configBean.save();
                if (results != StartPageResults.SUCCESS) {
                    throw new Exception("Save failed - check logs for error messages");
                }
            } catch (Throwable t) {
                log("The server was preconfigured but it failed to auto-install!", t);
                throw new ServletException(t);
            }
            log("Server has been auto-installed and should be ready shortly.");
        } else {
            log("Server is not preconfigured - will not perform auto-install. Please perform a manual install.");
        }

        return;
    }
}
