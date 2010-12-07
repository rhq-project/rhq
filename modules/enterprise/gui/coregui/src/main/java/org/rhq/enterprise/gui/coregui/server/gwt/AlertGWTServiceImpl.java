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
package org.rhq.enterprise.gui.coregui.server.gwt;

import org.rhq.core.domain.alert.Alert;
import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.criteria.AlertCriteria;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.enterprise.gui.coregui.client.gwt.AlertGWTService;
import org.rhq.enterprise.gui.coregui.server.util.SerialUtility;
import org.rhq.enterprise.server.alert.AlertManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * @author Ian Springer
 * @author Joseph Marques
 */
public class AlertGWTServiceImpl extends AbstractGWTServiceImpl implements AlertGWTService {
    private static final long serialVersionUID = 1L;

    private AlertManagerLocal alertManager = LookupUtil.getAlertManager();

    public PageList<Alert> findAlertsByCriteria(AlertCriteria criteria) {
        try {
            return SerialUtility.prepare(this.alertManager.findAlertsByCriteria(getSessionSubject(), criteria),
                "AlertService.findAlertsByCriteria");
        } catch (Exception e) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(e));
        }
    }

    public int deleteAlerts(int[] alertIds) {
        try {
            return this.alertManager.deleteAlerts(getSessionSubject(), alertIds);
        } catch (Exception e) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(e));
        }
    }

    public int deleteAlertsByContext(EntityContext context) {
        try {
            return this.alertManager.deleteAlertsByContext(getSessionSubject(), context);
        } catch (Exception e) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(e));
        }
    }

    public int acknowledgeAlerts(int[] alertIds) {
        try {
            return this.alertManager.acknowledgeAlerts(getSessionSubject(), alertIds);
        } catch (Exception e) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(e));
        }
    }

    public int acknowledgeAlertsByContext(EntityContext context) {
        try {
            return this.alertManager.acknowledgeAlertsByContext(getSessionSubject(), context);
        } catch (Exception e) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(e));
        }
    }
}