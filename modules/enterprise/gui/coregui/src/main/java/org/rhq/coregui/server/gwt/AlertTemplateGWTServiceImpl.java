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
package org.rhq.coregui.server.gwt;

import org.rhq.core.domain.alert.AlertDefinition;
import org.rhq.coregui.client.gwt.AlertTemplateGWTService;
import org.rhq.coregui.server.util.SerialUtility;
import org.rhq.enterprise.server.alert.AlertTemplateManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

public class AlertTemplateGWTServiceImpl extends AbstractGWTServiceImpl implements AlertTemplateGWTService {
    private static final long serialVersionUID = 1L;

    private AlertTemplateManagerLocal alertTemplateManager = LookupUtil.getAlertTemplateManager();

    @Override
    public int createAlertTemplate(AlertDefinition alertDefinition, Integer resourceTypeId) throws RuntimeException {
        try {
            int results = alertTemplateManager
                .createAlertTemplate(getSessionSubject(), alertDefinition, resourceTypeId);
            return results;
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public AlertDefinition updateAlertTemplate(AlertDefinition alertDefinition, boolean resetMatching)
        throws RuntimeException {
        try {
            AlertDefinition results = alertTemplateManager.updateAlertTemplate(getSessionSubject(), alertDefinition,
                resetMatching);
            return SerialUtility.prepare(results, "AlertTemplateService.updateAlertTemplate");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }
}