/*
 * RHQ Management Platform
 * Copyright (C) 2011 Red Hat, Inc.
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

import com.smartgwt.client.data.Record;

/**
 * A collection of utility methods for working with SmartGWT {@link Record}s.
 *
 * @author Ian Springer
 */
public class RecordUtility {

    private RecordUtility() {
        // static utility class only
    }

    public static Integer getAttributeAsInteger(Record record, String attributeName) {
        Object value;
        try {
            value = record.getAttributeAsInt(attributeName);
        } catch (Exception e) {
            try {
                value = record.getAttributeAsDouble(attributeName);
            } catch (Exception e1) {
                try {
                    value = record.getAttributeAsString(attributeName);
                } catch (Exception e2) {
                    throw new RuntimeException("Failed to obtain value of attribute [" + attributeName + "] of Record ["
                            + record + "].");
                }
            }
        }

        Integer integerValue;
        if (value instanceof Number) {
            integerValue = ((Number) value).intValue();
        } else if (value instanceof String) {
            integerValue = Integer.parseInt((String) value);
        } else if (value == null) {
            integerValue = null;
        } else {
            throw new RuntimeException("Value of attribute [" + attributeName + "] of Record [" + record
                    + "] is not an integer.");
        }

        return integerValue;
    }

}
