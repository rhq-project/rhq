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
package org.rhq.enterprise.gui.coregui.client.components.selector;

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
import com.smartgwt.client.widgets.events.DoubleClickEvent;
import com.smartgwt.client.widgets.events.DoubleClickHandler;
import com.smartgwt.client.widgets.events.KeyPressEvent;
import com.smartgwt.client.widgets.events.KeyPressHandler;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.events.ItemChangedEvent;
import com.smartgwt.client.widgets.form.events.ItemChangedHandler;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.grid.events.RecordDropEvent;
import com.smartgwt.client.widgets.grid.events.RecordDropHandler;
import com.smartgwt.client.widgets.grid.events.SelectionChangedHandler;
import com.smartgwt.client.widgets.grid.events.SelectionEvent;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.VLayout;
import com.smartgwt.client.widgets.layout.VStack;

import org.rhq.enterprise.gui.coregui.client.util.RPCDataSource;

/**
 * @author Greg Hinkle
 */
public abstract class AbstractSelector<T> extends VLayout {

    protected HashSet<Integer> selection = new HashSet<Integer>();

    protected ListGridRecord[] initialSelection;

    protected ListGrid availableGrid;
    protected ListGrid assignedGrid;

    protected TransferImgButton addButton;
    protected TransferImgButton removeButton;
    protected TransferImgButton addAllButton;
    protected TransferImgButton removeAllButton;

    protected Criteria latestCriteria;

    public AbstractSelector() {
    }

    public void setAssigned(ListGridRecord[] assignedRecords) {
        initialSelection = assignedRecords;
    }

    public HashSet<Integer> getSelection() {
        return selection;
    }

    protected abstract DynamicForm getAvailableFilterForm();

    protected abstract RPCDataSource<T> getDataSource();

    protected abstract Criteria getLatestCriteria(DynamicForm availableFilterForm);

    @Override
    protected void onInit() {
        super.onInit();

        final DynamicForm availableFilterForm = getAvailableFilterForm();

        if (availableFilterForm != null) {
            addMember(availableFilterForm);
        }

        HLayout hlayout = new HLayout();
        hlayout.setAlign(VerticalAlignment.BOTTOM);

        // LEFT SIDE
        availableGrid = new ListGrid();
        availableGrid.setHeight(350);
        availableGrid.setCanDragRecordsOut(true);
        availableGrid.setDragTrackerMode(DragTrackerMode.ICON);
        availableGrid.setTrackerImage(new ImgProperties("types/Service_up_16.png", 16, 16));
        availableGrid.setDragDataAction(DragDataAction.COPY);
        availableGrid.setDataSource(getDataSource());
        availableGrid.setFetchDelay(700);

        availableGrid.setAutoFetchData(true);
        availableGrid.setFields(new ListGridField("icon", 50), new ListGridField("name"));

        hlayout.addMember(availableGrid);

        if (availableFilterForm != null) {
            availableFilterForm.addItemChangedHandler(new ItemChangedHandler() {
                public void onItemChanged(ItemChangedEvent itemChangedEvent) {
                    latestCriteria = getLatestCriteria(availableFilterForm);

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
        }
        // CENTER BUTTONS
        VStack moveButtonStack = new VStack(6);
        moveButtonStack.setAlign(VerticalAlignment.CENTER);
        moveButtonStack.setWidth(40);

        addButton = new TransferImgButton(TransferImgButton.RIGHT);
        addButton.setDisabled(true);
        removeButton = new TransferImgButton(TransferImgButton.LEFT);
        removeButton.setDisabled(true);
        addAllButton = new TransferImgButton(TransferImgButton.RIGHT_ALL);
        removeAllButton = new TransferImgButton(TransferImgButton.LEFT_ALL);
        removeAllButton.setDisabled(true);

        moveButtonStack.addMember(addButton);
        moveButtonStack.addMember(removeButton);
        moveButtonStack.addMember(addAllButton);
        moveButtonStack.addMember(removeAllButton);

        hlayout.addMember(moveButtonStack);

        // RIGHT SIDE

        assignedGrid = new ListGrid();
        assignedGrid.setHeight(350);
        assignedGrid.setCanReorderRecords(true);
        assignedGrid.setCanDragRecordsOut(true);

        assignedGrid.setCanAcceptDroppedRecords(true);
        ListGridField iconField = new ListGridField("icon", 50);
        iconField.setType(ListGridFieldType.ICON);
        assignedGrid.setFields(iconField, new ListGridField("name"));

        hlayout.addMember(assignedGrid);

        addButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                assignedGrid.transferSelectedData(availableGrid);
                select(assignedGrid.getSelection());
                updateButtons();
            }
        });
        removeButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                deselect(assignedGrid.getSelection());
                assignedGrid.removeSelectedData();
                updateButtons();
            }
        });
        addAllButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                availableGrid.selectAllRecords();
                assignedGrid.transferSelectedData(availableGrid);
                select(availableGrid.getSelection());
                updateButtons();
            }
        });
        removeAllButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                assignedGrid.selectAllRecords();
                deselect(assignedGrid.getSelection());
                assignedGrid.removeSelectedData();
                updateButtons();
            }
        });

        availableGrid.addDoubleClickHandler(new DoubleClickHandler() {
            public void onDoubleClick(DoubleClickEvent event) {
                assignedGrid.transferSelectedData(availableGrid);
                select(assignedGrid.getSelection());
                updateButtons();
            }
        });

        assignedGrid.addDoubleClickHandler(new DoubleClickHandler() {
            public void onDoubleClick(DoubleClickEvent event) {
                deselect(assignedGrid.getSelection());
                assignedGrid.removeSelectedData();
                updateButtons();
            }
        });

        availableGrid.addSelectionChangedHandler(new SelectionChangedHandler() {
            public void onSelectionChanged(SelectionEvent selectionEvent) {
                updateButtons();
            }
        });

        assignedGrid.addSelectionChangedHandler(new SelectionChangedHandler() {
            public void onSelectionChanged(SelectionEvent selectionEvent) {
                updateButtons();
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

        if (initialSelection != null) {
            assignedGrid.setData(initialSelection);
            for (ListGridRecord record : initialSelection) {
                selection.add(record.getAttributeAsInt("id"));
            }
        }

        addMember(hlayout);

    }

    protected void updateButtons() {
        addButton.setDisabled(!availableGrid.anySelected() || availableGrid.getTotalRows() == 0);
        removeButton.setDisabled(!assignedGrid.anySelected() || assignedGrid.getTotalRows() == 0);
        addAllButton.setDisabled(availableGrid.getTotalRows() == 0);
        removeAllButton.setDisabled(assignedGrid.getTotalRows() == 0);
    }

    protected void select(ListGridRecord[] records) {
        availableGrid.deselectAllRecords();
        for (ListGridRecord record : records) {
            record.setEnabled(false);
            selection.add(record.getAttributeAsInt("id"));
        }
    }

    protected void deselect(ListGridRecord[] records) {
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
}
