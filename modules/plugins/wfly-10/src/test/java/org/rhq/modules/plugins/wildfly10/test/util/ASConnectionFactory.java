/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package org.rhq.modules.plugins.wildfly10.test.util;

import org.rhq.modules.plugins.wildfly10.ASConnection;
import org.rhq.modules.plugins.wildfly10.ASConnectionParams;
import org.rhq.modules.plugins.wildfly10.ASConnectionParamsBuilder;

/**
 * @author Thomas Segismont
 */
public class ASConnectionFactory {

    private ASConnectionFactory() {
        // Utility class
    }

    public static ASConnection getStandaloneASConnection() {
        ASConnectionParams asConnectionParams = new ASConnectionParamsBuilder() //
            .setHost(Constants.STANDALONE_HOST) //
            .setPort(Constants.STANDALONE_HTTP_PORT) //
            .setUsername(Constants.MANAGEMENT_USERNAME) //
            .setPassword(Constants.MANAGEMENT_PASSWORD) //
            .createASConnectionParams();
        return new ASConnection(asConnectionParams);
    }

    public static ASConnection getDomainControllerASConnection() {
        ASConnectionParams asConnectionParams = new ASConnectionParamsBuilder() //
            .setHost(Constants.DC_HOST) //
            .setPort(Constants.DC_HTTP_PORT) //
            .setUsername(Constants.MANAGEMENT_USERNAME) //
            .setPassword(Constants.MANAGEMENT_PASSWORD) //
            .createASConnectionParams();
        return new ASConnection(asConnectionParams);
    }
}
