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

/**
 * @author Ian Springer
 */
public class NavigationItem {
    private String name;
    private String icon;
    private ViewFactory viewFactory;
    private boolean enabled;

    public NavigationItem(String name, String icon, ViewFactory viewFactory) {
        this(name, icon, viewFactory, true);
    }

    public NavigationItem(String name, String icon, ViewFactory viewFactory, boolean enabled) {
        this.icon = icon;
        this.name = name;
        this.viewFactory = viewFactory;
        this.enabled = enabled;
    }

    public String getName() {
        return name;
    }
    
    public String getIcon() {
        return icon;
    }

    public ViewFactory getViewFactory() {
        return viewFactory;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
