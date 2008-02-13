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
package org.rhq.core.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Properties;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@Test
public class PropertiesFileUpdateTest {
    private File existingPropertiesFile;

    @BeforeMethod
    public void beforeMethod() throws IOException {
        existingPropertiesFile = File.createTempFile("properties-file-update-test", ".properties");
        PrintStream ps = new PrintStream(new FileOutputStream(existingPropertiesFile), true, "8859_1");
        ps.println("# first comment");
        ps.println("one=1");
        ps.println();
        ps.println("# second comment");
        ps.println("two=12");
        ps.println();
        ps.println("# third comment");
        ps.println("three=123");
        ps.flush();
        ps.close();
    }

    @AfterMethod
    public void afterMethod() {
        if (existingPropertiesFile != null) {
            existingPropertiesFile.delete();
        }
    }

    public void testEmptyValue() throws Exception {
        Properties props = loadPropertiesFile();

        // sanity check - validate our original test properties file is as we expect
        assert props.getProperty("one").equals("1");
        assert props.getProperty("two").equals("12");
        assert props.getProperty("three").equals("123");
        assert props.size() == 3;

        PropertiesFileUpdate update = new PropertiesFileUpdate(existingPropertiesFile.getAbsolutePath());

        // we want to change some of the values, but leave others alone
        Properties newProps = new Properties();
        newProps.setProperty("two", "");
        newProps.setProperty("four", "");

        update.update(newProps);
        props = loadPropertiesFile();
        assert props.getProperty("one").equals("1");
        assert props.getProperty("two").equals("");
        assert props.getProperty("three").equals("123");
        assert props.getProperty("four").equals("");
        assert props.size() == 4;

        update.update("one", null); // null is same as ""
        update.update("five", null); // null is same as ""
        props = loadPropertiesFile();
        assert props.getProperty("one").equals("");
        assert props.getProperty("two").equals("");
        assert props.getProperty("three").equals("123");
        assert props.getProperty("four").equals("");
        assert props.getProperty("five").equals("");
        assert props.size() == 5;
    }

    public void testBulkUpdate() throws Exception {
        Properties props = loadPropertiesFile();

        // sanity check - validate our original test properties file is as we expect
        assert props.getProperty("one").equals("1");
        assert props.getProperty("two").equals("12");
        assert props.getProperty("three").equals("123");
        assert props.size() == 3;

        PropertiesFileUpdate update = new PropertiesFileUpdate(existingPropertiesFile.getAbsolutePath());

        // we want to change some of the values, but leave others alone
        Properties newProps = new Properties();
        newProps.setProperty("two", "new2");
        newProps.setProperty("three", "123"); // same as the old value - should be ignored
        newProps.setProperty("four", "44444");

        update.update(newProps);
        props = loadPropertiesFile();
        assert props.getProperty("one").equals("1");
        assert props.getProperty("two").equals("new2");
        assert props.getProperty("three").equals("123");
        assert props.getProperty("four").equals("44444");
        assert props.size() == 4;
    }

    public void testUpdateKeyValue() throws Exception {
        Properties props = loadPropertiesFile();

        // sanity check - validate our original test properties file is as we expect
        assert props.getProperty("one").equals("1");
        assert props.getProperty("two").equals("12");
        assert props.getProperty("three").equals("123");
        assert props.size() == 3;

        PropertiesFileUpdate update = new PropertiesFileUpdate(existingPropertiesFile.getAbsolutePath());

        update.update("two", "22222");
        props = loadPropertiesFile();
        assert props.getProperty("one").equals("1");
        assert props.getProperty("two").equals("22222");
        assert props.getProperty("three").equals("123");
        assert props.size() == 3;

        update.update("one", "11111");
        props = loadPropertiesFile();
        assert props.getProperty("one").equals("11111");
        assert props.getProperty("two").equals("22222");
        assert props.getProperty("three").equals("123");
        assert props.size() == 3;

        update.update("three", "33333");
        props = loadPropertiesFile();
        assert props.getProperty("one").equals("11111");
        assert props.getProperty("two").equals("22222");
        assert props.getProperty("three").equals("33333");
        assert props.size() == 3;

        update.update("four", "1234");
        props = loadPropertiesFile();
        assert props.getProperty("one").equals("11111");
        assert props.getProperty("two").equals("22222");
        assert props.getProperty("three").equals("33333");
        assert props.getProperty("four").equals("1234");
        assert props.size() == 4;
    }

    private Properties loadPropertiesFile() throws IOException {
        Properties props = new Properties();
        FileInputStream is = new FileInputStream(existingPropertiesFile);
        props.load(is);
        is.close();
        return props;
    }
}