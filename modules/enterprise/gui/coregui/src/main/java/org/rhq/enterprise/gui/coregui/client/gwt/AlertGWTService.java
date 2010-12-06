/*
 * RHQ Management Platform
 * Copyright (C) 2010 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.client.gwt;

import com.google.gwt.user.client.rpc.RemoteService;

import org.rhq.core.domain.alert.Alert;
import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.criteria.AlertCriteria;
import org.rhq.core.domain.util.PageList;

/**
 * @author Joseph Marques
 * @author Ian Springer
 */
public interface AlertGWTService extends RemoteService {
    /**
     * Find all alerts that match the specified criteria.
     *
     * @param criteria the criteria
     *
     * @return all alerts that match the specified criteria
     */
    PageList<Alert> findAlertsByCriteria(AlertCriteria criteria);

    /**
     * Find the count of alerts that match the specified criteria.
     *
     * @param criteria the criteria
     *
     * @return the count of alerts that match the specified criteria
     */
    long findAlertCountByCriteria(AlertCriteria criteria);

    /**
     * Delete the alerts with the specified ids if the current user has permission to do so (i.e. either
     * the MANAGE_INVENTORY global permission, or the MANAGE_ALERTS permission for all corresponding resources).
     * If the user does not have permission for all of the specified alerts, then none of the alerts will be deleted
     * and a PermissionException will be thrown.
     *
     * If any of the ids do not correspond to alert entities that exist, those ids will be gracefully ignored.
     *
     * @param alertIds the ids of the alerts to be deleted
     * @return the number of alerts deleted
     */
    int deleteAlerts(int[] alertIds);

    /**
     * Deletes all alerts for the given context if the current user has permission to do so (i.e., either
     * the MANAGE_INVENTORY global permission, or the MANAGE_ALERTS permission for all corresponding resources).
     * If the user does not have permission for all of the specified alerts, then non of the laerts will be deleted
     * and a PermissionException will be thrown.
     * 
     * @param context represents a specific resource, all resource members of some group, or all resources visible to 
     *        the user
     * @return the number of alerts deleted
     */
    int deleteAlertsByContext(EntityContext context);

    /**
     * Acknowledges the alerts with the specified ids if the current user has permission to do so (i.e., either
     * the MANAGE_INVENTORY global permission, or the MANAGE_ALERTS permission for all corresponding resources).
     * If the user does not have permission for all of the specified alerts, then non of the laerts will be deleted
     * and a PermissionException will be thrown.
     * 
     * If any of the ids do not correspond to alert entities that exist, those ids will be gracefully ignored.
     *
     * @param alertIds the ids of the alerts to be acknowledged
     * @return the number of alerts acknowledged
     */
    int acknowledgeAlerts(int[] alertIds);

    /**
     * Acknowledges all alerts for the given context if the current user has permission to do so (i.e., either
     * the MANAGE_INVENTORY global permission, or the MANAGE_ALERTS permission for all corresponding resources).
     * If the user does not have permission for all of the specified alerts, then non of the laerts will be deleted
     * and a PermissionException will be thrown.
     * 
     * @param context represents a specific resource, all resource members of some group, or all resources visible to 
     *        the user
     * @return the number of alerts acknowledged
     */
    int acknowledgeAlertsByContext(EntityContext context);
}