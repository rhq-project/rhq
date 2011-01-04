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
package org.rhq.enterprise.server.search.translation.antlr;

/**
 * @author Joseph Marques
 */
public class RHQLAdvancedTerm implements RHQLTerm {
    private final String lineage;
    private final String path;
    private final String param;

    private final RHQLComparisonOperator operator;

    private final String value;

    public RHQLAdvancedTerm(String lineage, String path, String param, RHQLComparisonOperator operator, String value) {
        this.lineage = lineage;
        this.path = path;
        this.param = param;
        this.operator = operator;
        this.value = value;
    }

    public String getLineage() {
        return lineage;
    }

    public String getPath() {
        return path;
    }

    public String getParam() {
        return param;
    }

    public RHQLComparisonOperator getOperator() {
        return operator;
    }

    public String getValue() {
        return value;
    }
}