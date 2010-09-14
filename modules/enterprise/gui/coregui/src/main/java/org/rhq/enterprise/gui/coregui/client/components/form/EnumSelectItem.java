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
import java.util.Set;

import com.google.gwt.core.client.JavaScriptObject;
import com.smartgwt.client.types.MultipleAppearance;
import com.smartgwt.client.widgets.form.fields.SelectItem;

/**
 * A convenients class for exposing an {@link Enum} as a multi-select form item. 
 *
 * @author Joseph Marques 
 */
public class EnumSelectItem extends SelectItem {

    public EnumSelectItem(Class<? extends Enum<?>> e) {
        super();
        init(e);
    }

    public EnumSelectItem(JavaScriptObject jsObj, Class<? extends Enum<?>> e) {
        super(jsObj);
        init(e);
    }

    public EnumSelectItem(String name, Class<? extends Enum<?>> e) {
        super(name);
        init(e);
    }

    public EnumSelectItem(String name, String title, Class<? extends Enum<?>> e) {
        super(name, title);
        init(e);
    }

    public void init(Class<? extends Enum<?>> e) {
        setMultiple(true);
        setMultipleAppearance(MultipleAppearance.PICKLIST);

        LinkedHashMap<String, String> valueMap = getEnumValueMap(e);
        Set<String> keys = valueMap.keySet();

        setValueMap(valueMap);
        setValues(keys.toArray(new String[keys.size()])); // select them all by default
    }

    @SuppressWarnings("unchecked")
    private LinkedHashMap<String, String> getEnumValueMap(Class<? extends Enum> e) {
        LinkedHashMap<String, String> map = new LinkedHashMap<String, String>();
        for (Object o : EnumSet.allOf(e)) {
            Enum v = (Enum) o;
            map.put(v.name(), v.toString());
        }
        return map;
    }

}
