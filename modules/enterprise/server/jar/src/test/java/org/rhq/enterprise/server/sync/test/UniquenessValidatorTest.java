/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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

package org.rhq.enterprise.server.sync.test;

import static org.testng.Assert.fail;

import org.testng.annotations.Test;

import org.rhq.enterprise.server.sync.ValidationException;
import org.rhq.enterprise.server.sync.validators.UniquenessValidator;

/**
 * 
 *
 * @author Lukas Krejci
 */
@Test
public class UniquenessValidatorTest {

    public void testDetectsDuplicitEntries() throws Exception {
        UniquenessValidator<String> validator = new UniquenessValidator<String>();
        
        validator.initialize(null, null);
        
        validator.validateExportedEntity("1");
        validator.validateExportedEntity("2");
        try {
            validator.validateExportedEntity("1");
            fail("Uniqueness validator should have detected a duplicate entry.");
        } catch (ValidationException e) {
            //expected
        }
    }
}
