/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.client.util;

import com.smartgwt.client.data.fields.DataSourceTextField;

/**
 * @author Joseph Marques
 */
public class HighlightingDatasourceTextField extends DataSourceTextField {

    public HighlightingDatasourceTextField(String name, String title, int length, boolean required) {
        super(name, title, length, required);
    }

    public HighlightingDatasourceTextField(String name, String title, int length) {
        super(name, title, length);
    }

    public HighlightingDatasourceTextField(String name, String title) {
        super(name, title);
    }

    public HighlightingDatasourceTextField(String name) {
        super(name);
    }
    
}
