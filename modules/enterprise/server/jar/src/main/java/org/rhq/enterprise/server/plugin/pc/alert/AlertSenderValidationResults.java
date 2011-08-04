/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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

package org.rhq.enterprise.server.plugin.pc.alert;

import org.rhq.core.domain.configuration.Configuration;

/**
 * 
 *
 * @author Lukas Krejci
 */
public class AlertSenderValidationResults {

    private Configuration alertParameters;
    private Configuration extraParameters;
    
    public AlertSenderValidationResults(Configuration alertParameters, Configuration extraParameters) {
        this.alertParameters = alertParameters == null ? null : alertParameters.deepCopy();
        this.extraParameters = extraParameters == null ? null : extraParameters.deepCopy();
    }
    
    public Configuration getAlertParameters() {
        return alertParameters;
    }
    
    public Configuration getExtraParameters() {
        return extraParameters;
    }
}
