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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.jmock.Expectations;
import org.testng.annotations.Test;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.common.composite.SystemSetting;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.sync.entity.SystemSettings;
import org.rhq.core.util.StringUtil;
import org.rhq.enterprise.server.sync.importers.SystemSettingsImporter;
import org.rhq.enterprise.server.system.SystemManagerLocal;
import org.rhq.test.JMockTest;

/**
 * @author Lukas Krejci
 */
@Test
public class SystemSettingsImporterTest extends JMockTest {

    public void testNullConfigurationUnderstoodAsDefault() throws Exception {
        final SystemManagerLocal systemManager = context.mock(SystemManagerLocal.class);

        SystemSettingsImporter importer = new SystemSettingsImporter(null, systemManager);

        final HashMap<String, String> settings = new HashMap<String, String>();
        settings.put(SystemSetting.BASE_URL.getInternalName(), "url");
        addImportableProps(settings);
        final SystemSettings importedSettings = new SystemSettings(settings);

        context.checking(new Expectations() {
            {
                org.rhq.core.domain.common.composite.SystemSettings expectedImport = org.rhq.core.domain.common
                    .composite.SystemSettings.fromMap(settings);

                //base url is not imported by default
                expectedImport.remove(SystemSetting.BASE_URL);

                oneOf(systemManager).deobfuscate(with(any(org.rhq.core.domain.common.composite.SystemSettings.class)));
                oneOf(systemManager).setSystemSettings(with(any(Subject.class)), with(expectedImport));
            }
        });

        importer.configure(null);

        importer.update(null, importedSettings);

        importer.finishImport();
    }

    public void testImportConfigurationHonored() throws Exception {
        final SystemManagerLocal systemManager = context.mock(SystemManagerLocal.class);

        SystemSettingsImporter importer = new SystemSettingsImporter(null, systemManager);

        String[] importableProps = SystemSettingsImporter.DEFAULT_IMPORTED_PROPERTIES_LIST.split("\\s*,\\s*");

        final HashMap<String, String> settings = new HashMap<String, String>();
        addImportableProps(settings);
        final SystemSettings importedSettings = new SystemSettings(settings);

        final List<String> allowedSettings = new ArrayList<String>();
        for (int i = 0; i < importableProps.length; ++i) {
            if (i % 2 == 0) {
                allowedSettings.add(importableProps[i]);
            }
        }

        context.checking(new Expectations() {
            {
                org.rhq.core.domain.common.composite.SystemSettings expectedImport = new org.rhq.core.domain.common.composite.SystemSettings();
                for (String s : allowedSettings) {
                    expectedImport.put(SystemSetting.getByInternalName(s), settings.get(s));
                }

                oneOf(systemManager).deobfuscate(with(any(org.rhq.core.domain.common.composite.SystemSettings.class)));
                oneOf(systemManager).setSystemSettings(with(any(Subject.class)), with(expectedImport));
            }
        });

        Configuration importConfig = new Configuration();
        importConfig.put(new PropertySimple(SystemSettingsImporter.PROPERTIES_TO_IMPORT_PROPERTY, StringUtil
            .collectionToString(allowedSettings, ", ")));

        importer.configure(importConfig);
        
        importer.update(null, importedSettings);
        
        importer.finishImport();
    }

    private void addImportableProps(Map<String, String> settings) {
        String[] importableProps = SystemSettingsImporter.DEFAULT_IMPORTED_PROPERTIES_LIST.split("\\s*,\\s*");
        for (String p : importableProps) {
            String value;
            switch(SystemSetting.getByInternalName(p).getType()) {
            case BOOLEAN: value = "true"; break;
            case INTEGER: case LONG: value = "1"; break;
            case PASSWORD: value = "password"; break;
            default:
                value = "value";
            }
            settings.put(p, value);
        }
    }
}
