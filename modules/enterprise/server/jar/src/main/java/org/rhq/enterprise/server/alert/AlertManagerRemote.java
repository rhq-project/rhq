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
package org.rhq.enterprise.server.alert;

import javax.ejb.Remote;

import org.rhq.core.domain.alert.Alert;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.criteria.AlertCriteria;
import org.rhq.core.domain.util.PageList;

/**
 * Alert Manager Remote API.
 */
@Remote
public interface AlertManagerRemote {

    /**
     * @param subject
     * @param criteria
     * @return no null
     */
    PageList<Alert> findAlertsByCriteria(Subject subject, AlertCriteria criteria);

    /**
     * @param subject
     * @param alertIds
     * @return number of deleted alerts
     */
    int deleteAlerts(Subject subject, int[] alertIds);

    /**
     * Requires Manage Settings permission.
     *
     * @param subject
     * @param context
     * @return number of deleted alerts
     */
    int deleteAlertsByContext(Subject subject, EntityContext context);

    /**
     * @param subject
     * @param alertIds
     * @return number of acknowledged alerts
     */
    int acknowledgeAlerts(Subject subject, int[] alertIds);

    /**
     * @param subject
     * @param context
     * @return number of acknowledged alerts
     */
    int acknowledgeAlertsByContext(Subject subject, EntityContext context);

}
