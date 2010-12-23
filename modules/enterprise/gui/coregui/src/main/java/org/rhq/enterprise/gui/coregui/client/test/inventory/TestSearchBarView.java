package org.rhq.enterprise.gui.coregui.client.test.inventory;

import org.rhq.core.domain.search.SearchSubsystem;
import org.rhq.enterprise.gui.coregui.client.components.form.SearchBarItem;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.ResourceSearchView;

public class TestSearchBarView extends ResourceSearchView {
    public TestSearchBarView(String locatorId) {
        super(locatorId);
    }

    @Override
    protected void configureTableFilters() {
        final SearchBarItem searchFilter = new SearchBarItem("search", "Search", SearchSubsystem.RESOURCE);
        setFilterFormItems(searchFilter);
    }
}
