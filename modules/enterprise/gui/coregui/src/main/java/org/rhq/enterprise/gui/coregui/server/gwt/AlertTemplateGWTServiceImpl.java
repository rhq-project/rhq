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
package org.rhq.enterprise.gui.coregui.server.gwt;

import org.rhq.core.domain.alert.AlertDefinition;
import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.enterprise.gui.coregui.client.gwt.AlertTemplateGWTService;
import org.rhq.enterprise.gui.coregui.server.util.SerialUtility;
import org.rhq.enterprise.server.alert.AlertTemplateManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

public class AlertTemplateGWTServiceImpl extends AbstractGWTServiceImpl implements AlertTemplateGWTService {
    private static final long serialVersionUID = 1L;

    private AlertTemplateManagerLocal alertTemplateManager = LookupUtil.getAlertTemplateManager();

    @Override
    public int createAlertTemplate(AlertDefinition alertDefinition, Integer resourceTypeId) throws Exception {
        try {
            int results = alertTemplateManager
                .createAlertTemplate(getSessionSubject(), alertDefinition, resourceTypeId);
            return results;
        } catch (Exception e) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(e));
        }
    }

    @Override
    public AlertDefinition updateAlertTemplate(AlertDefinition alertDefinition, boolean purgeInternals)
        throws Exception {
        try {
            AlertDefinition results = alertTemplateManager.updateAlertTemplate(getSessionSubject(), alertDefinition,
                purgeInternals);
            return SerialUtility.prepare(results, "AlertTemplateService.updateAlertTemplate");
        } catch (Exception e) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(e));
        }
    }

    @Override
    public void enableAlertTemplates(Integer[] alertDefinitionIds) throws Exception {
        try {
            alertTemplateManager.enableAlertTemplates(getSessionSubject(), alertDefinitionIds);
            return;
        } catch (Exception e) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(e));
        }
    }

    @Override
    public void disableAlertTemplates(Integer[] alertDefinitionIds) throws Exception {
        try {
            alertTemplateManager.disableAlertTemplates(getSessionSubject(), alertDefinitionIds);
            return;
        } catch (Exception e) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(e));
        }
    }

    @Override
    public void removeAlertTemplates(Integer[] alertDefinitionIds) throws Exception {
        try {
            alertTemplateManager.removeAlertTemplates(getSessionSubject(), alertDefinitionIds);
            return;
        } catch (Exception e) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(e));
        }
    }
}