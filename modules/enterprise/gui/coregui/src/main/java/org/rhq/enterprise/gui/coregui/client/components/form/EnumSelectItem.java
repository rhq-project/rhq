/*
 * RHQ Management Platform
 * Copyright (C) 2010 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.client.components.form;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import com.smartgwt.client.types.MultipleAppearance;
import com.smartgwt.client.widgets.form.fields.SelectItem;

/**
 * A convenience class for exposing an {@link Enum} as a multi-select form item.
 *
 * @author Joseph Marques 
 */
public class EnumSelectItem extends SelectItem {

    /**
     * @param name name of the SelectItem
     * @param title display name of the SelectItem (should be I18N)
     * @param e the Enum class object
     * @param valueMap EnumName->DisplayName map. Enums without a mapping with use .toString() for the display. For I18N
     * this should be supplied. Can be null.
     * @param valueIcons EnumName->Icon map. Enums without a mapping have no icon. Can be null.
     */
    public EnumSelectItem(String name, String title, Class<? extends Enum<?>> e,
        LinkedHashMap<String, String> valueMap, Map<String, String> valueIcons) {
        super(name, title);
        init(e, valueMap, valueIcons);
    }

    public void init(Class<? extends Enum<?>> e, LinkedHashMap<String, String> valueMap, Map<String, String> valueIcons) {
        setMultiple(true);
        setMultipleAppearance(MultipleAppearance.PICKLIST);

        valueMap = getEnumValueMap(e, valueMap);
        Set<String> keys = valueMap.keySet();

        setValueMap(valueMap);
        setValues(keys.toArray(new String[keys.size()])); // select them all by default

        if (null != valueIcons) {
            setValueIcons(valueIcons);
        }
    }

    @SuppressWarnings("unchecked")
    private LinkedHashMap<String, String> getEnumValueMap(Class<? extends Enum> e,
        LinkedHashMap<String, String> valueMap) {
        LinkedHashMap<String, String> map = (null != valueMap) ? valueMap : new LinkedHashMap<String, String>();
        for (Object o : EnumSet.allOf(e)) {
            Enum v = (Enum) o;
            String name = v.name();
            if (!valueMap.containsKey(name)) {
                map.put(name, v.toString());
            }
        }
        return map;
    }

}
