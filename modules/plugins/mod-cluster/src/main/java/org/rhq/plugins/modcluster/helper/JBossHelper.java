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

package org.rhq.plugins.modcluster.helper;

import org.mc4j.ems.connection.bean.EmsBean;

/**
 * @author Stefan Negrea
 *
 */
public class JBossHelper {

    public static String getRawProxyInfo(EmsBean emsBean) {
        String rawProxyInfo = null;

        //try first to get the value for JBoss EAP5.x or AS6
        try {
            rawProxyInfo = (String) emsBean.getAttribute("ProxyInfo").refresh().toString();
        } catch (Exception e) {
        }

        //if not able to get the value after the first attempt try to get the value for JBoss 4.2/4.3
        if (rawProxyInfo == null) {
            try {
                rawProxyInfo = (String) emsBean.getAttribute("proxyInfo").refresh().toString();
            } catch (Exception e) {
            }
        }

        return rawProxyInfo;
    }
}
