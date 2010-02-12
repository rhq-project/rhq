package org.rhq.enterprise.server.perspective.policy;

import org.ajax4jsf.model.KeepAlive;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.bundle.Bundle;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.gui.table.bean.AbstractPagedDataUIBean;
import org.rhq.core.gui.table.model.PagedListDataModel;
import org.rhq.enterprise.server.bundle.BundleManagerRemote;
import org.rhq.enterprise.server.perspective.AbstractPerspectivePagedDataUIBean;

import java.util.List;

/**
 * Provides CRUD operations for provisioning {@link Bundle}s. The backing bean for bundles.xhtml.
 */
@Name("BundlesUIBean")
@Scope(ScopeType.EVENT)
@KeepAlive
public class BundlesUIBean extends AbstractPerspectivePagedDataUIBean {
    private BundleManagerRemote bundleManager;
    private List<Bundle> selectedBundles;

    @Create
    public void init() {
        try {
            this.bundleManager = this.perspectiveClient.getRemoteClient().getBundleManagerRemote();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public PagedListDataModel createDataModel() {
        return new DataModel(this);
    }

    public List<Bundle> getSelectedBundles() {
        return this.selectedBundles;
    }

    public void setSelectedBundles(List<Bundle> selectedResources) {
        this.selectedBundles = selectedResources;
    }

    private class DataModel extends PagedListDataModel<Bundle> {
        private DataModel(AbstractPagedDataUIBean pagedDataBean) {
            super(pagedDataBean);
        }

        @Override
        public PageList<Bundle> fetchPage(PageControl pageControl) {
            Subject subject;
            try {
                subject = BundlesUIBean.this.perspectiveClient.getSubject();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            // TODO: Implement filtering.
            PageList<Bundle> bundles = BundlesUIBean.this.bundleManager.findBundlesByCriteria(subject, null, null, pageControl);
            return bundles;
        }
    }
}
