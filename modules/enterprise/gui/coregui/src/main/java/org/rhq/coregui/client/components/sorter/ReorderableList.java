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
package org.rhq.coregui.client.components.sorter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.gwt.event.shared.HandlerRegistration;
import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.data.DataSource;
import com.smartgwt.client.types.ListGridFieldType;
import com.smartgwt.client.types.SelectionStyle;
import com.smartgwt.client.widgets.grid.HoverCustomizer;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.grid.events.RecordDropEvent;
import com.smartgwt.client.widgets.grid.events.RecordDropHandler;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.SectionStack;
import com.smartgwt.client.widgets.layout.SectionStackSection;

import org.rhq.coregui.client.util.enhanced.EnhancedVLayout;

/**
 * @author Ian Springer
 */
public class ReorderableList extends EnhancedVLayout {

    protected ListGridRecord[] initialSelection;
    protected HLayout hlayout;
    protected ListGrid listGrid;
    /** an icon displayed next to each item in the list grids, or null if no icon should be displayed */
    private String itemIcon;
    /** the item title (i.e. display name), which should be plural and capitalized, e.g. "Resource Groups", "Roles". */
    private String itemTitle;
    private String nameFieldTitle = MSG.common_title_name();

    private Set<ListOrderChangedHandler> listOrderChangedHandlers = new HashSet<ListOrderChangedHandler>();

    private boolean isReadOnly;

    private HoverCustomizer nameHoverCustomizer;

    private DataSource dataSource;
    private Criteria criteria;

    public ReorderableList(ListGridRecord[] records, String itemTitle, String itemIcon) {
        this(false, records, itemTitle, itemIcon, null, null, null);
    }

    public ReorderableList(ListGridRecord[] records, String itemTitle, String itemIcon,
        HoverCustomizer nameHoverCustomizer) {
        this(false, records, itemTitle, itemIcon, nameHoverCustomizer, null, null);
    }

    public ReorderableList(ListGridRecord[] records, String itemTitle, String itemIcon,
        HoverCustomizer nameHoverCustomizer, DataSource dataSource, Criteria criteria) {
        this(false, records, itemTitle, itemIcon, nameHoverCustomizer, dataSource, criteria);
    }

    public ReorderableList(boolean isReadOnly, ListGridRecord[] records, String itemTitle, String itemIcon,
        HoverCustomizer nameHoverCustomizer) {
        this(isReadOnly, records, itemTitle, itemIcon, nameHoverCustomizer, null, null);
    }

    public ReorderableList(boolean isReadOnly, ListGridRecord[] records, String itemTitle, String itemIcon,
        HoverCustomizer nameHoverCustomizer, DataSource dataSource, Criteria criteria) {
        super();

        this.isReadOnly = isReadOnly;

        setWidth100();
        setMargin(7);

        this.hlayout = new HLayout();
        this.listGrid = new ListGrid();
        this.initialSelection = records;
        this.listGrid.setRecords(records);

        if (this.isReadOnly) {
            this.listGrid.setSelectionType(SelectionStyle.NONE);
        }

        this.itemTitle = itemTitle;
        this.itemIcon = itemIcon;

        this.nameHoverCustomizer = nameHoverCustomizer;

        this.dataSource = dataSource;
        this.criteria = criteria;
    }

    /**
     * Returns the {@link com.smartgwt.client.data.Record record}s in the order currently specified by the user.
     *
     * @return the {@link com.smartgwt.client.data.Record record}s in the order currently specified by the user
     */
    public ListGridRecord[] getRecords() {
        return this.listGrid.getRecords();
    }

    /**
     *
     */
    public void setRecords(ListGridRecord[] records) {
        this.initialSelection = records;
        this.listGrid.setRecords(records);
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
        if (null != this.nameHoverCustomizer) {
            nameField.setShowHover(true);
            nameField.setHoverCustomizer(this.nameHoverCustomizer);
        }
        fields.add(nameField);

        if (dataSource != null) {
            this.listGrid.setDataSource(dataSource, fields.toArray(new ListGridField[fields.size()]));
            this.listGrid.setAutoFetchData(true);
        } else {
            this.listGrid.setFields(fields.toArray(new ListGridField[fields.size()]));
        }

        if (criteria != null) {
            this.listGrid.setInitialCriteria(criteria);
        }

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
            handler.onListOrderChanged(new ListOrderChangedEvent(this.listGrid.getSelectedRecords()));
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

}
