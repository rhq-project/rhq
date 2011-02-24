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
package org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.inventory;

import com.smartgwt.client.widgets.Canvas;

import org.rhq.core.domain.resource.composite.ResourceComposite;
import org.rhq.enterprise.gui.coregui.client.BookmarkableView;
import org.rhq.enterprise.gui.coregui.client.ViewPath;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

/**
 * A view for resource plugin configuration history.
 *
 * @author John Mazzitelli
 */
public class HistoryPluginConfigurationView extends LocatableVLayout implements BookmarkableView {
    private final ResourceComposite resourceComposite;
    private HistoryPluginConfigurationTable historyTable;
    private Canvas detailsCanvas = null;

    public HistoryPluginConfigurationView(String locatorId, ResourceComposite composite) {
        super(locatorId);
        this.resourceComposite = composite;

        historyTable = new HistoryPluginConfigurationTable(extendLocatorId("Table"), composite);
        addMember(historyTable);
    }

    @Override
    public void renderView(ViewPath viewPath) {
        if (viewPath.isEnd()) {
            setVisibleMember(this.historyTable);
        } else {
            int historyId = viewPath.getCurrentAsInt();

            if (detailsCanvas != null) {
                removeMember(detailsCanvas);
                this.detailsCanvas.destroy();
            }

            detailsCanvas = new HistoryPluginConfigurationSettings(extendLocatorId("SettingsView"),
                this.resourceComposite, historyId);

            addMember(detailsCanvas);
            setVisibleMember(detailsCanvas);
        }
    }
}
