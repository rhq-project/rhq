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

package org.rhq.server.rhaccess;

import java.net.MalformedURLException;
import java.net.URL;

import org.apache.log4j.Logger;

import org.rhq.core.domain.common.composite.SystemSetting;
import org.rhq.core.domain.common.composite.SystemSettings;
import org.rhq.enterprise.server.system.SystemManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

public class Config {

    private final static Logger log = Logger.getLogger(Config.class);

    private final SystemSettings settings;

    public Config() {
        SystemManagerLocal systemManager = LookupUtil.getSystemManager();
        settings = systemManager.getSystemSettings(LookupUtil.getSubjectManager().getOverlord());
    }
    /**
     * return same value as defined in WEB-INF/support.html
     * @return version of rh-access-plugin being sent to RHA
     */
    public String getUserAgent() {
        return "redhat-access-plugin-jon-1.0.4";
    }

    public boolean isBrokered() {
        return true;
    }

    public String getURL() {
        return "https://api.access.redhat.com";
    }

    public String getProxyUser() {
        return settings.get(SystemSetting.PROXY_SERVER_USERNAME);
    }

    public String getProxyPassword() {
        return settings.get(SystemSetting.PROXY_SERVER_PASSWORD);
    }

    public URL getProxyURL() {
        try {
            String url = settings.get(SystemSetting.PROXY_SERVER_HOST);
            if (url == null) {
                return null;
            }
            return new URL(url);
        } catch (MalformedURLException e) {
            log.error("Unable to parse PROXY_SERVER_HOST setting to URL", e);
            return null;
        }
    }

    public int getProxyPort() {
        String port = settings.get(SystemSetting.PROXY_SERVER_PORT);
        if (port == null) {
            port = "0";
        }
        return Integer.parseInt(port);

    }

    public int getSessionTimeout() {
        return 3000000;
    }

    public boolean isDevel() {
        return false;
    }
}
