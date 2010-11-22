package org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.summary;

import com.smartgwt.client.data.Criteria;

import org.rhq.enterprise.gui.coregui.client.components.table.Table;

public class ResourceErrorsView extends Table {
    public ResourceErrorsView(String locatorId, String string, Criteria criteria, Object object, String[] strings) {
        super(locatorId, MSG.common_title_component_errors(), criteria);
    }

    /** Set order and width of table display
     */
    @Override
    protected void configureTable() {
        getListGrid().getField(ResourceErrorsDataSource.Field.ERROR_TYPE).setWidth("10%");
        getListGrid().getField(ResourceErrorsDataSource.Field.TIME_OCCURED).setWidth("10%");
        getListGrid().getField(ResourceErrorsDataSource.Field.SUMMARY).setWidth("20%");
        getListGrid().getField(ResourceErrorsDataSource.Field.DETAIL).setWidth("60%");
        getListGrid().setWrapCells(true);
        getListGrid().setCellHeight(100);
    }
}
