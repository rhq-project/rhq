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
import org.rhq.enterprise.gui.coregui.client.gwt.AlertDefinitionGWTService;
import org.rhq.enterprise.gui.coregui.server.util.SerialUtility;
import org.rhq.enterprise.server.alert.AlertDefinitionManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

public class AlertDefinitionGWTServiceImpl extends AbstractGWTServiceImpl implements AlertDefinitionGWTService {
    private static final long serialVersionUID = 1L;

    private AlertDefinitionManagerLocal alertDefManager = LookupUtil.getAlertDefinitionManager();

    public AlertDefinition updateAlertDefinition(int alertDefinitionId, AlertDefinition alertDefinition,
        boolean updateInternals) throws Exception {
        try {
            AlertDefinition results = alertDefManager.updateAlertDefinition(getSessionSubject(), alertDefinitionId,
                alertDefinition, updateInternals);
            return SerialUtility.prepare(results, "updateAlertDefinition");
        } catch (Exception e) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(e));
        }
    }

    public int enableAlertDefinitions(Integer[] alertDefinitionIds) throws Exception {
        try {
            int results = alertDefManager.enableAlertDefinitions(getSessionSubject(), alertDefinitionIds);
            return results;
        } catch (Exception e) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(e));
        }
    }

    public int disableAlertDefinitions(Integer[] alertDefinitionIds) throws Exception {
        try {
            int results = alertDefManager.disableAlertDefinitions(getSessionSubject(), alertDefinitionIds);
            return results;
        } catch (Exception e) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(e));
        }
    }

    public int removeAlertDefinitions(Integer[] alertDefinitionIds) throws Exception {
        try {
            int results = alertDefManager.removeAlertDefinitions(getSessionSubject(), alertDefinitionIds);
            return results;
        } catch (Exception e) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(e));
        }
    }
}