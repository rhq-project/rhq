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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.resource.InventoryStatus;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.util.PageControl;
import org.rhq.enterprise.gui.legacy.Constants;
import org.rhq.enterprise.gui.legacy.MessageConstants;
import org.rhq.enterprise.gui.legacy.action.BaseAction;
import org.rhq.enterprise.gui.legacy.util.SessionUtils;
import org.rhq.enterprise.server.discovery.DiscoveryBossLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * This is the action triggered when selecting resources to be imported into inventory or ignored.
 */
public class ProcessQueueAction extends BaseAction {
    @Override
    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        DiscoveryBossLocal discovery = LookupUtil.getDiscoveryBoss();
        Subject user = SessionUtils.getWebUser(request.getSession()).getSubject();

        AIQueueForm queueForm = (AIQueueForm) form;
        Integer[] platformIds = queueForm.getPlatformsToProcess();
        Integer[] serverIds = queueForm.getServersToProcess();
        int queueAction = queueForm.getQueueAction();
        boolean isApproval = (queueAction == AIQueueForm.Q_DECISION_APPROVE);
        boolean isIgnore = (queueAction == AIQueueForm.Q_DECISION_IGNORE);
        if (!isApproval && !isIgnore) {
            throw new IllegalStateException("Illegal queue action id: " + queueAction);
        }

        List<Resource> selectedPlatforms = new ArrayList<Resource>();
        List<Resource> selectedServers = new ArrayList<Resource>();

        // Grab a fresh view of all platforms and servers with a status of 'NEW'.
        // TODO: don't use unlimited, try to use getQueuedPlatforms to get only those we care about
        Map<Resource, List<Resource>> queuedResources = discovery.getQueuedPlatformsAndServers(user, PageControl
            .getUnlimitedInstance());

        // perform some preprocessing, to make sure selections are valid
        String errorKey = null;
        for (Resource platform : queuedResources.keySet()) {
            if (isIgnore) {
                // March 05, 2009 - to date, it is illegal to ignore servers if the platform hasn't yet been committed
                if (platform.getInventoryStatus() == InventoryStatus.NEW) {
                    errorKey = MessageConstants.ERR_PLATFORM_NOT_COMMITTED;
                    break; // don't process any more platforms
                }
            }
        }
        if (errorKey != null) {
            // premature return, since we know subsequent inventory operations will fail
            //RequestUtils.setError(request, errorKey);
            SessionUtils.setError(request.getSession(), errorKey);
            return returnSuccess(request, mapping);
        }

        for (Resource platform : queuedResources.keySet()) {
            if (!selectedForProcessing(platform, platformIds)) {
                continue;
            }

            // we want to process one platform at a time, so clear any previous platform data
            selectedPlatforms.clear();
            selectedServers.clear();

            // only add the platform if we are approving it
            // you can't ignore/remove a platform (just don't run the agent if you want that!)
            if (isApproval) {
                selectedPlatforms.add(platform);
            }

            // Now check servers on this platform
            for (Resource server : queuedResources.get(platform)) {
                if (isSelectedForProcessing(server, serverIds)) {
                    selectedServers.add(server);

                    // If we're approving stuff, and this platform's not already in the list, add it.
                    if (isApproval && !selectedPlatforms.contains(platform)) {
                        selectedPlatforms.add(platform);
                    }
                }
            }

            // update one platform at a time, so they each get their own transaction
            try {
                InventoryStatus status = isApproval ? InventoryStatus.COMMITTED : InventoryStatus.IGNORED;

                // update the inventory status for all selected resources and tell their agents to pull down their schedules
                discovery.updateInventoryStatus(user, selectedPlatforms, selectedServers, status);
            } catch (Exception e) {
                request.getSession().setAttribute(Constants.IMPORT_ERROR_ATTR, e);
                break; // don't process any more platforms
            }
        }

        return returnSuccess(request, mapping);
    }

    private boolean selectedForProcessing(Resource platform, Integer[] platformsToProcess) {
        Integer id = platform.getId();
        for (Integer platformToProcess : platformsToProcess) {
            if (platformToProcess.equals(id)) {
                return true;
            }
        }

        return false;
    }

    private boolean isSelectedForProcessing(Resource server, Integer[] serversToProcess) {
        Integer id = server.getId();
        for (Integer serverToProcess : serversToProcess) {
            if (serverToProcess.equals(id)) {
                return true;
            }
        }

        return false;
    }
}