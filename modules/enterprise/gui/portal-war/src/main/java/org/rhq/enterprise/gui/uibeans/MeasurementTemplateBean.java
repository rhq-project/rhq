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
package org.rhq.enterprise.gui.uibeans;

public class MeasurementTemplateBean {
    private String name = null;
    private String alias = null;
    private String type = null;
    private String category = null;
    private String expression = null;

    public void setName(String s) {
        name = s;
    }

    public String getName() {
        return name;
    }

    public void setAlias(String s) {
        alias = s;
    }

    public String getAlias() {
        return alias;
    }

    public void setType(String s) {
        type = s;
    }

    public String getType() {
        return type;
    }

    public void setCategory(String s) {
        category = s;
    }

    public String getCategory() {
        return category;
    }

    public void setExpression(String s) {
        expression = s;
    }

    public String getExpression() {
        return expression;
    }

    public String toString() {
        return "name=" + name + " alias=" + alias + " type=" + type + " category=" + category + " expression="
            + expression;
    }
}