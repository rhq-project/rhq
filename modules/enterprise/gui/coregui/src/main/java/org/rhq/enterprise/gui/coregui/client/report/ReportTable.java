package org.rhq.enterprise.gui.coregui.client.report;

import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.VerticalAlignment;
import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.VLayout;
import com.smartgwt.client.widgets.toolbar.ToolStrip;

import org.rhq.enterprise.gui.coregui.client.PopupWindow;
import org.rhq.enterprise.gui.coregui.client.components.table.Table;
import org.rhq.enterprise.gui.coregui.client.components.table.TableAction;
import org.rhq.enterprise.gui.coregui.client.util.RPCDataSource;

public class ReportTable<DS extends RPCDataSource> extends Table<DS> {

    public ReportTable(String locatorId) {
        super(locatorId);
    }

    @Override
    protected void configureTable() {
        addTableActions();
    }

    private void addTableActions() {
        addTableAction("Export", "Export", new TableAction() {
            @Override
            public boolean isEnabled(ListGridRecord[] selection) {
                return true;
            }

            @Override
            public void executeAction(ListGridRecord[] selection, Object actionValue) {
                final PopupWindow exportWindow = new PopupWindow("exportSettings", null);

                VLayout layout = new VLayout();
                layout.setTitle("Export Settings");

                HLayout headerLayout = new HLayout();
                headerLayout.setAlign(Alignment.CENTER);
                Label header = new Label();
                header.setContents("Export Settings");
                header.setWidth100();
                header.setHeight(40);
                header.setPadding(20);
                //header.setStyleName("HeaderLabel");
                headerLayout.addMember(header);
                layout.addMember(headerLayout);

                HLayout formLayout = new HLayout();
                formLayout.setAlign(VerticalAlignment.TOP);

                DynamicForm form = new DynamicForm();

                SelectItem formatsList = new SelectItem("Format", "Format");
                formatsList.setValueMap("CSV", "XML");

                form.setItems(formatsList);
                formLayout.addMember(form);
                layout.addMember(formLayout);

                ToolStrip buttonBar = new ToolStrip();
                buttonBar.setAlign(Alignment.RIGHT);

                IButton finishButton = new IButton("Finish", new ClickHandler() {
                    @Override
                    public void onClick(ClickEvent clickEvent) {
                        exportWindow.setVisible(false);
                        exportWindow.destroy();
                    }
                });
                buttonBar.addMember(finishButton);
                layout.addMember(buttonBar);

                exportWindow.addItem(layout);
                exportWindow.show();
                refreshTableInfo();
            }
        });
    }

}
