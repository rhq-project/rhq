package org.rhq.enterprise.gui.coregui.client.test.inventory;

import static org.rhq.enterprise.gui.coregui.client.inventory.resource.ResourceDataSourceField.CATEGORY;
import static org.rhq.enterprise.gui.coregui.client.inventory.resource.ResourceDataSourceField.NAME;

import com.smartgwt.client.widgets.form.fields.TextItem;

import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.search.SearchSubsystem;
import org.rhq.enterprise.gui.coregui.client.components.form.EnumSelectItem;
import org.rhq.enterprise.gui.coregui.client.components.form.SearchBarItem;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.ResourceSearchView;

public class TestSearchBarView extends ResourceSearchView {
    public TestSearchBarView(String locatorId) {
        super(locatorId);
    }

    @Override
    protected void configureTableFilters() {
        final TextItem nameFilter = new TextItem(NAME.propertyName(), NAME.title());
        final EnumSelectItem categoryFilter = new EnumSelectItem(CATEGORY.propertyName(), CATEGORY.title(),
            ResourceCategory.class);
        final SearchBarItem searchFilter = new SearchBarItem(SearchSubsystem.RESOURCE);

        setFilterFormItems(nameFilter, categoryFilter, searchFilter);
    }
}
