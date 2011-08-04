package org.rhq.enterprise.gui.coregui.client.components.sorter;

import com.smartgwt.client.widgets.grid.ListGridRecord;

/**
 *
 */
public class ListOrderChangedEvent {

    private ListGridRecord[] items;

    public ListOrderChangedEvent(ListGridRecord[] items) {
        this.items = items;
    }

    public ListGridRecord[] getItems() {
        return this.items;
    }

}
