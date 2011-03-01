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
package org.rhq.enterprise.gui.coregui.client.inventory.groups.detail.configuration;

import com.smartgwt.client.widgets.Canvas;

import org.rhq.core.domain.resource.group.composite.ResourceGroupComposite;
import org.rhq.enterprise.gui.coregui.client.BookmarkableView;
import org.rhq.enterprise.gui.coregui.client.ViewPath;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

/**
 * A view for group resource configuration history.
 *
 * @author John Mazzitelli
 */
public class HistoryGroupResourceConfigurationView extends LocatableVLayout implements BookmarkableView {
    private final ResourceGroupComposite groupComposite;
    private HistoryGroupResourceConfigurationTable groupHistoryTable;
    private Canvas detailsCanvas = null;

    public HistoryGroupResourceConfigurationView(String locatorId, ResourceGroupComposite groupComposite) {
        super(locatorId);
        this.groupComposite = groupComposite;

        groupHistoryTable = new HistoryGroupResourceConfigurationTable(extendLocatorId("Table"), groupComposite);
        addMember(groupHistoryTable);
    }

    @Override
    public void renderView(ViewPath viewPath) {
        if (viewPath.isEnd()) {
            setVisibleMember(this.groupHistoryTable);
        } else {
            // the details view can be one of two: the "Settings" view which shows the group config properties themselves
            // or "Members" view which shows a tabular set of data, one history row for each individual resource in the group
            // the syntax is "/#####/{Settings,Members}" where ##### is the group history ID
            int groupHistoryId = viewPath.getCurrentAsInt();
            viewPath.next();
            boolean configView = false;
            if (viewPath.isEnd()) {
                configView = true; // if nothing follows the ID, the default view to show is the config properties
            } else {
                String currentPath = viewPath.getCurrent().getPath();
                if ("Settings".equals(currentPath)) { // do not i18n this string, its a URL fragment
                    configView = true;
                } else if ("Members".equals(currentPath)) { // do not i18n this string, its a URL fragment
                    configView = false;
                } else {
                    throw new IllegalArgumentException("Cannot render page - invalid URL: " + currentPath);
                }
            }

            if (detailsCanvas != null) {
                removeMember(detailsCanvas);
                this.detailsCanvas.destroy();
            }

            if (configView) {
                detailsCanvas = new HistoryGroupResourceConfigurationSettings(extendLocatorId("SettingsView"),
                    this.groupComposite, groupHistoryId);
            } else {
                detailsCanvas = new HistoryGroupResourceConfigurationMembers(extendLocatorId("MembersView"),
                    this.groupComposite, groupHistoryId);
            }
            addMember(detailsCanvas);
            setVisibleMember(detailsCanvas);
        }
    }
}
