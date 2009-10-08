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
import javax.faces.component.UIData;
import javax.faces.application.FacesMessage;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.domain.content.transfer.DeployPackageStep;
import org.rhq.core.domain.content.transfer.ResourcePackageDetails;
import org.rhq.core.domain.content.PackageVersion;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.enterprise.server.content.ContentUIManagerLocal;
import org.rhq.enterprise.server.content.ContentManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;

/**
 * Bean responsible for making the request to an agent for installation steps on a particular package. Currently,
 * the steps are cached in this bean after the call to the agent. If the user makes another requst to translate steps,
 * the steps from the previous call (and other package) will be lost. Thus subsequent requests to view previously
 * displayed installation steps will result in new requests sent to the agent.
 *
 * There may be a need in the future to cache these somewhere. However, since we do not receive incremental step updates
 * during the deployment process, there is less of a need to cache or persist these. 
 *
 * @author Jason Dobies
 */
public class InstallationStepsUIBean {

    private int selectedPackageId;
    private PackageVersion packageVersion;

    private List<DeployPackageStep> deploySteps;
    private UIData stepsData;

    private final Log log = LogFactory.getLog(this.getClass());

    public String loadSteps() {
        ContentUIManagerLocal contentUIManager = LookupUtil.getContentUIManager();
        ContentManagerLocal contentManager = LookupUtil.getContentManager();

        HttpServletRequest request = FacesContextUtility.getRequest();
        selectedPackageId = Integer.parseInt(request.getParameter("selectedPackageId"));

        log.info("Loading package version for ID: " + selectedPackageId);

        packageVersion = contentUIManager.getPackageVersion(selectedPackageId);

        Resource resource = EnterpriseFacesContextUtility.getResource();

        // Going forward, we'll need to create this earlier and store the user entered configuration in these
        // objects.  jdobies, Mar 3, 2008
        ResourcePackageDetails details = ContentUtils.toResourcePackageDetails(packageVersion);

        try {
            deploySteps = contentManager.translateInstallationSteps(resource.getId(), details);
        } catch (Exception e) {
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR, "Could not retrieve installation steps", e);
        }

        log.info("Translated number of steps: " + (deploySteps != null ? deploySteps.size() : null));

/*
        // TEST DATA
        deploySteps = new ArrayList<DeployPackageStep>();

        deploySteps.add(new DeployPackageStep("0", "Backup some file somewhere"));
        deploySteps.add(new DeployPackageStep("1", "Do some more stuff"));
        deploySteps.add(new DeployPackageStep("2", "Restart something"));
*/

        return "loadedSteps";
    }

    public List<DeployPackageStep> getDeploySteps() {
        return deploySteps;
    }
    public UIData getStepsData() {
        return stepsData;
    }

    public void setStepsData(UIData stepsData) {
        this.stepsData = stepsData;
    }

    public int getSelectedPackageId() {
        return selectedPackageId;
    }

    public void setSelectedPackageId(int selectedPackageId) {
        this.selectedPackageId = selectedPackageId;
    }

    public PackageVersion getPackageVersion() {
        return packageVersion;
    }

    public void setPackageVersion(PackageVersion packageVersion) {
        this.packageVersion = packageVersion;
    }
}
