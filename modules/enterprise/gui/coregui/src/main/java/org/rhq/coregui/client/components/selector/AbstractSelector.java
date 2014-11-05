/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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
package org.rhq.coregui.client.components.selector;

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
import com.smartgwt.client.widgets.Label;
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
import com.smartgwt.client.widgets.grid.HoverCustomizer;
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

import org.rhq.coregui.client.ImageManager;
import org.rhq.coregui.client.util.RPCDataSource;
import org.rhq.coregui.client.util.enhanced.EnhancedUtility;
import org.rhq.coregui.client.util.enhanced.EnhancedVLayout;
import org.rhq.coregui.client.util.enhanced.EnhancedVStack;

/**
 * @author Greg Hinkle
 * @author Ian Springer
 */
public abstract class AbstractSelector<T, C extends org.rhq.core.domain.criteria.Criteria> extends EnhancedVLayout {

    private static final String SELECTOR_KEY = "id";

    // We only make a single fetch request to load the available records. This is the maximum number we will allow the
    // DataSource to return. If we don't manage to load the entire data set, we'll display a warning message telling the
    // user they need to define some filters.
    private static final int MAX_AVAILABLE_RECORDS = 100;

    protected ListGridRecord[] initialSelection;
    protected List<Record> availableRecords;
    protected DynamicForm availableFilterForm;
    protected HLayout hlayout;
    protected ListGrid availableGrid;
    protected ListGrid assignedGrid;
    protected RPCDataSource<T, C> datasource;

    private Set<AssignedItemsChangedHandler> assignedItemsChangedHandlers = new HashSet<AssignedItemsChangedHandler>();

    protected TransferImgButton addButton;
    protected TransferImgButton removeButton;
    protected TransferImgButton addAllButton;
    protected TransferImgButton removeAllButton;

    protected Criteria latestCriteria;

    private boolean isReadOnly;
    private Label messageLayout;

    public AbstractSelector() {
        this(false);
    }

    public AbstractSelector(boolean isReadOnly) {
        super();

        this.isReadOnly = isReadOnly;

        setWidth100();
        setMargin(7);

        this.hlayout = new HLayout();
        this.assignedGrid = new ListGrid();

        if (this.isReadOnly) {
            this.assignedGrid.setSelectionType(SelectionStyle.NONE);
        } else {
            this.availableGrid = new ListGrid();
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

    protected abstract RPCDataSource<T, C> getDataSource();

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
            this.messageLayout = new Label();
            this.messageLayout.setMargin(3);
            this.messageLayout.setAutoHeight();
            addMember(this.messageLayout);
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

    @Override
    public void destroy() {
        // explicitly destroy non-enhanced member layouts
        EnhancedUtility.destroyMembers(hlayout);
        super.destroy();

        // For reasons unknown, possibly issues in smartgwt's cleanup of VStack and SectionStack, these
        // widgets did not always get destroyed (for example, if something was moved to assigned, but nothing
        // was moved to available - go figure), so destroy them manually when the other cleanup is already done.
        if (null != availableGrid) {
            availableGrid.removeFromParent();
            availableGrid.destroy();
        }
        if (null != assignedGrid) {
            assignedGrid.removeFromParent();
            assignedGrid.destroy();
        }
        if (null != addButton) {
            addButton.removeFromParent();
            addButton.destroy();
        }
        if (null != addAllButton) {
            addAllButton.removeFromParent();
            addAllButton.destroy();
        }
        if (null != removeButton) {
            removeButton.removeFromParent();
            removeButton.destroy();
        }
        if (null != removeAllButton) {
            removeAllButton.removeFromParent();
            removeAllButton.destroy();
        }
    }

    private SectionStack buildAvailableItemsStack() {
        SectionStack availableSectionStack = new SectionStack();
        availableSectionStack.setWidth("*");
        availableSectionStack.setHeight100();

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
        if (supportsNameHoverCustomizer()) {
            nameField.setShowHover(true);
            nameField.setHoverCustomizer(getNameHoverCustomizer());
        }
        availableFields.add(nameField);
        this.availableGrid.setFields(availableFields.toArray(new ListGridField[availableFields.size()]));

        availableSection.setItems(this.availableGrid);
        availableSectionStack.addSection(availableSection);

        this.datasource = getDataSource();
        this.datasource.setDataPageSize(getMaxAvailableRecords());
        // Load data.
        if (this.availableFilterForm != null) {
            // this grabs any initial criteria prior to the first data fetch
            latestCriteria = getLatestCriteria(availableFilterForm);

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
        populateAvailableGrid((null == latestCriteria) ? new Criteria() : latestCriteria);

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

    protected void populateAvailableGrid(Criteria criteria) {
        // TODO until http://code.google.com/p/smartgwt/issues/detail?id=490 is fixed always go to the server for data
        if (datasource == null) {
            datasource = getDataSource();
        }
        datasource.invalidateCache();
        DSRequest requestProperties = new DSRequest();
        requestProperties.setStartRow(0);
        requestProperties.setEndRow(getMaxAvailableRecords());
        this.datasource.fetchData(criteria, new DSCallback() {
            public void execute(DSResponse response, Object rawData, DSRequest request) {
                try {
                    availableRecords = new ArrayList<Record>();
                    Record[] allRecords = response.getData();
                    int assignedNumber = doPostPopulateAvailableGrid(allRecords);
                    int totalRecords = (response.getTotalRows() != null) ? response.getTotalRows() : allRecords.length;
                    int totalAvailableRecords = totalRecords - assignedNumber;
                    if (availableRecords.size() < totalAvailableRecords) {
                        messageLayout.setContents(imgHTML(ImageManager.getAvailabilityYellowIcon())
                            + " "
                            + MSG.view_selector_availableLessThanTotalAvailable(
                                String.valueOf(availableRecords.size()), String.valueOf(totalAvailableRecords),
                                getItemTitle(), getItemTitle()));
                    } else {
                        // Clear the warning message, if any, from the previous fetch.
                        // Note, surprisingly, setContents(null) doesn't work.
                        if (messageLayout != null) {
                            messageLayout.setContents("&nbsp;");
                        }
                    }
                    if (messageLayout != null) {
                        messageLayout.markForRedraw();
                    }
                    availableGrid.setData(availableRecords.toArray(new Record[availableRecords.size()]));
                } finally {
                    updateButtonEnablement();
                }
            }
        }, requestProperties);
    }
    
    protected int doPostPopulateAvailableGrid(Record[] allRecords) {
        ListGridRecord[] assignedRecords = assignedGrid.getRecords();
        if (assignedRecords.length != 0) {
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
        return assignedRecords.length;
    }

    private VStack buildButtonStack() {
        VStack moveButtonStack = new EnhancedVStack(6);
        moveButtonStack.setWidth(42);
        moveButtonStack.setHeight(250);
        moveButtonStack.setAlign(VerticalAlignment.CENTER);

        this.addButton = new TransferImgButton(TransferImgButton.RIGHT);
        this.addButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                addSelectedRows();
            }
        });
        moveButtonStack.addMember(this.addButton);

        this.removeButton = new TransferImgButton(TransferImgButton.LEFT);
        this.removeButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                removeSelectedRows();

            }
        });
        moveButtonStack.addMember(this.removeButton);

        this.addAllButton = new TransferImgButton(TransferImgButton.RIGHT_ALL);
        this.addAllButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                availableGrid.selectAllRecords();
                addSelectedRows();
            }
        });
        moveButtonStack.addMember(this.addAllButton);

        this.removeAllButton = new TransferImgButton(TransferImgButton.LEFT_ALL);
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
        assignedSectionStack.setWidth("*");
        assignedSectionStack.setHeight100();
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
        if (supportsNameHoverCustomizer()) {
            nameField.setShowHover(true);
            nameField.setHoverCustomizer(getNameHoverCustomizer());
        }

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

    protected int getMaxAvailableRecords() {
        return MAX_AVAILABLE_RECORDS;
    }

    protected boolean supportsNameHoverCustomizer() {
        return false;
    }

    protected HoverCustomizer getNameHoverCustomizer() {
        return null;
    }

    private void notifyAssignedItemsChangedHandlers() {
        for (AssignedItemsChangedHandler handler : this.assignedItemsChangedHandlers) {
            handler.onSelectionChanged(new AssignedItemsChangedEvent(this.assignedGrid.getSelectedRecords()));
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

    protected void updateButtonEnablement() {
        if (!isReadOnly) {
            addButton.setDisabled(!availableGrid.anySelected());
            removeButton.setDisabled(!assignedGrid.anySelected());
            addAllButton.setDisabled(!containsAtLeastOneEnabledRecord(this.availableGrid));
            removeAllButton.setDisabled(!containsAtLeastOneEnabledRecord(this.assignedGrid));
        }
    }

    @Deprecated
    public ListGrid getAvailableGrid() {
        return availableGrid;
    }

    @Deprecated
    public ListGrid getAssignedGrid() {
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
