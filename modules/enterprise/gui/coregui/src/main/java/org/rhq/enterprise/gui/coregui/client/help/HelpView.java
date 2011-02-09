/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.client.help;

import java.util.ArrayList;
import java.util.List;
import java.util.MissingResourceException;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.HTMLFlow;

import org.rhq.core.domain.common.ProductInfo;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.MessageConstants;
import org.rhq.enterprise.gui.coregui.client.components.AboutModalWindow;
import org.rhq.enterprise.gui.coregui.client.components.FullHTMLPane;
import org.rhq.enterprise.gui.coregui.client.components.view.AbstractSectionedLeftNavigationView;
import org.rhq.enterprise.gui.coregui.client.components.view.NavigationItem;
import org.rhq.enterprise.gui.coregui.client.components.view.NavigationSection;
import org.rhq.enterprise.gui.coregui.client.components.view.ViewFactory;
import org.rhq.enterprise.gui.coregui.client.components.view.ViewName;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;

/**
 * The Help top-level view.
 *
 * @author Jay Shaughnessy
 */
public class HelpView extends AbstractSectionedLeftNavigationView {
    public static final ViewName VIEW_ID = new ViewName("Help", MSG.common_title_help());

    private static final ViewName SECTION_PRODUCT_VIEW_ID = new ViewName("Product", MSG.view_help_section_product());

    private static ProductInfo PRODUCT_INFO;

    public HelpView() {
        // This is a top level view, so our locator id can simply be our view id.
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
    protected HTMLFlow defaultView() {
        String contents = "<h1>" + MSG.common_title_help() + "</h1>\n" + MSG.view_helpTop_description();
        HTMLFlow flow = new HTMLFlow(contents);
        flow.setPadding(20);
        return flow;
    }

    private NavigationSection buildProductSection() {

        NavigationItem aboutItem = new NavigationItem(new ViewName("AboutBox", MSG.view_help_section_product_about()),
            "[SKIN]/../actions/help.png", new ViewFactory() {
                public Canvas createView() {
                    final AboutModalWindow aboutModalWindow = new AboutModalWindow();
                    if (PRODUCT_INFO == null) {
                        GWTServiceLookup.getSystemService().getProductInfo(new AsyncCallback<ProductInfo>() {
                            public void onFailure(Throwable caught) {
                                CoreGUI.getErrorHandler().handleError(MSG.view_aboutBox_failedToLoad(), caught);
                            }

                            public void onSuccess(ProductInfo result) {
                                PRODUCT_INFO = result;
                                aboutModalWindow.setTitle(MSG.view_aboutBox_title(PRODUCT_INFO.getFullName()));
                            }
                        });
                    } else {
                        aboutModalWindow.setTitle(MSG.view_aboutBox_title(PRODUCT_INFO.getFullName()));
                    }
                    aboutModalWindow.show();
                    return aboutModalWindow;
                }
            });

        return new NavigationSection(SECTION_PRODUCT_VIEW_ID, aboutItem);
    }

    private void addUrlSections(List<NavigationSection> sections) {

        MessageConstants mc = CoreGUI.getMessageConstants();
        int numSections = Integer.valueOf(mc.view_help_section_count());

        for (int i = 1; i <= numSections; ++i) {
            int numItems = Integer.valueOf(mc.getString("view_help_section_" + i + "_item_count"));
            NavigationItem[] items = new NavigationItem[numItems];
            String sectionTitle = mc.getString("view_help_section_" + i + "_title");

            for (int j = 1; j <= numItems; ++j) {
                String title = mc.getString("view_help_section_" + i + "_propTitle_" + j);
                final String url = mc.getString("view_help_section_" + i + "_propUrl_" + j);
                String icon;
                try {
                    icon = mc.getString("view_help_section_" + i + "_propIcon_" + j);
                } catch (MissingResourceException e) {
                    icon = "[SKIN]/../headerIcons/document.png";
                }

                final String itemName = "Section" + i + "Item" + j;
                NavigationItem item = new NavigationItem(new ViewName(itemName, title), icon, new ViewFactory() {
                    public Canvas createView() {
                        return new FullHTMLPane(extendLocatorId(itemName), url);
                    }
                });
                items[j - 1] = item;
            }

            NavigationSection section = new NavigationSection(new ViewName("Section" + i, sectionTitle), items);
            sections.add(section);
        }
    }
}