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

import java.util.Properties;

import org.jmock.Expectations;
import org.testng.annotations.Test;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.sync.entity.SystemSettings;
import org.rhq.enterprise.server.sync.validators.SystemSettingsValidator;
import org.rhq.enterprise.server.system.SystemManagerLocal;
import org.rhq.test.JMockTest;

/**
 * 
 *
 * @author Lukas Krejci
 */
@Test
public class SystemSettingsValidatorTest extends JMockTest {

    public void testValidatorCallsSystemManager() {
        final SystemManagerLocal systemManager = context.mock(SystemManagerLocal.class);
        
        SystemSettingsValidator validator = new SystemSettingsValidator(systemManager);
        
        final Properties settings = new Properties();
        settings.put("A", "a");
        settings.put("B", "b");
        SystemSettings importedSettings = new SystemSettings(settings);
        
        context.checking(new Expectations() {
            {
                oneOf(systemManager).validateSystemConfiguration(with(any(Subject.class)), with(settings));
            }
        });
        
        validator.validateExportedEntity(importedSettings);
    }    
}
