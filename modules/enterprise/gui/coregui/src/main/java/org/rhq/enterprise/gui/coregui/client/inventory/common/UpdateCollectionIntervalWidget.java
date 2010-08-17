package org.rhq.enterprise.gui.coregui.client.inventory.common;

import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.FormItem;
import com.smartgwt.client.widgets.form.fields.IntegerItem;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.form.fields.events.ChangedEvent;
import com.smartgwt.client.widgets.form.fields.events.ChangedHandler;
import com.smartgwt.client.widgets.form.validator.IntegerRangeValidator;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.VLayout;
import org.rhq.enterprise.gui.coregui.client.components.table.TableWidget;

/**
 * TODO
 */
public class UpdateCollectionIntervalWidget extends HLayout implements TableWidget {
    private AbstractMeasurementScheduleListView schedulesView;
    private DynamicForm form;
    private IButton setButton;

    public UpdateCollectionIntervalWidget(AbstractMeasurementScheduleListView schedulesView) {
        this.schedulesView = schedulesView;
    }

    @Override
    protected void onDraw() {
        super.onDraw();

        VLayout spacer = new VLayout();
        spacer.setWidth(20);
        addMember(spacer);

        this.form = new DynamicForm();
        this.form.setNumCols(3);
        IntegerItem intervalItem = new IntegerItem("interval", "Collection Interval");
        IntegerRangeValidator integerRangeValidator = new IntegerRangeValidator();
        integerRangeValidator.setMin(1);
        intervalItem.setValidators(integerRangeValidator);
        intervalItem.setValidateOnChange(true);
        intervalItem.addChangedHandler(new ChangedHandler() {
            public void onChanged(ChangedEvent changedEvent) {
                refresh(UpdateCollectionIntervalWidget.this.schedulesView.getListGrid());
            }
        });
        SelectItem unitsItem = new SelectItem("units", null);
        unitsItem.setValueMap("seconds", "minutes", "hours");
        unitsItem.setDefaultValue("seconds");
        unitsItem.setShowTitle(false);
        this.form.setFields(intervalItem, unitsItem);
        addMember(this.form);

        this.setButton = new IButton("Set");
        this.setButton.setDisabled(true);
        this.setButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                form.validate();
                UpdateCollectionIntervalWidget.this.schedulesView.getDataSource().updateSchedules(
                        UpdateCollectionIntervalWidget.this.schedulesView, getInterval());
            }
        });
        addMember(this.setButton);
    }

    @Override
    public void refresh(ListGrid listGrid) {
        int count = listGrid.getSelection().length;
        Long interval = getInterval();
        this.setButton.setDisabled(count == 0 || interval == null);
    }

    private Long getInterval() {
        FormItem item = this.form.getItem("interval");
        if (item.getValue() == null || !item.validate()) {
            return null;
        }
        String stringValue = this.form.getValueAsString("interval");
        long value = Long.valueOf(stringValue.trim());
        String units = this.form.getValueAsString("units");
        value *= 1000;
        if (units.equals("minutes")) {
            value *= 60;
        } else if (units.equals("hours")) {
            value *= 60 * 60;
        }
        return value;
    }
}
