/*
 * RHQ Management Platform
 * Copyright (C) 2011 Red Hat, Inc.
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
package org.rhq.guiperftest;

import junit.framework.Test;
import junit.framework.TestCase;
import net.sf.sahi.client.Browser;

import java.util.UUID;

/**
 * Performance tests for RHQ GUI.
 *
 * @author Ian Springer
 */
public class GuiPerfTestCase extends TestCase {

    public GuiPerfTestCase(String name) {
        super(name);
    }

    public void testLoadResourceTree() {
        Browser browser = GuiPerfTestSuite.getBrowser();

        browser.link("Inventory").click();
        browser.cell("Platforms").click();
        browser.div("Linux Operating System").under(browser.cell("Description")).doubleClick();
        // check that tree has been drawn
        assertTrue(browser.table("treeCellSelected").isVisible());
    }

/*
    public void testCreateCompatibleGroup() throws InterruptedException {
        Browser browser = GuiPerfTestSuite.getBrowser();

        browser.link("Inventory").click();
        browser.cell("Compatible Groups").click();
        browser.cell("New").click();

        String groupName = "group" + UUID.randomUUID();
        browser.textbox("name").setValue(groupName);
        browser.textarea("description").setValue("bleh");
        browser.cell("Next").click();

        //browser.textbox("search").setValue("RHQ Agent");
        //browser.textbox("search").click();

        browser.cell("Choose a value").click();
        browser.xy(browser.cell("RHQAgent Plugin"), 3, 3).hover();
        browser.xy(browser.cell("treeMenuSelected[2]", 3, 3)).click();
        //browser.xy(browser.cell("RHQ Agent"), 3, 3).near(browser.cell("RHQAgent Plugin")).click();

        browser.waitFor(1000);
        browser.div("RHQ Agent[1]").hover();
        browser.waitFor(1000);
        browser.div("RHQ Agent[1]").click();
        browser.image("right_Over.png").click();
        browser.cell("Finish").click();
    }
*/

/*
    public void testCreateRole() throws InterruptedException {
        Browser browser = GuiPerfTestSuite.getBrowser();

        browser.link("Administration").click();
        browser.cell("Roles").click();
        browser.cell("New").click();

        String roleName = "role" + UUID.randomUUID();
        browser.textbox("name").setValue(roleName);
        browser.textbox("description").setValue("bleh");
        browser.image(0).near(browser.div("Manage Security")).click();
        browser.image("unchecked.png").near(browser.div("Manage Security")).click();
        browser.cell("Save").click();
    }
*/

    public static Test suite() {
        return new GuiPerfTestSuite(GuiPerfTestCase.class);
        /*long maxElapsedTime = 10000;

        TestSuite undecoratedSuite = new TestSuite(GuiPerfTestCase.class);
        Enumeration undecoratedTests = undecoratedSuite.tests();
        GuiPerfTestSuite timedSuite = new GuiPerfTestSuite();
        while (undecoratedTests.hasMoreElements()) {
            Test undecoratedTest = (Test) undecoratedTests.nextElement();
            System.out.println("test: " + undecoratedTest);
            TimedTest timedTest = new TimedTest(undecoratedTest, maxElapsedTime);
            timedTest.setQuiet();
            timedSuite.addTest(timedTest);
        }

        // Run the whole suite 10x concurrently.
        LoadTest loadTest = new LoadTest(timedSuite, 10);
        loadTest.setEnforceTestAtomicity(true);

        return loadTest;*/
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

}
