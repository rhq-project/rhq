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

package org.rhq.core.domain.configuration;

import static org.testng.Assert.*;

import org.testng.annotations.Test;

public class PropertySimpleTest {

    @Test
    public void deepCopyShouldCopyAllSimpleFields() {
        PropertySimple original = new PropertySimple("simpleProperty", "Simple Property");
        original.setId(1);
        original.setErrorMessage("error message");
        original.setUnmaskedStringValue("Unmasked Simple Property");

        PropertySimple copy = original.deepCopy();

        assertEquals(copy.getId(), original.getId(), "Failed to copy the id property");
        assertEquals(copy.getName(), original.getName(), "Failed to copy the name property");
        assertEquals(copy.getErrorMessage(), original.getErrorMessage(), "Failed to copy the errorMessage property");
        assertEquals(copy.getOverride(), original.getOverride(), "Failed to copy the override property");
        assertEquals(copy.getUnmaskedStringValue(), original.getUnmaskedStringValue(), "Failed to copy the unmaskedStringValue property");
        assertEquals(copy.getStringValue(), original.getStringValue(), "Failed to copy the stringValue property");
    }

}
