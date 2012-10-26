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
import java.util.Properties;

import org.jmock.Expectations;
import org.testng.annotations.Test;

import org.rhq.core.domain.auth.Subject;
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
        settings.put("CAM_BASE_URL", "url");
        settings.put("SOME_UNKNOWN_PROPERTY", "asdf");
        String[] importableProps = SystemSettingsImporter.DEFAULT_IMPORTED_PROPERTIES_LIST.split("\\s*,\\s*");
        for (String p : importableProps) {
            settings.put(p, "value");
        }
        final SystemSettings importedSettings = new SystemSettings(settings);

        context.checking(new Expectations() {
            {
                Properties expectedImport = new Properties();
                expectedImport.putAll(settings);
                //base url is not imported by default
                expectedImport.remove("CAM_BASE_URL");
                expectedImport.remove("SOME_UNKNOWN_PROPERTY");

                oneOf(systemManager).setSystemConfiguration(with(any(Subject.class)), with(expectedImport), with(true));
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
        for (String p : importableProps) {
            settings.put(p, "value");
        }
        final SystemSettings importedSettings = new SystemSettings(settings);

        final List<String> allowedSettings = new ArrayList<String>();
        for (int i = 0; i < importableProps.length; ++i) {
            if (i % 2 == 0) {
                allowedSettings.add(importableProps[i]);
            }
        }

        context.checking(new Expectations() {
            {
                Properties expectedImport = new Properties();
                for (String s : allowedSettings) {
                    expectedImport.put(s, settings.get(s));
                }

                oneOf(systemManager).setSystemConfiguration(with(any(Subject.class)), with(expectedImport), with(true));
            }
        });

        Configuration importConfig = new Configuration();
        importConfig.put(new PropertySimple(SystemSettingsImporter.PROPERTIES_TO_IMPORT_PROPERTY, StringUtil
            .collectionToString(allowedSettings, ", ")));

        importer.configure(importConfig);
        
        importer.update(null, importedSettings);
        
        importer.finishImport();
    }
}
