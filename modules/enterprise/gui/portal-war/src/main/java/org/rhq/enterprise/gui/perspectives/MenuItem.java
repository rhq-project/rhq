/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.enterprise.gui.perspectives;

/** Features for each menu item.
 * 
 *  @author Simeon Pinder
 *
 */
public class MenuItem {

    //constants:begin Extension point map
    public static String menu = "menu";
    public static String menu_BEGIN = "menu.BEGIN";
    public static String logo = "menu.logo";

    public static String overview_BEFORE = "menu.overview_BEFORE";
    public static String overview = "menu.overview";

    public static String resources_BEFORE = "menu.resources_BEFORE";
    public static String resources = "menu.resources";

    //constants:end

    private String iconUrl = "";
    private int iconWidth = 0;
    private int iconHeight = 0;
    private String url = "";
    private String name = "";
    private boolean rendered = true;

    MenuItem(String iconUrl, String url, String name) {
        this.iconUrl = iconUrl;
        this.url = url;
        this.name = name;
    }

    public String getIconUrl() {
        return iconUrl;
    }

    public void setIconUrl(String iconUrl) {
        this.iconUrl = iconUrl;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isRendered() {
        return rendered;
    }

    public void setRendered(boolean render) {
        this.rendered = render;
    }

    public int getIconHeight() {
        return iconHeight;
    }

    public void setIconHeight(int iconHeight) {
        this.iconHeight = iconHeight;
    }

    public int getIconWidth() {
        return iconWidth;
    }

    public void setIconWidth(int iconWidth) {
        this.iconWidth = iconWidth;
    }
}
