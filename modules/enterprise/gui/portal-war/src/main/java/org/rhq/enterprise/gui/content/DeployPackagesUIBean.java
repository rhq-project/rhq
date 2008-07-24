package org.rhq.enterprise.gui.content;

import java.util.List;
import java.util.Set;
import java.util.HashSet;
import javax.faces.component.UIData;
import javax.faces.model.DataModel;
import javax.faces.application.FacesMessage;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.domain.content.composite.PackageVersionComposite;
import org.rhq.core.domain.content.transfer.ResourcePackageDetails;
import org.rhq.core.domain.content.*;
import org.rhq.core.domain.content.Package;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.enterprise.server.util.LookupUtil;
import org.rhq.enterprise.server.content.ContentUIManagerLocal;
import org.rhq.enterprise.server.content.ContentManagerLocal;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.gui.common.framework.PagedDataTableUIBean;
import org.rhq.enterprise.gui.common.paging.PagedListDataModel;
import org.rhq.enterprise.gui.common.paging.PageControlView;

/**
 * Bean responsible for the end of the deploy package workflow. This bean will provide the list of packages that
 * have been selected (and ultimately configured by the user) to be deployed to the agent. This bean also provides
 * the action to perform the actual deployment.
 *
 * @author Jason Dobies
 */
public class DeployPackagesUIBean extends PagedDataTableUIBean {

    public static final String MANAGED_BEAN_NAME = "DeployPackagesUIBean";

    private int[] selectedPackageIds;
    private UIData packagesToDeployData;

    private String notes;

    private final Log log = LogFactory.getLog(this.getClass());

    /**
     * JSF action that will actually make the call to deploy the packages in the workflow.
     *
     * @return navigation outcome
     */
    public String deployPackages() {

        if (notes != null && notes.length() > 512) {
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR, "Package notes must be 512 characters or less.");
            return null;
        }

        HttpServletRequest request = FacesContextUtility.getRequest();
        HttpSession session = request.getSession();
        int[] packageIds = (int[])session.getAttribute("selectedPackages");

        // Going forward, we'll need to create this earlier and store the user entered configuration in these
        // objects.  jdobies, Mar 3, 2008
        ContentUIManagerLocal contentUIManager = LookupUtil.getContentUIManager();
        Set<ResourcePackageDetails> packagesToDeploy = new HashSet<ResourcePackageDetails>(packageIds.length);
        for (int pkgId : packageIds) {
            PackageVersion packageVersion = contentUIManager.getPackageVersion(pkgId);
            ResourcePackageDetails pkgDetails = ContentUtils.toResourcePackageDetails(packageVersion);
            packagesToDeploy.add(pkgDetails);
        }

        Subject subject = EnterpriseFacesContextUtility.getSubject();
        Resource resource = EnterpriseFacesContextUtility.getResource();

        try {
            ContentManagerLocal contentManager = LookupUtil.getContentManager();
            contentManager.deployPackages(subject, resource.getId(), packagesToDeploy, notes);
        } catch (Exception e) {
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR, "Could not send deploy request to agent", e);
        }

        return "successOrFailure";
    }

    public DataModel getDataModel() {
        if (dataModel == null) {
            dataModel = new DeployPackagesDataModel(PageControlView.PackagesToDeployList, MANAGED_BEAN_NAME);
        }

        return dataModel;
    }

    public UIData getPackagesToDeployData() {
        return packagesToDeployData;
    }

    public void setPackagesToDeployData(UIData packagesToDeployData) {
        this.packagesToDeployData = packagesToDeployData;
    }

    public String getNotes() {
        if (notes == null) {
            HttpServletRequest request = FacesContextUtility.getRequest();
            HttpSession session = request.getSession();
            int[] packageIds = (int[])session.getAttribute("selectedPackages");

            ContentUIManagerLocal contentUIManager = LookupUtil.getContentUIManager();

            StringBuffer sb = new StringBuffer("Packages: ");
            int counter = 0;
            for (int pkgId : packageIds) {
                PackageVersion packageVersion = contentUIManager.getPackageVersion(pkgId);
                Package generalPackage = packageVersion.getGeneralPackage();

                String version = packageVersion.getDisplayVersion() != null ?
                    packageVersion.getDisplayVersion() : packageVersion.getVersion();

                String packageToAppend = generalPackage.getName() + " " + version;

                // Don't generate notes that would fail our own validation
                if (sb.toString().length() + packageToAppend.length() > 508) {

                    // If we're not at the last package yet, add ... to show there were more
                    if (counter != (packageIds.length - 1)) {
                        sb.append("...");
                        break;
                    }

                    // If we are at the last package, see if this one will fit, otherwise add ...
                    if (sb.toString().length() + packageToAppend.length() <= 511) {
                        sb.append(packageToAppend);
                    }
                    else {
                        sb.append("...");
                    }

                    break;
                }

                sb.append(packageToAppend);

                if (counter++ < (packageIds.length - 1))
                    sb.append(", ");
            }

            notes = sb.toString();
        }

        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public int[] getSelectedPackageIds() {
        if (selectedPackageIds == null) {
            selectedPackageIds = (int[])FacesContextUtility.getRequest().getSession().getAttribute("selectedPackages");
        }
        
        return selectedPackageIds;
    }

    private class DeployPackagesDataModel extends PagedListDataModel<PackageVersionComposite> {

        private DeployPackagesDataModel(PageControlView view, String beanName) {
            super(view, beanName);
        }

        public PageList<PackageVersionComposite> fetchPage(PageControl pc) {
            Subject subject = EnterpriseFacesContextUtility.getSubject();

            ContentUIManagerLocal contentUIManager = LookupUtil.getContentUIManager();
            PageList<PackageVersionComposite> results =
                contentUIManager.getPackageVersionComposites(subject, getSelectedPackageIds(), pc);

            return results;
        }
    }
}
