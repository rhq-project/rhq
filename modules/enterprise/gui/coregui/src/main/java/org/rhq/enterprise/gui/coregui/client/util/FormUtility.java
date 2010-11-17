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

import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.FormItem;

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

}
