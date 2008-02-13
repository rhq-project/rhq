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
package org.rhq.enterprise.gui.legacy.portlet.autodiscovery;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;

public class AIQueueForm extends ActionForm {
    public static final int Q_DECISION_APPROVE = 1;
    public static final int Q_DECISION_IGNORE = 2;

    private Integer[] platformsToProcess;
    private Integer[] serversToProcess;
    private int queueAction;

    public AIQueueForm() {
        super();
    }

    public Integer[] getPlatformsToProcess() {
        return platformsToProcess;
    }

    public void setPlatformsToProcess(Integer[] platforms) {
        platformsToProcess = platforms;
    }

    public Integer[] getServersToProcess() {
        return serversToProcess;
    }

    public void setServersToProcess(Integer[] servers) {
        serversToProcess = servers;
    }

    public int getQueueAction() {
        return queueAction;
    }

    public void setQueueAction(int action) {
        queueAction = action;
    }

    @Override
    public void reset(ActionMapping mapping, HttpServletRequest request) {
        reset();
    }

    @Override
    public void reset(ActionMapping mapping, ServletRequest request) {
        reset();
    }

    private void reset() {
        platformsToProcess = new Integer[0];
        serversToProcess = new Integer[0];
    }
}