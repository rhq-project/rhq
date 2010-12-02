/*
 * RHQ Management Platform
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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
package org.rhq.enterprise.gui.coregui.client.components.view;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Ian Springer
 */
public class NavigationSection {
    private ViewName viewName;
    private List<NavigationItem> navigationItems;
    private Map<String, NavigationItem> navigationItemsByName;

    public NavigationSection(ViewName name, NavigationItem... navigationItems) {
        this.viewName = name;
        this.navigationItems = Arrays.asList(navigationItems);
        this.navigationItemsByName = new LinkedHashMap<String, NavigationItem>(navigationItems.length);
        for (NavigationItem navigationItem : navigationItems) {
            this.navigationItemsByName.put(navigationItem.getName(), navigationItem);
        }
    }

    public ViewName getViewName() {
        return viewName;
    }

    public String getName() {
        return viewName.getName();
    }

    public String getTitle() {
        return viewName.getTitle();
    }

    public List<NavigationItem> getNavigationItems() {
        return navigationItems;
    }

    public NavigationItem getNavigationItem(String name) {
        return navigationItemsByName.get(name);
    }
}
