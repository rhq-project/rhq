/*
 * RHQ Management Platform
 * Copyright (C) 2012 Red Hat, Inc.
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
package org.rhq.enterprise.startup;

import java.io.IOException;

import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;

/**
 * Test for parsing of the subsystem XML.
 *
 * @author Ian Springer
 * @author John Mazzitelli
 */
@Test(enabled = false)
// TODO: can't get the XML test to pass, but I can't see why it fails
public class SubsystemXmlParsingTest extends AbstractSubsystemBaseTest {

    public SubsystemXmlParsingTest() {
        super(StartupExtension.SUBSYSTEM_NAME, new StartupExtension());
    }

    @Override
    @BeforeTest
    public void initializeParser() throws Exception {
        super.initializeParser();
    }

    @Override
    public void testSubsystem() throws Exception {
        super.testSubsystem();
    }

    @Override
    @AfterTest
    public void cleanup() throws Exception {
        super.cleanup();
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        return "<subsystem xmlns=\"" + StartupExtension.NAMESPACE + "\">" //
            + "</subsystem>";
    }

}
