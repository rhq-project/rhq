/*
 * RHQ Management Platform
 * Copyright (C) 2012 Red Hat, Inc.
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
package org.rhq.core.pluginapi.configuration;

import java.util.ArrayList;
import java.util.List;

import org.rhq.core.domain.configuration.PropertySimple;

/**
 * @author Ian Springer
 */
public class ListPropertySimpleWrapper {

    protected PropertySimple prop;

    public ListPropertySimpleWrapper(PropertySimple prop) {
        if (prop == null) {
            throw new IllegalArgumentException("'prop' parameter must not be null.");
        }
        this.prop = prop;
    }

    public void setValue(List list) {
        String stringValue;
        if (list != null) {
            StringBuilder buffer = new StringBuilder();
            for (Object element : list) {
                buffer.append(element).append('\n');
            }
            stringValue = buffer.toString();
        } else {
            stringValue = null;
        }
        this.prop.setStringValue(stringValue);
    }

    public List<String> getValue() {
        List<String> list = new ArrayList<String>();

        String stringValue = this.prop.getStringValue();
        if (stringValue != null) {
            String[] lines = stringValue.split("\n+");
            for (String line : lines) {
                String element = line.trim();
                list.add(element);
            }
        }

        return list;
    }

}
