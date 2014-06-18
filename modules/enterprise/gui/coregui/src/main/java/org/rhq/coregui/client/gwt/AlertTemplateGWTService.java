/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
package org.rhq.coregui.client.gwt;

import com.google.gwt.user.client.rpc.RemoteService;

import org.rhq.core.domain.alert.AlertDefinition;

public interface AlertTemplateGWTService extends RemoteService {

    int createAlertTemplate(AlertDefinition alertDefinition, Integer resourceTypeId) throws RuntimeException;

    /**
     * Updates a alert template definition.
     * 
     * @param alertDefinition
     * @param resetMatching must be true if you are updating conditions, condition expression, or dampening settings, can be false otherwise
     * @return the updated definition
     * @throws Exception
     */
    AlertDefinition updateAlertTemplate(AlertDefinition alertDefinition, boolean resetMatching)
        throws RuntimeException;
}