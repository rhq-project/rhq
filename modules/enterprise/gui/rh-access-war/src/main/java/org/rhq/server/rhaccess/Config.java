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

import java.net.URL;

public class Config {

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
        return null;
    }

    public String getProxyPassword() {
        return null;
    }

    public URL getProxyURL() {
        return null;
    }

    public int getProxyPort() {
        return 0;

    }

    public int getSessionTimeout() {
        return 3000000;
    }

    public boolean isDevel() {
        return false;
    }
}
