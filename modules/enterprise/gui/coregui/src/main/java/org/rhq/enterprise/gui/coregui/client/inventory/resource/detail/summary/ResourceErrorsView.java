package org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.summary;

import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.widgets.Window;
import com.smartgwt.client.widgets.events.CloseClickHandler;
import com.smartgwt.client.widgets.events.CloseClientEvent;
import com.smartgwt.client.widgets.grid.HoverCustomizer;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.grid.events.RecordClickEvent;
import com.smartgwt.client.widgets.grid.events.RecordClickHandler;

import org.rhq.enterprise.gui.coregui.client.components.table.Table;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableHTMLPane;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableWindow;

public class ResourceErrorsView extends Table {
    public ResourceErrorsView(String locatorId, String string, Criteria criteria, Object object, String[] strings) {
        super(locatorId, MSG.common_title_component_errors(), criteria);
    }

    /** Set order and width of table display
     */
    @Override
    protected void configureTable() {
        getListGrid().getField(ResourceErrorsDataSource.Field.ERROR_TYPE).setWidth("20%");
        getListGrid().getField(ResourceErrorsDataSource.Field.TIME_OCCURED).setWidth("15%");
        getListGrid().getField(ResourceErrorsDataSource.Field.SUMMARY).setWidth("30%");
        //modify icon display listgrid element
        ListGridField iconField = getListGrid().getField(ResourceErrorsDataSource.Field.ICON);
        iconField.setWidth("35%");
        //add action on click of cell
        iconField.addRecordClickHandler(new RecordClickHandler() {
            @Override
            public void onRecordClick(RecordClickEvent event) {
                final Window winModal = new LocatableWindow(extendLocatorId("errorDetailsWin"));
                winModal.setTitle(MSG.common_title_component_errors());
                winModal.setOverflow(Overflow.VISIBLE);
                winModal.setShowMinimizeButton(false);
                winModal.setShowMaximizeButton(true);
                winModal.setIsModal(true);
                winModal.setShowModalMask(true);
                winModal.setAutoSize(true);
                winModal.setAutoCenter(true);
                winModal.setShowResizer(true);
                winModal.setCanDragResize(true);
                winModal.centerInPage();
                winModal.addCloseClickHandler(new CloseClickHandler() {
                    @Override
                    public void onCloseClick(CloseClientEvent event) {
                        winModal.markForDestroy();
                    }
                });

                LocatableHTMLPane htmlPane = new LocatableHTMLPane(extendLocatorId("errorDetailsPane"));
                htmlPane.setMargin(10);
                htmlPane.setDefaultWidth(550);
                htmlPane.setDefaultHeight(550);
                htmlPane.setContents((event.getRecord().getAttribute(ResourceErrorsDataSource.Field.DETAIL)));
                winModal.addItem(htmlPane);
                winModal.show();
            }
        });
        iconField.setShowHover(true);
        //show some details in mouseOver
        iconField.setHoverCustomizer(new HoverCustomizer() {
            @Override
            public String hoverHTML(Object value, ListGridRecord record, int rowNum, int colNum) {
                String html = record.getAttribute(ResourceErrorsDataSource.Field.DETAIL);
                if (html.length() > 80) {
                    // this was probably an error stack trace, snip it so the tooltip isn't too big
                    html = "<pre>" + html.substring(0, 80) + "...</pre><p>"
                        + MSG.view_group_pluginConfig_table_clickStatusIcon() + "</p>";
                }
                return html;
            }
        });
    }
}
