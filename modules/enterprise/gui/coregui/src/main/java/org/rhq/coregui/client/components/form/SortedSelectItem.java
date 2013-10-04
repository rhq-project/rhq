/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
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

package org.rhq.coregui.client.components.form;

import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.SortedSet;
import java.util.TreeSet;

import com.google.gwt.core.client.JavaScriptObject;
import com.smartgwt.client.widgets.form.fields.SelectItem;

/**
 * A convenience class for a sorted variant of {@link SelectItem}.
 *  
 * @author Jirka Kremser
 */
public class SortedSelectItem extends SelectItem {
    public SortedSelectItem() {
        super();
    }

    public SortedSelectItem(JavaScriptObject jsObj) {
        super(jsObj);
    }

    public SortedSelectItem(String name) {
        super(name);
    }

    public SortedSelectItem(String name, String title) {
        super(name, title);
    }

    @Override
    public void setValueMap(@SuppressWarnings("rawtypes")
    LinkedHashMap valueMap) {
        if (valueMap == null) {
            throw new IllegalArgumentException("valueMap cannot be null");
        }
        @SuppressWarnings("unchecked")
        LinkedHashMap<String, String> safeValueMap = (LinkedHashMap<String, String>) valueMap;
        LinkedHashMap<String, String> sortedValueMap = new LinkedHashMap<String, String>(valueMap.size());
        SortedSet<Entry<String, String>> sortedEntries = new TreeSet<Entry<String, String>>(
            new Comparator<Entry<String, String>>() {

                @Override
                public int compare(Entry<String, String> o1, Entry<String, String> o2) {
                    return o1.getValue().compareTo(o2.getValue());
                }
            });
        sortedEntries.addAll(safeValueMap.entrySet());
        for (Entry<String, String> entry : sortedEntries) {
            sortedValueMap.put(entry.getKey(), entry.getValue());
        }

        super.setValueMap(sortedValueMap);
    }

    @Override
    public void setValueMap(String... valueMap) {
        if (valueMap == null) {
            throw new IllegalArgumentException("valueMap cannot be null");
        }
        String[] sortedValueArray = new String[valueMap.length];
        System.arraycopy(valueMap, 0, sortedValueArray, 0, valueMap.length);
        Arrays.sort(sortedValueArray);

        super.setValueMap(sortedValueArray);
    }

    @Override
    public void setValues(String... valueMap) {
        setValueMap(valueMap);
    }

}
