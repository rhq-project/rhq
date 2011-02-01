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
package org.rhq.enterprise.gui.coregui.client.components.sorter;

import com.google.gwt.event.shared.HandlerRegistration;
import com.smartgwt.client.types.Cursor;
import com.smartgwt.client.types.ListGridFieldType;
import com.smartgwt.client.types.SelectionStyle;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.grid.events.RecordDropEvent;
import com.smartgwt.client.widgets.grid.events.RecordDropHandler;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.SectionStack;
import com.smartgwt.client.widgets.layout.SectionStackSection;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableListGrid;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Ian Springer
 */
public class ReorderableList extends LocatableVLayout {

    protected ListGridRecord[] initialSelection;
    protected HLayout hlayout;
    protected LocatableListGrid listGrid;
    /** an icon displayed next to each item in the list grids, or null if no icon should be displayed */
    private String itemIcon;
    /** the item title (i.e. display name), which should be plural and capitalized, e.g. "Resource Groups", "Roles". */
    private String itemTitle;
    private String nameFieldTitle = MSG.common_title_name();

    private Set<ListOrderChangedHandler> listOrderChangedHandlers = new HashSet<ListOrderChangedHandler>();

    private boolean isReadOnly;

    public ReorderableList(String locatorId, ListGridRecord[] records, String itemTitle, String itemIcon) {
        this(locatorId, false, records, itemTitle, itemIcon);
    }

    public ReorderableList(String locatorId, boolean isReadOnly, ListGridRecord[] records, String itemTitle, String itemIcon) {
        super(locatorId);

        this.isReadOnly = isReadOnly;

        setWidth100();
        setMargin(7);

        this.hlayout = new HLayout();
        this.listGrid = new LocatableListGrid(extendLocatorId("listGrid"));
        this.initialSelection = records;
        this.listGrid.setRecords(records);

        if (this.isReadOnly) {
            this.listGrid.setSelectionType(SelectionStyle.NONE);
        }

        this.itemTitle = itemTitle;
        this.itemIcon = itemIcon;
    }

    /**
     * Returns the {@link com.smartgwt.client.data.Record record}s in the order currently specified by the user.
     *
     * @return the {@link com.smartgwt.client.data.Record record}s in the order currently specified by the user
     */
    public ListGridRecord[] getRecords() {
        return this.listGrid.getRecords();
    }

    @Override
    protected void onInit() {
        super.onInit();

        this.listGrid.setWidth(300);
        this.listGrid.setHeight(250);
        this.listGrid.setCanReorderRecords(true);
        // TODO: Change cursor to hand or something like that when user hovers over a record in the grid.
        this.listGrid.setLoadingMessage(MSG.common_msg_loading());
        this.listGrid.setEmptyMessage(MSG.common_msg_noItemsToShow());
        this.listGrid.addRecordDropHandler(new RecordDropHandler() {
            @Override
            public void onRecordDrop(RecordDropEvent event) {
                notifyListOrderChangedHandlers();
            }
        });

        List<ListGridField> fields = new ArrayList<ListGridField>();
        String itemIcon = getItemIcon();
        if (itemIcon != null) {
            ListGridField iconField = new ListGridField("icon", 25);
            iconField.setType(ListGridFieldType.ICON);
            iconField.setCellIcon(itemIcon);
            iconField.setShowDefaultContextMenu(false);
            fields.add(iconField);
        }
        ListGridField nameField = new ListGridField(getNameField(), this.nameFieldTitle);
        fields.add(nameField);
        this.listGrid.setFields(fields.toArray(new ListGridField[fields.size()]));

        if (getItemTitle() != null) {
            SectionStack itemsStack = buildItemsSectionStack();
            this.hlayout.addMember(itemsStack);
        } else {
            this.hlayout.addMember(this.listGrid);
        }

        addMember(this.hlayout);
    }

    private SectionStack buildItemsSectionStack() {
        SectionStack itemsSectionStack = new SectionStack();
        itemsSectionStack.setWidth(300);
        itemsSectionStack.setHeight(250);

        SectionStackSection itemsSection = new SectionStackSection(getItemTitle());
        itemsSection.setCanCollapse(false);
        itemsSection.setExpanded(true);
        itemsSection.setItems(this.listGrid);

        itemsSectionStack.addSection(itemsSection);

        return itemsSectionStack;
    }

    private void notifyListOrderChangedHandlers() {
        for (ListOrderChangedHandler handler : this.listOrderChangedHandlers) {
            handler.onListOrderChanged(new ListOrderChangedEvent(this.listGrid.getSelection()));
        }
    }

    public void reset() {
        this.listGrid.setData(this.initialSelection);
    }

    public HandlerRegistration addListOrderChangedHandler(final ListOrderChangedHandler handler) {
        this.listOrderChangedHandlers.add(handler);
        return new HandlerRegistration() {
            @Override
            public void removeHandler() {
                listOrderChangedHandlers.remove(handler);
            }
        };
    }

    protected String getNameField() {
        return "name";
    }

    public void setNameFieldTitle(String nameFieldTitle) {
        this.nameFieldTitle = nameFieldTitle;
    }

    public String getItemTitle() {
        return itemTitle;
    }

    protected String getItemIcon() {
        return itemIcon;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        this.listGrid.destroy();
    }

}
