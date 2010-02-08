package org.rhq.enterprise.gui.inventory.browse;

import java.util.ArrayList;
import java.util.List;

import javax.faces.model.DataModel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.criteria.ResourceGroupCriteria;
import org.rhq.core.domain.resource.group.GroupCategory;
import org.rhq.core.domain.resource.group.composite.ResourceGroupComposite;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.enterprise.gui.common.framework.PagedDataTableUIBean;
import org.rhq.enterprise.gui.common.paging.PageControlView;
import org.rhq.enterprise.gui.common.paging.PagedListDataModel;
import org.rhq.enterprise.gui.inventory.browse.BrowseResourcesUIBean.Suggestion;
import org.rhq.enterprise.server.resource.group.ResourceGroupManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

public class BrowseGroupsUIBean extends PagedDataTableUIBean {
    protected final Log log = LogFactory.getLog(BrowseGroupsUIBean.class);

    public static final String MANAGED_BEAN_NAME = "BrowseGroupsUIBean";

    private String filter;
    private GroupCategory category;

    public BrowseGroupsUIBean() {
        String subtab = FacesContextUtility.getOptionalRequestParameter("subtab", "").toLowerCase();
        if (subtab.equals("compatible")) {
            category = GroupCategory.COMPATIBLE;
        } else if (subtab.equals("mixed")) {
            category = GroupCategory.MIXED;
        }

        filter = FacesContextUtility.getOptionalRequestParameter("filter");
    }

    public String getFilter() {
        return filter;
    }

    public void setFilter(String filter) {
        this.filter = filter;
    }

    public GroupCategory getCategory() {
        return category;
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
            String filter = getFilter();
            GroupCategory category = getCategory();

            ResourceGroupCriteria criteria = new ResourceGroupCriteria();
            criteria.setPageControl(pc);
            if (filter != null && !filter.equals("")) {
                criteria.addFilterName(filter);
            }
            criteria.addFilterGroupCategory(category);

            PageList<ResourceGroupComposite> results;
            results = groupManager.findResourceGroupCompositesByCriteria(getSubject(), criteria);
            return results;
        }
    }

    public List<Suggestion> autocomplete(Object suggest) {
        String currentInputText = (String) suggest;
        List<Suggestion> results = new ArrayList<Suggestion>();
        // offer suggestions based on currentInputText
        return results;
    }
}
