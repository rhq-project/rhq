/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.rhq.coregui.client.help;

import java.util.ArrayList;
import java.util.List;
import java.util.MissingResourceException;

import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.Label;

import org.rhq.core.domain.common.ProductInfo;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.IconEnum;
import org.rhq.coregui.client.MessageConstants;
import org.rhq.coregui.client.components.AboutModalWindow;
import org.rhq.coregui.client.components.TitleBar;
import org.rhq.coregui.client.components.view.AbstractSectionedLeftNavigationView;
import org.rhq.coregui.client.components.view.NavigationItem;
import org.rhq.coregui.client.components.view.NavigationSection;
import org.rhq.coregui.client.components.view.ViewFactory;
import org.rhq.coregui.client.components.view.ViewName;
import org.rhq.coregui.client.util.enhanced.EnhancedVLayout;

/**
 * The Help top-level view.
 *
 * @author Jay Shaughnessy
 */
public class HelpView extends AbstractSectionedLeftNavigationView {

    public static final ViewName VIEW_ID = new ViewName("Help", MSG.common_title_help(), IconEnum.HELP);

    public static final ViewName SECTION_PRODUCT_VIEW_ID = new ViewName("Product", MSG.view_help_section_product());
    private final ProductInfo productInfo = CoreGUI.get().getProductInfo();
    private final MessageConstants messageConstants = CoreGUI.getMessageConstants();
    private boolean contentFromProductInfo;

    public HelpView() {
        super(VIEW_ID.getName());
    }

    @Override
    protected List<NavigationSection> getNavigationSections() {
        List<NavigationSection> sections = new ArrayList<NavigationSection>();

        NavigationSection docSection = buildProductSection();
        sections.add(docSection);

        addUrlSections(sections);

        return sections;
    }

    @Override
    protected EnhancedVLayout defaultView() {
        EnhancedVLayout vLayout = new EnhancedVLayout();
        vLayout.setWidth100();

        TitleBar titleBar = new TitleBar(MSG.common_title_help(), VIEW_ID.getIcon().getIcon24x24Path());
        vLayout.addMember(titleBar);

        Label label = new Label(MSG.view_helpTop_description());
        label.setPadding(10);
        vLayout.addMember(label);

        return vLayout;
    }

    private NavigationSection buildProductSection() {

        NavigationItem aboutItem = new NavigationItem(new ViewName("AboutBox", MSG.view_help_section_product_about(),
            IconEnum.HELP), new ViewFactory() {
                public Canvas createView() {
                    final AboutModalWindow aboutModalWindow = new AboutModalWindow(productInfo);
                    aboutModalWindow.show();
                    return aboutModalWindow;
                }
            });
        aboutItem.setRefreshRequired(true);
        return new NavigationSection(SECTION_PRODUCT_VIEW_ID, aboutItem);
    }

    private void addUrlSections(List<NavigationSection> sections) {
        this.selectContentLocation();

        int numSections = Integer.valueOf(this.getContent("view_help_section_count"));

        for (int i = 1; i <= numSections; ++i) {
            int numItems = Integer.valueOf(this.getContent("view_help_section_" + i + "_item_count"));
            NavigationItem[] items = new NavigationItem[numItems];
            String sectionTitle = this.getContent("view_help_section_" + i + "_title");

            for (int j = 1; j <= numItems; ++j) {
                String title = this.getContent("view_help_section_" + i + "_propTitle_" + j);
                final String url = this.getContent("view_help_section_" + i + "_propUrl_" + j);
                String icon;
                try {
                    icon = this.getContent("view_help_section_" + i + "_propIcon_" + j);
                    if (icon == null) {
                        icon = IconEnum.HELP.getIcon16x16Path();
                    }
                } catch (MissingResourceException e) {
                    icon = IconEnum.HELP.getIcon16x16Path();
                }

                final String itemName = "Section" + i + "Item" + j;
                NavigationItem item = new NavigationItem(new ViewName(itemName, title), icon, new ViewFactory() {
                    public Canvas createView() {
                        com.google.gwt.user.client.Window.open(url, "_blank", "");
                        return null;
                    }
                });
                items[j - 1] = item;
            }

            NavigationSection section = new NavigationSection(new ViewName("Section" + i, sectionTitle), items);
            sections.add(section);
        }
    }

    /**
     * Preselects content location.
     *
     * I18N GWT bakes all the messages into the compiled Javascript (it is impossible to change anything
     * without recompiling the entire project). However, there is a need to dynamically change the help
     * content after the code is compiled and shipped. ProductInfo is dynamically loaded at runtime from disk.
     * This implementation allows runtime location selection for help page content:
     * 1) ProductInfo
     * 2) I18N GWT system
     *
     * NOTE: All the content is loaded from a single location and production info properties file gets priority.
     */
    private void selectContentLocation() {
        if (productInfo.getHelpViewContent().containsKey("view_help_section_count")) {
            //use the product info file for help content
            this.contentFromProductInfo = true;
        } else {
            //use the I18N mechanism for help content
            this.contentFromProductInfo = false;
        }
    }

    /**
     * Retrieves help view content from the selected content location.
     *
     * @return help view content
     */
    private String getContent(String key) {
        if (this.contentFromProductInfo) {
            return productInfo.getHelpViewContent().get(key);
        } else {
            return messageConstants.getString(key);
        }
    }
}