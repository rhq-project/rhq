package org.rhq.enterprise.gui.inventory.browse;

import javax.faces.application.FacesMessage;
import javax.faces.model.DataModel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.criteria.ResourceGroupCriteria;
import org.rhq.core.domain.resource.group.GroupCategory;
import org.rhq.core.domain.resource.group.composite.ResourceGroupComposite;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.core.gui.util.StringUtility;
import org.rhq.enterprise.gui.common.framework.PagedDataTableUIBean;
import org.rhq.enterprise.gui.common.paging.PageControlView;
import org.rhq.enterprise.gui.common.paging.PagedListDataModel;
import org.rhq.enterprise.server.resource.group.ResourceGroupManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

public class BrowseGroupsUIBean extends PagedDataTableUIBean {
    protected final Log log = LogFactory.getLog(BrowseGroupsUIBean.class);

    public static final String MANAGED_BEAN_NAME = "BrowseGroupsUIBean";

    private String search;
    private GroupCategory category;

    private ResourceGroupManagerLocal groupManager = LookupUtil.getResourceGroupManager();

    public BrowseGroupsUIBean() {
        String subtab = FacesContextUtility.getOptionalRequestParameter("subtab", "").toLowerCase();
        if (subtab.equals("compatible")) {
            category = GroupCategory.COMPATIBLE;
        } else if (subtab.equals("mixed")) {
            category = GroupCategory.MIXED;
        }
    }

    public String getSearch() {
        return this.search;
    }

    public void setSearch(String search) {
        this.search = search;
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

    private class ResultsDataModel extends PagedListDataModel<ResourceGroupComposite> {

        public ResultsDataModel(PageControlView view, String beanName) {
            super(view, beanName);
        }

        public PageList<ResourceGroupComposite> fetchPage(PageControl pc) {
            String search = getSearch();
            GroupCategory category = getCategory();

            ResourceGroupCriteria criteria = new ResourceGroupCriteria();
            criteria.setPageControl(pc);
            if (search != null && !search.trim().equals("")) {
                criteria.setSearchExpression(search);
            }
            criteria.addFilterGroupCategory(category);

            PageList<ResourceGroupComposite> results;
            results = groupManager.findResourceGroupCompositesByCriteria(getSubject(), criteria);
            return results;
        }
    }

    public String deleteSelectedGroups() {
        try {
            Subject subject = getSubject();

            String[] selectedGroups = getSelectedItems();
            int[] groupIds = StringUtility.getIntArray(selectedGroups);

            for (int nextGroupId : groupIds) {
                groupManager.deleteResourceGroup(subject, nextGroupId);
            }

            FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO, "Deleted selected groups");
        } catch (Exception e) {
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR, "Failed to delete all selected groups", e);
        }

        return "success";
    }

    public String uninventoryMembers() {
        try {
            Subject subject = getSubject();

            String[] selectedGroups = getSelectedItems();
            int[] groupIds = StringUtility.getIntArray(selectedGroups);

            for (int nextGroupId : groupIds) {
                groupManager.uninventoryMembers(subject, nextGroupId);
            }

            FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO, "Uninventoried members of selected groups");
        } catch (Exception e) {
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR,
                "Failed to uninventory members of selected groups", e);
        }

        return "success";
    }

    private String[] getSelectedItems() {
        return FacesContextUtility.getRequest().getParameterValues("selectedItems");
    }

}
