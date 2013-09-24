package org.rhq.enterprise.gui.coregui.client.bundle.deploy.selection;

import com.smartgwt.client.data.DSRequest;

import org.rhq.core.domain.criteria.ResourceGroupCriteria;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.selection.ResourceGroupSelector;

public class PlatformResGroupSelector extends ResourceGroupSelector {

    public PlatformResGroupSelector() {
        super();
    }

    @Override
    protected SelectedPlatformResGroupsDataSource getDataSource() {
        return new SelectedPlatformResGroupsDataSource();
    }

    protected class SelectedPlatformResGroupsDataSource extends SelectedResourceGroupsDataSource {

        @Override
        protected ResourceGroupCriteria getFetchCriteria(final DSRequest request) {
            ResourceGroupCriteria result = super.getFetchCriteria(request);
            result.addFilterExplicitResourceCategory(ResourceCategory.PLATFORM);
            return result;
        }
    }

}
