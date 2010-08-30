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
package org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.configuration;

import com.smartgwt.client.widgets.Window;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.enterprise.gui.coregui.client.components.configuration.ConfigurationEditor;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableLayout;

/**
 * @author Greg Hinkle
 */
public class ConfigurationHistoryDetailView extends LocatableLayout {

    private ConfigurationDefinition definition;
    private Configuration configuration;

    public ConfigurationHistoryDetailView(String locatorId) {
        super(locatorId);
    }

    @Override
    protected void onDraw() {
        super.onDraw();

        if (definition != null && configuration != null) {
            setup();
        }
    }

    public void setConfiguration(ConfigurationDefinition definition, Configuration configuration) {
        this.definition = definition;
        this.configuration = configuration;
        setup();
    }

    private void setup() {
        if (getChildren().length > 0)
            getChildren()[0].destroy();

        ConfigurationEditor editor = new ConfigurationEditor(getLocatorId(), definition, configuration);
        editor.setReadOnly(true);
        addMember(editor);
        markForRedraw();
    }

    public void displayInDialog() {

        Window window = new Window();
        window.setTitle("Configuration Details");
        window.setWidth(800);
        window.setHeight(800);
        window.setIsModal(true);
        window.setShowModalMask(true);
        window.setCanDragResize(true);
        window.centerInPage();
        window.addItem(this);
        window.show();
    }
}
