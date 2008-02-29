package org.rhq.enterprise.gui.content;

import java.util.List;
import javax.faces.component.UIData;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.domain.content.composite.PackageVersionComposite;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.enterprise.server.util.LookupUtil;
import org.rhq.enterprise.server.content.ContentUIManagerLocal;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;

/**
 * @author Jason Dobies
 */
public class DeployPackagesUIBean {

    private int[] selectedPackageIds;
    private List<PackageVersionComposite> packagesToDeploy;
    private UIData packagesToDeployData;

    private final Log log = LogFactory.getLog(this.getClass());

    public String deployPackages() {
        HttpServletRequest request = FacesContextUtility.getRequest();
        HttpSession session = request.getSession();
        String[] packageIds = (String[])session.getAttribute("selectedPackages");

        log.info("Deploying packages");
        for (String pkgId : packageIds) {
            log.info("Package: " + pkgId);
        }

        return "successOrFailure";
    }

    public List<PackageVersionComposite> getPackagesToDeploy() {
        if (packagesToDeploy == null) {
            Subject subject = EnterpriseFacesContextUtility.getSubject();

            ContentUIManagerLocal contentUIManager = LookupUtil.getContentUIManager();
            packagesToDeploy = contentUIManager.getPackageVersionComposites(subject, getSelectedPackageIds());
        }

        return packagesToDeploy;
    }

    public UIData getPackagesToDeployData() {
        return packagesToDeployData;
    }

    public void setPackagesToDeployData(UIData packagesToDeployData) {
        this.packagesToDeployData = packagesToDeployData;
    }

    public int[] getSelectedPackageIds() {
        if (selectedPackageIds == null) {
            selectedPackageIds = (int[]) FacesContextUtility.getRequest().getSession().getAttribute("selectedPackages");
        }
        
        return selectedPackageIds;
    }
}
