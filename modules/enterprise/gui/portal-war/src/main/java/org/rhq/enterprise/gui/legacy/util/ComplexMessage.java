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
package org.rhq.enterprise.gui.legacy.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ComplexMessage extends ArrayList {
    public ComplexMessage(CMsgElement[] elts) {
        this(Arrays.asList(elts));
    }

    public ComplexMessage(List elts) {
        addAll(elts);
    }

    public ComplexMessage(CMsgElement elt) {
        add(elt);
    }

    public ComplexMessage(String text) {
        this(text, (String) null);
    }

    public ComplexMessage(String text, String[] params) {
        this(text, null, params);
    }

    public ComplexMessage(String text, String url, String[] params) {
        this(new CMsgElement(text, url, params));
    }

    public ComplexMessage(String text, String url) {
        this(text, url, null);
    }

    public void addElement(CMsgElement elt) {
        add(elt);
    }

    public void addElement(String text) {
        add(new CMsgElement(text));
    }

    public void addElement(String text, String url) {
        add(new CMsgElement(text, url));
    }

    public void addElement(String text, String url, String[] params) {
        add(new CMsgElement(text, url, params));
    }
}