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

import java.util.List;
import java.util.ArrayList;
import javax.servlet.http.HttpServletRequest;
import javax.faces.component.UIData;
import javax.faces.model.SelectItem;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.domain.content.InstalledPackageHistory;
import org.rhq.core.domain.content.PackageInstallationStep;
import org.rhq.core.domain.content.PackageVersion;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.enterprise.server.util.LookupUtil;
import org.rhq.enterprise.server.content.ContentUIManagerLocal;

/**
 * Bean used to provide details on a specific entry in the package audit trail ({@link InstalledPackageHistory}).
 *
 * @author Jason Dobies
 */
public class ShowInstalledPackageHistoryUIBean {

    private InstalledPackageHistory history;
    private int selectedHistoryId;

    private List<PackageInstallationStep> installationSteps;
    private UIData stepsData;

    private PackageInstallationStep step;

    private final Log log = LogFactory.getLog(this.getClass());

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

    public PackageInstallationStep getStep() {
        if (step == null) {
            HttpServletRequest request = FacesContextUtility.getRequest();
            int stepId = Integer.parseInt(request.getParameter("stepId"));

            ContentUIManagerLocal contentUIManager = LookupUtil.getContentUIManager();
            step = contentUIManager.getPackageInstallationStep(stepId);
        }

        return step;
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

    public List<PackageInstallationStep> getInstallationSteps() {
        if (installationSteps == null) {
            InstalledPackageHistory history = getHistory();

            if (history == null) {
                log.error("Trying to load steps for null history");
                return installationSteps;
            }

            ContentUIManagerLocal contentUIManager = LookupUtil.getContentUIManager();
            installationSteps = contentUIManager.getPackageInstallationSteps(history.getId()); 
        }

        return installationSteps;
    }

    public void setInstallationSteps(List<PackageInstallationStep> installationSteps) {
        this.installationSteps = installationSteps;
    }

    public UIData getStepsData() {
        return stepsData;
    }

    public void setStepsData(UIData stepsData) {
        this.stepsData = stepsData;
    }
}
