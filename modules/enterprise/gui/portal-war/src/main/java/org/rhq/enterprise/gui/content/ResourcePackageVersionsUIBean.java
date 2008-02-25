package org.rhq.enterprise.gui.content;

import javax.faces.model.DataModel;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.content.composite.PackageVersionComposite;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.enterprise.gui.common.framework.PagedDataTableUIBean;
import org.rhq.enterprise.gui.common.paging.PageControlView;
import org.rhq.enterprise.gui.common.paging.PagedListDataModel;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.content.ContentUIManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

public class ResourcePackageVersionsUIBean extends PagedDataTableUIBean {

    public static final String MANAGED_BEAN_NAME = "ResourcePackageVersionsUIBean";

    public ResourcePackageVersionsUIBean() {
    }

    @Override
    public DataModel getDataModel() {
        if (dataModel == null) {
            dataModel = new ResourcePackageVersionsDataModel(PageControlView.ResourcePackageVersionsList,
                MANAGED_BEAN_NAME);
        }

        return dataModel;
    }

    private class ResourcePackageVersionsDataModel extends PagedListDataModel<PackageVersionComposite> {
        public ResourcePackageVersionsDataModel(PageControlView view, String beanName) {
            super(view, beanName);
        }

        @Override
        @SuppressWarnings("unchecked")
        public PageList<PackageVersionComposite> fetchPage(PageControl pc) {
            Subject subject = EnterpriseFacesContextUtility.getSubject();
            Integer resourceId = Integer.parseInt(FacesContextUtility.getRequiredRequestParameter("id"));
            //String filter = FacesContextUtility.getRequiredRequestParameter("contentForm:filter");
            ContentUIManagerLocal manager = LookupUtil.getContentUIManager();

            PageList<PackageVersionComposite> results = manager.getPackageVersionCompositesByFilter(subject,
                resourceId, null, pc);
            return results;
        }
    }

}
