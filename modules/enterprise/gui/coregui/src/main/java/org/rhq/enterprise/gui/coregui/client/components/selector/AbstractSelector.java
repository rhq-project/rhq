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
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.grid.events.RecordDropEvent;
import com.smartgwt.client.widgets.grid.events.RecordDropHandler;
import com.smartgwt.client.widgets.grid.events.SelectionChangedHandler;
import com.smartgwt.client.widgets.grid.events.SelectionEvent;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.VStack;

import org.rhq.enterprise.gui.coregui.client.util.RPCDataSource;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableListGrid;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableTransferImgButton;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

/**
 * @author Greg Hinkle
 */
public abstract class AbstractSelector<T> extends LocatableVLayout {

    protected HashSet<Integer> selection = new HashSet<Integer>();
    protected HashSet<String> selectionAlternateIds = new HashSet<String>();

    protected ListGridRecord[] initialSelection;
    protected DynamicForm availableFilterForm;
    protected HLayout hlayout;
    protected LocatableListGrid availableGrid;
    protected LocatableListGrid assignedGrid;
    protected RPCDataSource<T> datasource;

    protected TransferImgButton addButton;
    protected TransferImgButton removeButton;
    protected TransferImgButton addAllButton;
    protected TransferImgButton removeAllButton;
    private static String SELECTOR_KEY = "id";
    protected Criteria latestCriteria;

    public AbstractSelector(String locatorId) {
        super(locatorId);
        hlayout = new HLayout();
        availableGrid = new LocatableListGrid(extendLocatorId("availableGrid"));
        assignedGrid = new LocatableListGrid(extendLocatorId("assignedGrid"));
    }

    public void setAssigned(ListGridRecord[] assignedRecords) {
        initialSelection = assignedRecords;
    }

    /** List of indices for the records being transferred.
     * 
     * @return
     */
    public HashSet<Integer> getSelection() {
        return selection;
    }

    public HashSet<String> getSelectionAlternateIds() {
        return selectionAlternateIds;
    }

    protected abstract DynamicForm getAvailableFilterForm();

    protected abstract RPCDataSource<T> getDataSource();

    protected abstract Criteria getLatestCriteria(DynamicForm availableFilterForm);

    @Override
    protected void onInit() {
        super.onInit();

        availableFilterForm = getAvailableFilterForm();
        if (availableFilterForm != null) {
            addMember(availableFilterForm);
        }

        hlayout.setAlign(VerticalAlignment.BOTTOM);

        // LEFT SIDE 
        availableGrid.setHeight(300);
        availableGrid.setCanDragRecordsOut(true);
        availableGrid.setCanAcceptDroppedRecords(true);
        availableGrid.setDragTrackerMode(DragTrackerMode.ICON);
        availableGrid.setTrackerImage(new ImgProperties("types/Service_up_16.png", 16, 16));
        availableGrid.setDragDataAction(DragDataAction.COPY);
        datasource = getDataSource();
        availableGrid.setDataSource(datasource);
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
                                // TODO until http://code.google.com/p/smartgwt/issues/detail?id=490 is fixed always go to the server for data
                                availableGrid.invalidateCache();

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

        addButton = new LocatableTransferImgButton(this.getLocatorId(), TransferImgButton.RIGHT);
        addButton.setDisabled(true);
        removeButton = new LocatableTransferImgButton(this.getLocatorId(), TransferImgButton.LEFT);
        removeButton.setDisabled(true);
        addAllButton = new LocatableTransferImgButton(this.getLocatorId(), TransferImgButton.RIGHT_ALL);
        removeAllButton = new LocatableTransferImgButton(this.getLocatorId(), TransferImgButton.LEFT_ALL);
        removeAllButton.setDisabled(true);

        moveButtonStack.addMember(addButton);
        moveButtonStack.addMember(removeButton);
        moveButtonStack.addMember(addAllButton);
        moveButtonStack.addMember(removeAllButton);

        hlayout.addMember(moveButtonStack);

        // RIGHT SIDE
        assignedGrid.setHeight(300);
        assignedGrid.setCanReorderRecords(true);
        assignedGrid.setCanDragRecordsOut(true);
        assignedGrid.setDragTrackerMode(DragTrackerMode.ICON);
        assignedGrid.setTrackerImage(new ImgProperties("types/Service_up_16.png", 16, 16));
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

        availableGrid.addRecordDropHandler(new RecordDropHandler() {
            public void onRecordDrop(RecordDropEvent recordDropEvent) {
                deselect(recordDropEvent.getDropRecords());
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
                if (getSelectorKey().equalsIgnoreCase("id")) {
                    selection.add(record.getAttributeAsInt(getSelectorKey()));
                } else {
                    selectionAlternateIds.add(record.getAttributeAsString(getSelectorKey()));
                }
            }
        }

        updateButtons(); // initialize their state

        addMember(hlayout);
    }

    protected ClickHandler getAddButtonClickHandler() {
        return new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                assignedGrid.transferSelectedData(availableGrid);
                select(assignedGrid.getSelection());
                updateButtons();
            }
        };
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        availableGrid.destroy();
        assignedGrid.destroy();
        addButton.destroy();
        removeButton.destroy();
        addAllButton.destroy();
        removeAllButton.destroy();
        if (availableFilterForm != null) {
            availableFilterForm.destroy();
        }
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
            if (getSelectorKey().equalsIgnoreCase("id")) {
                selection.add(record.getAttributeAsInt(getSelectorKey()));
            } else {
                selectionAlternateIds.add(record.getAttributeAsString(getSelectorKey()));
            }
        }
        assignedGrid.markForRedraw();
    }

    protected void deselect(ListGridRecord[] records) {
        HashSet<Integer> toRemove = new HashSet<Integer>();
        HashSet<String> toRemoveStringIds = new HashSet<String>();
        if (getSelectorKey().equalsIgnoreCase("id")) {//integer id based
            for (ListGridRecord record : records) {
                toRemove.add(record.getAttributeAsInt(getSelectorKey()));
            }
            selection.removeAll(toRemove);

            for (Integer id : toRemove) {
                Record r = null;
                r = availableGrid.getDataAsRecordList().find(getSelectorKey(), id);
                if (r != null) {
                    ((ListGridRecord) r).setEnabled(true);
                }
            }
        } else {//not using 'id' as selection criteria
            for (ListGridRecord record : records) {
                toRemoveStringIds.add(record.getAttributeAsString(getSelectorKey()));
            }
            selectionAlternateIds.removeAll(toRemoveStringIds);

            for (String id : toRemoveStringIds) {
                Record r = null;
                r = availableGrid.getDataAsRecordList().find(getSelectorKey(), id);
                if (r != null) {
                    ((ListGridRecord) r).setEnabled(true);
                }
            }
        }
        availableGrid.markForRedraw();
    }

    public LocatableListGrid getAvailableGrid() {
        return availableGrid;
    }

    public LocatableListGrid getAssignedGrid() {
        return assignedGrid;
    }

    protected String getSelectorKey() {
        return SELECTOR_KEY;
    }

    //    public TransferImgButton getAddButton() {
    //        return addButton;
    //    }
}
