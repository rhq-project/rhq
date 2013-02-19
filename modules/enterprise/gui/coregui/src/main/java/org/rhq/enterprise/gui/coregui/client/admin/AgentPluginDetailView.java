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

package org.rhq.enterprise.gui.coregui.client.admin;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.types.VisibilityMode;
import com.smartgwt.client.widgets.Img;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.CanvasItem;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;
import com.smartgwt.client.widgets.layout.SectionStack;
import com.smartgwt.client.widgets.layout.SectionStackSection;

import org.rhq.core.domain.plugin.Plugin;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.ImageManager;
import org.rhq.enterprise.gui.coregui.client.components.table.TimestampCellFormatter;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.gwt.PluginGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableSectionStack;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

/**
 * Shows details of an agent plugin.
 * 
 * @author John Mazzitelli
 */
public class AgentPluginDetailView extends LocatableVLayout {

    private final PluginGWTServiceAsync pluginManager = GWTServiceLookup.getPluginService();
    private final int pluginId;

    public AgentPluginDetailView(String locatorId, int pluginId) {
        super(locatorId);
        this.pluginId = pluginId;
        setHeight100();
        setWidth100();
    }

    @Override
    protected void onDraw() {
        super.onDraw();

        final LocatableSectionStack sectionStack;

        sectionStack = new LocatableSectionStack(extendLocatorId("stack"));
        sectionStack.setVisibilityMode(VisibilityMode.MULTIPLE);
        sectionStack.setWidth100();
        sectionStack.setHeight100();
        sectionStack.setOverflow(Overflow.AUTO);

        pluginManager.getAgentPlugin(this.pluginId, new AsyncCallback<Plugin>() {
            public void onSuccess(Plugin plugin) {
                prepareDetailsSection(sectionStack, plugin);
                prepareHelpSection(sectionStack, plugin);
            }

            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError(MSG.view_admin_plugins_loadFailure(), caught);
            }
        });

        addMember(sectionStack);
    }

    private void prepareHelpSection(SectionStack stack, Plugin plugin) {
        if (plugin.getHelp() != null && plugin.getHelp().length() > 0) {
            SectionStackSection section = new SectionStackSection(MSG.common_title_help());
            section.setExpanded(true);
            Label help = new Label(plugin.getHelp());
            section.setItems(help);
            stack.addSection(section);
        }
        return;
    }

    private void prepareDetailsSection(SectionStack stack, Plugin plugin) {
        DynamicForm form = new DynamicForm();
        form.setMargin(10);
        form.setWidth100();
        form.setWrapItemTitles(false);
        form.setNumCols(4);

        StaticTextItem nameItem = new StaticTextItem("name", MSG.common_title_name());
        nameItem.setValue(plugin.getName());

        StaticTextItem displayNameItem = new StaticTextItem("displayName", MSG.common_title_display_name());
        displayNameItem.setValue(plugin.getDisplayName());

        StaticTextItem versionItem = new StaticTextItem("version", MSG.common_title_version());
        versionItem.setValue(plugin.getVersion());

        StaticTextItem md5Item = new StaticTextItem("MD5", "MD5");
        md5Item.setValue(plugin.getMD5());

        StaticTextItem pathItem = new StaticTextItem("path", MSG.common_title_path());
        pathItem.setValue(plugin.getPath());

        StaticTextItem ampsItem = new StaticTextItem("ampsVersion", "AMPS " + MSG.common_title_version());
        ampsItem.setValue(plugin.getAmpsVersion());

        StaticTextItem descItem = new StaticTextItem("desc", MSG.common_title_description());
        descItem.setValue(plugin.getDescription());

        StaticTextItem mtimeItem = new StaticTextItem("mtime", MSG.common_title_lastUpdated());
        mtimeItem.setValue(TimestampCellFormatter.format(Long.valueOf(plugin.getMtime()),
            TimestampCellFormatter.DATE_TIME_FORMAT_MEDIUM));

        StaticTextItem kindItem = new StaticTextItem("kind", MSG.common_title_kind());
        switch (plugin.getDeployment()) {
        case AGENT:
            kindItem.setValue(MSG.view_admin_plugins_agent());
            break;
        case SERVER:
            kindItem.setValue(MSG.view_admin_plugins_server());
            break;
        }

        CanvasItem enabledItem = new CanvasItem("enabled", MSG.common_title_enabled());
        Img img = new Img(ImageManager.getAvailabilityIcon(plugin.isEnabled()), 16, 16);
        enabledItem.setCanvas(img);

        form.setItems(displayNameItem, nameItem, versionItem, ampsItem, md5Item, kindItem, descItem, pathItem,
            mtimeItem, enabledItem);

        SectionStackSection section = new SectionStackSection(MSG.common_title_details());
        section.setExpanded(true);
        section.setItems(form);
        stack.addSection(section);

        return;
    }
}
