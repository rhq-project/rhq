package org.rhq.enterprise.gui.inventory.browse;

import javax.faces.model.DataModel;

import org.rhq.core.domain.criteria.ResourceGroupCriteria;
import org.rhq.core.domain.resource.group.composite.ResourceGroupComposite;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.enterprise.gui.common.framework.PagedDataTableUIBean;
import org.rhq.enterprise.gui.common.paging.PageControlView;
import org.rhq.enterprise.gui.common.paging.PagedListDataModel;
import org.rhq.enterprise.server.resource.group.ResourceGroupManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

public class BrowseGroupsUIBean extends PagedDataTableUIBean {
    public static final String MANAGED_BEAN_NAME = "BrowseGroupsUIBean";
    private static final String FORM_PREFIX = "browseGroupsForm:";

    private String filter;

    public String getFilter() {
        return filter;
    }

    public void setFilter(String filter) {
        this.filter = filter;
    }

    @Override
    public DataModel getDataModel() {
        if (dataModel == null) {
            dataModel = new ResultsDataModel(PageControlView.BrowseGroups, MANAGED_BEAN_NAME);
        }
        return dataModel;
    }

    private ResourceGroupManagerLocal groupManager = LookupUtil.getResourceGroupManager();

    private class ResultsDataModel extends PagedListDataModel<ResourceGroupComposite> {

        public ResultsDataModel(PageControlView view, String beanName) {
            super(view, beanName);
        }

        public PageList<ResourceGroupComposite> fetchPage(PageControl pc) {
            getDataFromRequest();
            String filter = getFilter();

            ResourceGroupCriteria criteria = new ResourceGroupCriteria();
            criteria.setPageControl(pc);
            if (filter != null && !filter.equals("")) {
                criteria.addFilterName(filter);
            }

            PageList<ResourceGroupComposite> results = groupManager.findResourceGroupCompositesByCriteria(getSubject(),
                criteria);
            return results;
        }

        private void getDataFromRequest() {
            BrowseGroupsUIBean.this.filter = FacesContextUtility.getOptionalRequestParameter(FORM_PREFIX + "filter");
        }
    }
}
