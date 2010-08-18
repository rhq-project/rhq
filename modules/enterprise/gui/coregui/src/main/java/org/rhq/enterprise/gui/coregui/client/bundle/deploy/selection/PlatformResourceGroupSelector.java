package org.rhq.enterprise.gui.coregui.client.bundle.deploy.selection;

import com.smartgwt.client.data.DSRequest;

import org.rhq.core.domain.criteria.ResourceGroupCriteria;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.selection.ResourceGroupSelector;
import org.rhq.enterprise.gui.coregui.client.util.RPCDataSource;

public class PlatformResourceGroupSelector extends ResourceGroupSelector {

    public PlatformResourceGroupSelector(String id) {
        super(id);
    }

    @Override
    protected RPCDataSource<ResourceGroup> getDataSource() {
        return new SelectedPlatformResourceGroupsDataSource();
    }

    protected class SelectedPlatformResourceGroupsDataSource extends SelectedResourceGroupsDataSource {

        @Override
        protected ResourceGroupCriteria getFetchCriteria(final DSRequest request) {
            ResourceGroupCriteria result = super.getFetchCriteria(request);
            result.addFilterExplicitResourceCategory(ResourceCategory.PLATFORM);
            return result;
        }
    }

}
