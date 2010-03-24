/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.rhq.enterprise.gui.coregui.client.inventory.resource.selection;

import java.util.HashSet;

import com.google.gwt.user.client.Timer;
import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.types.DragDataAction;
import com.smartgwt.client.types.DragTrackerMode;
import com.smartgwt.client.types.ListGridFieldType;
import com.smartgwt.client.types.VerticalAlignment;
import com.smartgwt.client.widgets.ImgProperties;
import com.smartgwt.client.widgets.TransferImgButton;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.events.KeyPressEvent;
import com.smartgwt.client.widgets.events.KeyPressHandler;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.events.ItemChangedEvent;
import com.smartgwt.client.widgets.form.events.ItemChangedHandler;
import com.smartgwt.client.widgets.form.fields.IPickTreeItem;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.grid.events.RecordDropEvent;
import com.smartgwt.client.widgets.grid.events.RecordDropHandler;
import com.smartgwt.client.widgets.grid.events.SelectionChangedHandler;
import com.smartgwt.client.widgets.grid.events.SelectionEvent;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.LayoutSpacer;
import com.smartgwt.client.widgets.layout.VLayout;
import com.smartgwt.client.widgets.layout.VStack;

import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.ResourceDatasource;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.type.ResourceTypeTreeDataSource;

/**
 * @author Greg Hinkle
 */
public class ResourceSelector extends HLayout {

    private HashSet<Integer> selection = new HashSet<Integer>();

    private ListGrid availableGrid;
    private ListGrid assignedGrid;

    private Criteria latestCriteria;

    public ResourceSelector() {
        setAlign(VerticalAlignment.BOTTOM);
        setHeight(380);
    }

    @Override
    protected void onDraw() {
        super.onDraw();


        final DynamicForm availableFilterForm = new DynamicForm();
        availableFilterForm.setNumCols(6);
        final TextItem search = new TextItem("search", "Search");

        IPickTreeItem typeSelectItem = new IPickTreeItem("type","Type");
        typeSelectItem.setDataSource(new ResourceTypeTreeDataSource());
        typeSelectItem.setValueField("id");
        typeSelectItem.setCanSelectParentItems(true);
        typeSelectItem.setLoadDataOnDemand(false);



        SelectItem categorySelect = new SelectItem("category","Category");
        categorySelect.setValueMap("Platform","Server","Service");
        categorySelect.setAllowEmptyValue(true);
        availableFilterForm.setItems(search, typeSelectItem, categorySelect);

        VLayout availableLayout = new VLayout();
        availableLayout.addMember(new LayoutSpacer());

        availableLayout.addMember(availableFilterForm);

        availableGrid = new ListGrid();
        availableGrid.setHeight(350);
        availableGrid.setCanDragRecordsOut(true);
        availableGrid.setDragTrackerMode(DragTrackerMode.ICON);
        availableGrid.setTrackerImage(new ImgProperties("types/Service_up_16.png", 16, 16));
        availableGrid.setDragDataAction(DragDataAction.COPY);
        availableGrid.setDataSource(new SelectedResourceDataSource());
        availableGrid.setFetchDelay(700);

        availableGrid.setAutoFetchData(true);
        availableGrid.setFields(new ListGridField("icon", 50), new ListGridField("name"));

        availableLayout.addMember(availableGrid);

        addMember(availableLayout);



        availableFilterForm.addItemChangedHandler(new ItemChangedHandler() {
            public void onItemChanged(ItemChangedEvent itemChangedEvent) {
                latestCriteria = new Criteria();
                latestCriteria.setAttribute("name",availableFilterForm.getValue("search"));
                latestCriteria.setAttribute("type",availableFilterForm.getValue("type"));
                latestCriteria.setAttribute("category",availableFilterForm.getValue("category"));

                Timer t = new Timer() {
                    @Override
                    public void run() {
                        if (latestCriteria != null) {
                            Criteria c = latestCriteria;
                            latestCriteria = null;
                            availableGrid.fetchData(c);
                        }
                    }
                };
                t.schedule(500);
            }
        });






        VStack moveButtonStack = new VStack(6);
        moveButtonStack.setAlign(VerticalAlignment.CENTER);
        moveButtonStack.setWidth(40);

        final TransferImgButton addButton = new TransferImgButton(TransferImgButton.RIGHT);
        addButton.setDisabled(true);
        final TransferImgButton removeButton = new TransferImgButton(TransferImgButton.LEFT);
        removeButton.setDisabled(true);
        TransferImgButton addAllButton = new TransferImgButton(TransferImgButton.RIGHT_ALL);
        TransferImgButton removeAllButton = new TransferImgButton(TransferImgButton.LEFT_ALL);

        moveButtonStack.addMember(addButton);
        moveButtonStack.addMember(removeButton);
        moveButtonStack.addMember(addAllButton);
        moveButtonStack.addMember(removeAllButton);

        addMember(moveButtonStack);



        VLayout assignedLayout = new VLayout();
        assignedLayout.addMember(new LayoutSpacer());

        assignedGrid = new ListGrid();
        assignedGrid.setHeight(350);
        assignedGrid.setCanReorderRecords(true);
        assignedGrid.setCanDragRecordsOut(true);

        assignedGrid.setCanAcceptDroppedRecords(true);
        ListGridField iconField = new ListGridField("icon", 50);
        iconField.setType(ListGridFieldType.ICON);
        assignedGrid.setFields(iconField, new ListGridField("name"));

        assignedLayout.addMember(assignedGrid);
        addMember(assignedLayout);


        addButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                assignedGrid.transferSelectedData(availableGrid);
                select(assignedGrid.getSelection());
            }
        });
        removeButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                deselect(assignedGrid.getSelection());
                assignedGrid.removeSelectedData();
            }
        });
        addAllButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                availableGrid.selectAllRecords();
                assignedGrid.transferSelectedData(availableGrid);
                select(availableGrid.getSelection());
            }
        });
        removeAllButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                assignedGrid.selectAllRecords();
                deselect(assignedGrid.getSelection());
                assignedGrid.removeSelectedData();
            }
        });

        availableGrid.addSelectionChangedHandler(new SelectionChangedHandler() {
            public void onSelectionChanged(SelectionEvent selectionEvent) {
                addButton.setDisabled(!availableGrid.anySelected());
            }
        });

        assignedGrid.addSelectionChangedHandler(new SelectionChangedHandler() {
            public void onSelectionChanged(SelectionEvent selectionEvent) {
                removeButton.setDisabled(!assignedGrid.anySelected());
            }
        });

        assignedGrid.addKeyPressHandler(new KeyPressHandler() {
            public void onKeyPress(KeyPressEvent event) {
                if ("Delete".equals(event.getKeyName())) {
                    deselect(assignedGrid.getSelection());
                    assignedGrid.removeSelectedData();
                }
            }
        });


        assignedGrid.addRecordDropHandler(new RecordDropHandler() {
            public void onRecordDrop(RecordDropEvent recordDropEvent) {
                select(recordDropEvent.getDropRecords());
            }
        });
    }

    private void select(ListGridRecord[] records) {
        availableGrid.deselectAllRecords();
        for (ListGridRecord record : records) {
            record.setEnabled(false);
            selection.add(record.getAttributeAsInt("id"));
        }
    }

    private void deselect(ListGridRecord[] records) {
        HashSet<Integer> toRemove = new HashSet<Integer>();
        for (ListGridRecord record : records) {
            toRemove.add(record.getAttributeAsInt("id"));
        }
        selection.removeAll(toRemove);


        for (Integer id : toRemove) {
            Record r = availableGrid.getDataAsRecordList().find("id", id);
            if (r != null) {
                ((ListGridRecord) r).setEnabled(true);
            }
        }
        availableGrid.markForRedraw();
    }


    private class SelectedResourceDataSource extends ResourceDatasource {


        @Override
        public ListGridRecord[] buildRecords(PageList<Resource> resources) {
            ListGridRecord[] records = super.buildRecords(resources);
            for (ListGridRecord record : records) {
                if (selection.contains(record.getAttributeAsInt("id"))) {
                    record.setEnabled(false);
                }
            }
            return records;
        }
    }
}
