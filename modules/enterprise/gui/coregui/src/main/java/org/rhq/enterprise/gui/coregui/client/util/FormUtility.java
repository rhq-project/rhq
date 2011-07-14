/*
 * RHQ Management Platform
 * Copyright (C) 2010 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.client.util;

import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.FormItem;
import com.smartgwt.client.widgets.form.fields.FormItemIcon;
import com.smartgwt.client.widgets.form.fields.events.IconClickEvent;
import com.smartgwt.client.widgets.form.fields.events.IconClickHandler;

import org.rhq.enterprise.gui.coregui.client.ImageManager;

/**
 * A collection of utility methods for working with SmartGWT {@link DynamicForm}s.
 *
 * @author Joseph Marques
 */
public class FormUtility {

    private FormUtility() {
        // static utility class only
    }

    public static String getStringSafely(FormItem item) {
        return getStringSafely(item, null);
    }

    public static String getStringSafely(FormItem item, String defaultValue) {
        if (item.getValue() == null) {
            return defaultValue;
        } else {
            return item.getValue().toString();
        }
    }

    /**
     * Adds a help icon to the given form item. When the help icon is clicked,
     * the helpText is displayed to the user.
     * 
     * @param item item that will get a single help icon added to it
     * @param helpText the help text to show the user when the help icon is clicked
     */
    public static void addContextualHelp(FormItem item, final String helpText) {
        addContextualHelp(item, helpText, (FormItemIcon[]) null);
    }

    /**
     * Just like {@link #addContextualHelp(FormItem, String)} except this will also add
     * the given icons to the form item, with the help icon being the last icon.
     * 
     * @param item item that will get the given icons plus a help icon added to it
     * @param helpText the help text to show the user when the help icon is clicked
     * @param icons other icons to add to the form item - these will appear before the help icon
     */
    public static void addContextualHelp(FormItem item, final String helpText, FormItemIcon... icons) {
        final FormItemIcon helpIcon = new FormItemIcon();
        helpIcon.setSrc(ImageManager.getHelpIcon());

        if (icons == null) {
            item.setIcons(helpIcon);
        } else {
            FormItemIcon[] allIcons = new FormItemIcon[icons.length + 1];
            System.arraycopy(icons, 0, allIcons, 0, icons.length);
            allIcons[icons.length] = helpIcon;
            item.setIcons(allIcons);
        }

        item.addIconClickHandler(new IconClickHandler() {
            public void onIconClick(IconClickEvent event) {
                if (event.getIcon().equals(helpIcon)) {
                    SC.say(helpText);
                }
            }
        });
    }

}
