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
package org.rhq.enterprise.gui.content;

import javax.servlet.http.HttpServletRequest;
import org.rhq.core.domain.content.InstalledPackageHistory;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.enterprise.server.content.ContentUIManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * Used to load a particular entry in the package audit trail for a resource.
 *
 * @author Jason Dobies
 */
public class AuditTrailItemUIBean {

    private InstalledPackageHistory history;
    private int selectedHistoryId;

    public InstalledPackageHistory getHistory() {
        if (history == null) {
            ContentUIManagerLocal contentUIManager = LookupUtil.getContentUIManager();
            history = contentUIManager.getInstalledPackageHistory(getSelectedHistoryId());
        }

        return history;
    }

    public void setHistory(InstalledPackageHistory history) {
        this.history = history;
    }

    public int getSelectedHistoryId() {
        if (selectedHistoryId == 0) {
            HttpServletRequest request = FacesContextUtility.getRequest();
            selectedHistoryId = Integer.parseInt(request.getParameter("selectedHistoryId"));
        }

        return selectedHistoryId;
    }

    public void setSelectedHistoryId(int selectedHistoryId) {
        this.selectedHistoryId = selectedHistoryId;
    }


}
