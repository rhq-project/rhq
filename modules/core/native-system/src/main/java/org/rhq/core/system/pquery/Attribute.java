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
package org.rhq.core.system.pquery;

import java.util.Arrays;
import org.rhq.core.system.pquery.Conditional.Category;

/**
 * The attribute portion of a {@link Conditional}.
 *
 * @author John Mazzitelli
 */
class Attribute {
    enum ProcessCategoryAttributes {
        name, basename, pidfile, pid
    }

    private String attributeValue;

    Attribute(String attributeValue, Category category) {
        validate(attributeValue, category);

        this.attributeValue = attributeValue;
    }

    String getAttributeValue() {
        return attributeValue;
    }

    /**
     * Returns the attribute's value as a <code>Integer</code>, if it really represents a number. If the
     * {@link #getAttributeValue() attribute's string value} is not a number, <code>null</code> is returned.
     *
     * @return the attribute string as a <code>Integer</code> or <code>null</code> if the attribute is not parsable as a
     *         number
     */
    Integer getAttributeValueAsInteger() {
        try {
            return Integer.valueOf(this.attributeValue);
        } catch (Exception ignore) {
            return null;
        }
    }

    public String toString() {
        return attributeValue;
    }

    /**
     * Ensures that the given attribute is valid for the given category. This method returns if its valid; an exception
     * is thrown if its invalid.
     *
     * @param  attribute
     * @param  category  the category that the attribute is to be validated for use with
     *
     * @throws IllegalArgumentException if it is invalid
     */
    private void validate(String attribute, Category category) {
        if (category.equals(Category.process)) {
            try {
                ProcessCategoryAttributes.valueOf(attribute);
                return;
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid attribute [" + attribute + "] for category [" + category
                    + "]. Must be one of: " + Arrays.toString(ProcessCategoryAttributes.values()));
            }
        } else if (category.equals(Category.arg)) {
            return; // arg attributes can be any number, any string or * - can't really validate that
        }

        throw new IllegalArgumentException("Attribute cannot be validated due to unknown category: " + category);
    }
}