 /*
  * RHQ Management Platform
  * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.core.system.pquery;

/**
 * A <i>conditional</i> is the left side of the equals sign within a {@link Criteria}. It consists of a {@link Category}
 * , an attribute and an {@link Operator}.
 *
 * <p>The default separator between the different tokens of the conditional is a "|". If a "|" appears in any attribute
 * value, or if you wish just to make the query have a different separator, you can redefine the separator by placing it
 * as the first character of the conditional string. Example: <code>.process.name.matches=java.exe</code></p>
 *
 * @author John Mazzitelli
 */
class Conditional {
    char separator = '|';

    enum Category {
        process, arg
    }

    enum Operator {
        match, nomatch,
    }

    enum Qualifier {
        unspecified, parent
    }

    private Category category;
    private Attribute attribute;
    private Operator operator;
    private Qualifier qualifier;

    Conditional(String conditional) {
        char possibleSeparator = conditional.charAt(0);

        if (!Character.isLetter(possibleSeparator)) {
            separator = possibleSeparator;
            conditional = conditional.substring(1);
        }

        String[] tokens = conditional.split("\\" + separator, 4);

        if ((tokens.length < 3) || (tokens.length > 4)) {
            throw new IllegalArgumentException("Conditional needs a category, attribute and operator: " + conditional);
        }

        String categoryString = tokens[0];
        String attributeString = tokens[1];
        String operatorString = tokens[2];
        String qualifierString = (tokens.length == 4) ? tokens[3] : null;

        try {
            this.category = Category.valueOf(categoryString);
        } catch (Exception e) {
        }

        if (this.category == null) {
            throw new IllegalArgumentException("Invalid category: " + conditional);
        }

        this.attribute = new Attribute(attributeString, this.category);

        try {
            this.operator = Operator.valueOf(operatorString);
        } catch (Exception e) {
        }

        if (this.operator == null) {
            throw new IllegalArgumentException("Invalid operator: " + conditional);
        }

        if (qualifierString != null) {
            try {
                this.qualifier = Qualifier.valueOf(qualifierString);
            } catch (Exception e) {
            }

            if (this.qualifier == null) {
                throw new IllegalArgumentException("Invalid qualifier: " + conditional);
            }
        } else {
            this.qualifier = Qualifier.unspecified;
        }

        return;
    }

    Category getCategory() {
        return category;
    }

    Attribute getAttribute() {
        return attribute;
    }

    Operator getOperator() {
        return operator;
    }

    Qualifier getQualifier() {
        return qualifier;
    }

    public String toString() {
        String str = "" + category + separator + attribute + separator + operator;

        if (qualifier != null) {
            str += "" + separator + qualifier;
        }

        return str;
    }
}