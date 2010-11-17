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
package org.rhq.enterprise.gui.coregui.client.test;

import java.util.ArrayList;
import java.util.List;

import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.HTMLFlow;

import org.rhq.enterprise.gui.coregui.client.components.view.AbstractSectionedLeftNavigationView;
import org.rhq.enterprise.gui.coregui.client.components.view.NavigationItem;
import org.rhq.enterprise.gui.coregui.client.components.view.NavigationSection;
import org.rhq.enterprise.gui.coregui.client.components.view.ViewFactory;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.selection.ResourceSelector;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.type.ResourceTypeTreeView;
import org.rhq.enterprise.gui.coregui.client.test.configuration.TestConfigurationView;
import org.rhq.enterprise.gui.coregui.client.test.configuration.TestGroupConfigurationView;
import org.rhq.enterprise.gui.coregui.client.test.i18n.TestPluralizationView;

/**
 * The Test top-level view. This view is "hidden", i.e. there are no links to it, so the user must go to the URL
 * directly using their browser.
 *
 * @author Ian Springer
 */
public class TestTopView extends AbstractSectionedLeftNavigationView {

    public static final String VIEW_ID = "Test";

    // view IDs for Inventory section
    private static final String INVENTORY_SECTION_VIEW_ID = "Inventory";

    private static final String PAGE_RESOURCE_SELECTOR = "ResourceSelector";
    private static final String PAGE_TYPE_TREE = "TypeTree";

    // view IDs for Configuration section
    private static final String CONFIGURATION_SECTION_VIEW_ID = "Configuration";

    private static final String PAGE_CONFIG_EDITOR = "ConfigEditor";
    private static final String PAGE_GROUP_CONFIG_EDITOR = "GroupConfigEditor";

    // view IDs for Misc section
    private static final String MISC_SECTION_VIEW_ID = "Misc";
    private static final String PAGE_PLURALIZATION_TEST = "PluralizationTest";

    public TestTopView() {
        // This is a top level view, so our locator id can simply be our view id.
        super(VIEW_ID);
    }

    protected Canvas defaultView() {
        String contents = "<h1>" + MSG.view_testTop_title() + "</h1>\n" + MSG.view_testTop_description();
        HTMLFlow flow = new HTMLFlow(contents);
        flow.setPadding(20);
        return flow;
    }

    @Override
    protected List<NavigationSection> getNavigationSections() {
        List<NavigationSection> sections = new ArrayList<NavigationSection>();

        NavigationSection inventorySection = buildInventorySection();
        sections.add(inventorySection);

        NavigationSection configurationSection = buildConfigurationSection();
        sections.add(configurationSection);

        NavigationSection miscSection = buildMiscSection();
        sections.add(miscSection);

        return sections;
    }

    private NavigationSection buildInventorySection() {
        NavigationItem resourceSelectorItem = new NavigationItem(PAGE_RESOURCE_SELECTOR, null, new ViewFactory() {
            public Canvas createView() {
                return new ResourceSelector(extendLocatorId(PAGE_RESOURCE_SELECTOR));
            }
        });

        NavigationItem typeTreeItem = new NavigationItem(PAGE_TYPE_TREE, null, new ViewFactory() {
            public Canvas createView() {
                return new ResourceTypeTreeView(extendLocatorId(PAGE_TYPE_TREE));
            }
        });

        return new NavigationSection(INVENTORY_SECTION_VIEW_ID, resourceSelectorItem, typeTreeItem);
    }

    private NavigationSection buildConfigurationSection() {
        NavigationItem configEditorItem = new NavigationItem(PAGE_CONFIG_EDITOR, null, new ViewFactory() {
            public Canvas createView() {
                return new TestConfigurationView(extendLocatorId(PAGE_CONFIG_EDITOR));
            }
        });

        NavigationItem groupConfigEditorItem = new NavigationItem(PAGE_GROUP_CONFIG_EDITOR, null, new ViewFactory() {
            public Canvas createView() {
                return new TestGroupConfigurationView(extendLocatorId(PAGE_GROUP_CONFIG_EDITOR));
            }
        });

        return new NavigationSection(CONFIGURATION_SECTION_VIEW_ID, configEditorItem, groupConfigEditorItem);
    }

    private NavigationSection buildMiscSection() {
        NavigationItem pluralizationItem = new NavigationItem(PAGE_PLURALIZATION_TEST, null, new ViewFactory() {
            public Canvas createView() {
                return new TestPluralizationView(extendLocatorId(PAGE_PLURALIZATION_TEST));
            }
        });

        return new NavigationSection(MISC_SECTION_VIEW_ID, pluralizationItem);
    }
}
