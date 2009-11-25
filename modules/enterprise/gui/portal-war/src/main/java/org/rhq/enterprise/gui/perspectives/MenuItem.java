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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/** Features for each menu item.  A menu item can be as simple as a logo, 
 *  menu separator bar or as complicated as a root menu item with an arbitrary 
 *  amount of child menuitems.
 * 
 *  @author Simeon Pinder
 *
 */
public class MenuItem {

    //url to icon image
    private String iconUrl = "";
    //width of icon
    private int iconWidth = 0;
    //height of icon
    private int iconHeight = 0;
    //url to direct user to onclick
    private String url = "";
    //The label to be displayed for this menu item
    private String name = "";
    //whether this menu item is to be enabled/visible
    private boolean rendered = true;
    //whether this menuItem represent a menu separator
    private boolean isMenuSeparator = false;
    //includes N child MenuItem instances
    private List<MenuItem> childMenuItems = new ArrayList<MenuItem>();
    //Stores a unique reference to the ExtensionPoint key for this item.
    private String extensionKey = "";
    //whether this menuItem(relevant to top menu items only) is aligned from far left
    private boolean alignLeft = true;

    //default no args. To satisfy basic bean functionality.
    MenuItem() {
    }

    MenuItem(String iconUrl, String url, String name) {
        this.iconUrl = iconUrl;
        this.url = url;
        this.name = name;
    }

    MenuItem(String iconUrl, String url, String name, List<MenuItem> children, String key, HashMap<String, MenuItem> map) {
        this(iconUrl, url, name);
        children.add(this);
        map.put(key, this);
        this.extensionKey = key;
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

    public boolean isMenuSeparator() {
        return isMenuSeparator;
    }

    public void setMenuSeparator(boolean isMenuSeparator) {
        this.isMenuSeparator = isMenuSeparator;
    }

    public void setChildMenuItems(List<MenuItem> childMenuItems) {
        this.childMenuItems = childMenuItems;
    }

    public List<MenuItem> getChildMenuItems() {
        return childMenuItems;
    }

    public String getExtensionKey() {
        return extensionKey;
    }

    public void setExtensionKey(String extensionKey) {
        this.extensionKey = extensionKey;
    }

    public boolean isAlignLeft() {
        return alignLeft;
    }

    public void setAlignLeft(boolean alignLeft) {
        this.alignLeft = alignLeft;
    }
}
