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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Timer;
import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.data.DSCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.DragDataAction;
import com.smartgwt.client.types.DragTrackerMode;
import com.smartgwt.client.types.ListGridFieldType;
import com.smartgwt.client.types.SelectionStyle;
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
import com.smartgwt.client.widgets.layout.LayoutSpacer;
import com.smartgwt.client.widgets.layout.SectionStack;
import com.smartgwt.client.widgets.layout.SectionStackSection;
import com.smartgwt.client.widgets.layout.VStack;

import org.rhq.enterprise.gui.coregui.client.util.RPCDataSource;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableListGrid;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableTransferImgButton;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

/**
 * @author Greg Hinkle
 * @author Ian Springer
 */
public abstract class AbstractSelector<T> extends LocatableVLayout {

    private static final String SELECTOR_KEY = "id";

    protected ListGridRecord[] initialSelection;
    protected List<Record> availableRecords;
    protected DynamicForm availableFilterForm;
    protected HLayout hlayout;
    protected LocatableListGrid availableGrid;
    protected LocatableListGrid assignedGrid;
    protected RPCDataSource<T> datasource;

    private Set<AssignedItemsChangedHandler> assignedItemsChangedHandlers = new HashSet<AssignedItemsChangedHandler>();

    protected TransferImgButton addButton;
    protected TransferImgButton removeButton;
    protected TransferImgButton addAllButton;
    protected TransferImgButton removeAllButton;

    protected Criteria latestCriteria;

    private boolean isReadOnly;

    public AbstractSelector(String locatorId) {
        this(locatorId, false);
    }

    public AbstractSelector(String locatorId, boolean isReadOnly) {
        super(locatorId);

        this.isReadOnly = isReadOnly;

        setWidth100();
        setMargin(7);

        this.hlayout = new HLayout();
        this.assignedGrid = new LocatableListGrid(extendLocatorId("assignedGrid"));

        if (this.isReadOnly) {
            this.assignedGrid.setSelectionType(SelectionStyle.NONE);
        } else {
            this.availableGrid = new LocatableListGrid(extendLocatorId("availableGrid"));
        }
    }

    public void setAssigned(ListGridRecord[] assignedRecords) {
        initialSelection = assignedRecords;
    }

    /**
     * Returns the set of currently selected {@link Record record}s.
     *
     * @return the set of currently selected {@link Record record}s
     */
    public ListGridRecord[] getSelectedRecords() {
        return this.assignedGrid.getRecords();
    }

    /**
     * Returns the set of currently selected {@link T item}s.
     *
     * @return the set of currently selected {@link T item}s
     */
    public Set<T> getSelectedItems() {
        ListGridRecord[] selectedRecords = this.assignedGrid.getRecords();
        return getDataSource().buildDataObjects(selectedRecords);
    }

    /**
     * Returns the IDs of the currently selected items
     * 
     * @return the IDs of the currently selected items
     */
    public Set<Integer> getSelection() {
        ListGridRecord[] selectedRecords = this.assignedGrid.getRecords();
        Set<Integer> ids = new HashSet<Integer>(selectedRecords.length);
        for (ListGridRecord selectedRecord : selectedRecords) {
            Integer id = selectedRecord.getAttributeAsInt(getSelectorKey());
            ids.add(id);
        }
        return ids;
    }

    protected abstract DynamicForm getAvailableFilterForm();

    protected abstract RPCDataSource<T> getDataSource();

    protected abstract Criteria getLatestCriteria(DynamicForm availableFilterForm);

    /**
     * Subclasses can override this if they want an icon displayed next to each item in the list grids.
     *
     * @return the icon to be displayed, or null if no icon should be displayed
     */
    protected String getItemIcon() {
        return null;
    }

    @Override
    protected void onInit() {
        super.onInit();

        this.hlayout.setAlign(Alignment.LEFT);

        if (!this.isReadOnly) {
            // LEFT SIDE
            this.availableFilterForm = getAvailableFilterForm();
            if (this.availableFilterForm != null) {
                addMember(this.availableFilterForm);
                LayoutSpacer spacer = new LayoutSpacer();
                spacer.setHeight(10);
                addMember(spacer);
            }

            SectionStack availableItemsStack = buildAvailableItemsStack();
            hlayout.addMember(availableItemsStack);

            // CENTER BUTTONS
            VStack moveButtonStack = buildButtonStack();
            hlayout.addMember(moveButtonStack);
        }

        // RIGHT SIDE
        SectionStack assignedItemsStack = buildAssignedItemsStack();
        this.hlayout.addMember(assignedItemsStack);

        // initialize the state of the buttons - allows subclasses to tweek buttons on init time
        updateButtonEnablement();

        addMember(this.hlayout);
    }

    private SectionStack buildAvailableItemsStack() {
        SectionStack availableSectionStack = new SectionStack();
        availableSectionStack.setWidth(300);
        availableSectionStack.setHeight(250);

        SectionStackSection availableSection = new SectionStackSection(getAvailableItemsGridTitle());
        availableSection.setCanCollapse(false);
        availableSection.setExpanded(true);

        // Drag'n'Drop Settings
        this.availableGrid.setCanReorderRecords(true);
        this.availableGrid.setCanDragRecordsOut(true);
        this.availableGrid.setDragDataAction(DragDataAction.MOVE);
        if (getItemIcon() != null) {
            this.availableGrid.setDragTrackerMode(DragTrackerMode.ICON);
            this.availableGrid.setTrackerImage(new ImgProperties(getItemIcon(), 16, 16));
        }
        this.availableGrid.setCanAcceptDroppedRecords(true);

        this.availableGrid.setLoadingMessage(MSG.common_msg_loading());
        this.availableGrid.setEmptyMessage(MSG.common_msg_noItemsToShow());

        List<ListGridField> availableFields = new ArrayList<ListGridField>();
        String itemIcon = getItemIcon();
        if (itemIcon != null) {
            ListGridField iconField = new ListGridField("icon", 25);
            iconField.setType(ListGridFieldType.ICON);
            iconField.setCellIcon(itemIcon);
            iconField.setShowDefaultContextMenu(false);
            availableFields.add(iconField);
        }
        ListGridField nameField = new ListGridField(getNameField(), MSG.common_title_name());
        availableFields.add(nameField);
        this.availableGrid.setFields(availableFields.toArray(new ListGridField[availableFields.size()]));

        availableSection.setItems(this.availableGrid);
        availableSectionStack.addSection(availableSection);

        // Load data.
        this.datasource = getDataSource();
        populateAvailableGrid(new Criteria());

        if (this.availableFilterForm != null) {
            this.availableFilterForm.addItemChangedHandler(new ItemChangedHandler() {
                public void onItemChanged(ItemChangedEvent itemChangedEvent) {
                    latestCriteria = getLatestCriteria(availableFilterForm);

                    Timer timer = new Timer() {
                        @Override
                        public void run() {
                            if (latestCriteria != null) {
                                Criteria criteria = latestCriteria;
                                latestCriteria = null;
                                populateAvailableGrid(criteria);
                            }
                        }
                    };
                    timer.schedule(500);
                }
            });
        }

        // Add event handlers.
        
        this.availableGrid.addSelectionChangedHandler(new SelectionChangedHandler() {
            public void onSelectionChanged(SelectionEvent selectionEvent) {
                updateButtonEnablement();
            }
        });

        this.availableGrid.addDoubleClickHandler(new DoubleClickHandler() {
            public void onDoubleClick(DoubleClickEvent event) {
                addSelectedRows();
            }
        });

        this.availableGrid.addRecordDropHandler(new RecordDropHandler() {
            public void onRecordDrop(RecordDropEvent recordDropEvent) {
                removeSelectedRows();
                recordDropEvent.cancel();
            }
        });

        return availableSectionStack;
    }

    private void populateAvailableGrid(Criteria criteria) {
        // TODO until http://code.google.com/p/smartgwt/issues/detail?id=490 is fixed always go to the server for data
        this.datasource.invalidateCache();
        this.datasource.fetchData(criteria, new DSCallback() {
            public void execute(DSResponse response, Object rawData, DSRequest request) {
                try {
                    availableRecords = new ArrayList<Record>();
                    Record[] allRecords = response.getData();
                    ListGridRecord[] assignedRecords = assignedGrid.getRecords();
                    if (assignedRecords != null && assignedRecords.length != 0) {
                        Set<String> selectedRecordIds = new HashSet<String>(assignedRecords.length);
                        for (Record record : assignedRecords) {
                            String id = record.getAttribute(getSelectorKey());
                            selectedRecordIds.add(id);
                        }
                        for (Record record : allRecords) {
                            String id = record.getAttribute(getSelectorKey());
                            if (!selectedRecordIds.contains(id)) {
                                availableRecords.add(record);
                            }
                        }
                    } else {
                        availableRecords.addAll(Arrays.asList(allRecords));
                    }                    
                    availableGrid.setData(availableRecords.toArray(new Record[availableRecords.size()]));
                } finally {
                    updateButtonEnablement();
                }
            }
        });
    }

    private VStack buildButtonStack() {
        VStack moveButtonStack = new VStack(6);
        moveButtonStack.setWidth(42);
        moveButtonStack.setHeight(250);
        moveButtonStack.setAlign(VerticalAlignment.CENTER);

        this.addButton = new LocatableTransferImgButton(this.getLocatorId(), TransferImgButton.RIGHT);
        this.addButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                addSelectedRows();
            }
        });
        moveButtonStack.addMember(this.addButton);

        this.removeButton = new LocatableTransferImgButton(this.getLocatorId(), TransferImgButton.LEFT);
        this.removeButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                removeSelectedRows();

            }
        });
        moveButtonStack.addMember(this.removeButton);

        this.addAllButton = new LocatableTransferImgButton(this.getLocatorId(), TransferImgButton.RIGHT_ALL);
        this.addAllButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                availableGrid.selectAllRecords();
                addSelectedRows();
            }
        });
        moveButtonStack.addMember(this.addAllButton);

        this.removeAllButton = new LocatableTransferImgButton(this.getLocatorId(), TransferImgButton.LEFT_ALL);
        this.removeAllButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                assignedGrid.selectAllRecords();
                removeSelectedRows();
            }
        });
        moveButtonStack.addMember(this.removeAllButton);

        return moveButtonStack;
    }

    private SectionStack buildAssignedItemsStack() {
        SectionStack assignedSectionStack = new SectionStack();
        assignedSectionStack.setWidth(300);
        assignedSectionStack.setHeight(250);
        assignedSectionStack.setAlign(Alignment.LEFT);

        SectionStackSection assignedSection = new SectionStackSection(getAssignedItemsGridTitle());
        assignedSection.setCanCollapse(false);
        assignedSection.setExpanded(true);

        // Drag'n'Drop Settings
        this.assignedGrid.setCanReorderRecords(true);
        this.assignedGrid.setCanDragRecordsOut(true);
        this.assignedGrid.setDragDataAction(DragDataAction.MOVE);
        if (getItemIcon() != null) {
            this.assignedGrid.setDragTrackerMode(DragTrackerMode.ICON);
            this.assignedGrid.setTrackerImage(new ImgProperties(getItemIcon(), 16, 16));
        }
        this.assignedGrid.setCanAcceptDroppedRecords(true);

        this.assignedGrid.setLoadingMessage(MSG.common_msg_loading());
        this.assignedGrid.setEmptyMessage(MSG.common_msg_noItemsToShow());

        List<ListGridField> assignedFields = new ArrayList<ListGridField>();
        String itemIcon = getItemIcon();
        if (itemIcon != null) {
            ListGridField iconField = new ListGridField("icon", 25);
            iconField.setType(ListGridFieldType.ICON);
            iconField.setCellIcon(itemIcon);
            iconField.setShowDefaultContextMenu(false);
            assignedFields.add(iconField);
        }
        ListGridField nameField = new ListGridField(getNameField(), MSG.common_title_name());
        assignedFields.add(nameField);
        this.assignedGrid.setFields(assignedFields.toArray(new ListGridField[assignedFields.size()]));

        assignedSection.setItems(this.assignedGrid);
        assignedSectionStack.addSection(assignedSection);

        // Load data.
        if (this.initialSelection != null) {
            this.assignedGrid.setData(this.initialSelection);
        }

        if (this.isReadOnly) {
            this.assignedGrid.setDisabled(true);
        } else {
            this.assignedGrid.addSelectionChangedHandler(new SelectionChangedHandler() {
                public void onSelectionChanged(SelectionEvent selectionEvent) {
                    updateButtonEnablement();
                }
            });

            this.assignedGrid.addDoubleClickHandler(new DoubleClickHandler() {
                public void onDoubleClick(DoubleClickEvent event) {
                    removeSelectedRows();
                }
            });

            this.assignedGrid.addKeyPressHandler(new KeyPressHandler() {
                public void onKeyPress(KeyPressEvent event) {
                    if ("Delete".equals(event.getKeyName())) {
                        removeSelectedRows();
                    }
                }
            });

            this.assignedGrid.addRecordDropHandler(new RecordDropHandler() {
                public void onRecordDrop(RecordDropEvent recordDropEvent) {
                    addSelectedRows();
                    recordDropEvent.cancel();
                }
            });
        }

        return assignedSectionStack;
    }

    private void notifyAssignedItemsChangedHandlers() {
        for (AssignedItemsChangedHandler handler : this.assignedItemsChangedHandlers) {
            handler.onSelectionChanged(new AssignedItemsChangedEvent(this.assignedGrid.getSelection()));
        }
    }

    public void reset() {
        this.assignedGrid.setData(this.initialSelection);
        populateAvailableGrid(getLatestCriteria(getAvailableFilterForm()));
    }

    public HandlerRegistration addAssignedItemsChangedHandler(final AssignedItemsChangedHandler handler) {
        this.assignedItemsChangedHandlers.add(handler);
        return new HandlerRegistration() {
            @Override
            public void removeHandler() {
                assignedItemsChangedHandlers.remove(handler);
            }
        };
    }

    /**
     * Moves the rows selected in the assigned grid to the available grid.
     */
    public void removeSelectedRows() {
        moveSelectedData(this.assignedGrid, this.availableGrid);
        notifyAssignedItemsChangedHandlers();
        updateButtonEnablement();
    }

    /**
     * Moves the rows selected in the available grid to the assigned grid.
     */
    public void addSelectedRows() {
        moveSelectedData(this.availableGrid, this.assignedGrid);
        notifyAssignedItemsChangedHandlers();
        updateButtonEnablement();
    }

    private void moveSelectedData(ListGrid sourceGrid, ListGrid targetGrid) {
        targetGrid.transferSelectedData(sourceGrid);
        sourceGrid.removeSelectedData();
    }

    protected String getNameField() {
        return "name";
    }

    /**
     * Return the item title (i.e. display name), which should be plural and capitalized, e.g. "Resource Groups", "Roles".
     *
     * @return the item title (i.e. display name), which should be plural and capitalized, e.g. "Resource Groups", "Roles"
     */
    protected abstract String getItemTitle();

    protected String getAvailableItemsGridTitle() {
        String itemTitle = getItemTitle();
        return MSG.view_selector_available(itemTitle);
    }

    protected String getAssignedItemsGridTitle() {
        String itemTitle = getItemTitle();
        return MSG.view_selector_assigned(itemTitle);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        assignedGrid.destroy();

        if (!isReadOnly) {
            if (availableFilterForm != null) {
                availableFilterForm.destroy();
            }

            availableGrid.destroy();

            addButton.destroy();
            removeButton.destroy();
            addAllButton.destroy();
            removeAllButton.destroy();
        }
    }

    protected void updateButtonEnablement() {
        addButton.setDisabled(!availableGrid.anySelected());
        removeButton.setDisabled(!assignedGrid.anySelected());
        addAllButton.setDisabled(!containsAtLeastOneEnabledRecord(this.availableGrid));
        removeAllButton.setDisabled(!containsAtLeastOneEnabledRecord(this.assignedGrid));
    }

    @Deprecated
    public LocatableListGrid getAvailableGrid() {
        return availableGrid;
    }

    @Deprecated
    public LocatableListGrid getAssignedGrid() {
        return assignedGrid;
    }

    protected String getSelectorKey() {
        return SELECTOR_KEY;
    }

    private static boolean containsAtLeastOneEnabledRecord(ListGrid grid) {
        boolean result = false;
        ListGridRecord[] assignedRecords = grid.getRecords();
        for (ListGridRecord assignedRecord : assignedRecords) {
            if (isEnabled(assignedRecord)) {
                result = true;
                break;
            }
        }
        return result;
    }

    private static boolean isEnabled(ListGridRecord assignedRecord) {
        return assignedRecord.getAttribute("enabled") == null || assignedRecord.getEnabled();
    }

}
