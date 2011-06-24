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

import junit.framework.TestResult;
import junit.framework.TestSuite;
import net.sf.sahi.client.Browser;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Ian Springer
 */
public class GuiPerfTestSuite extends TestSuite {

    private static final ThreadLocal<Browser> BROWSERS = new ThreadLocal<Browser>();

    private AtomicInteger threadCount = new AtomicInteger();

    public GuiPerfTestSuite() {
        SahiUtility.initSahi();
    }

    public GuiPerfTestSuite(Class<GuiPerfTestCase> testClass) {
        super(testClass);

        SahiUtility.initSahi();
    }

    @Override
    public void run(TestResult result) {
        Browser browser = BROWSERS.get();
        if (browser == null) {
            // Start at 1, so 0 is reserved for manual recording and testing.
            int browserIndex = this.threadCount.incrementAndGet();
            System.out.println("browserIndex=" + browserIndex);
            browser = SahiUtility.createBrowser(browserIndex);
            BROWSERS.set(browser);

            login(browser);
        }

        super.run(result);

        logout(browser);
        browser.close();
    }

    public static Browser getBrowser() {
        return BROWSERS.get();
    }

    private void login(Browser browser) {
        browser.navigateTo("http://localhost:7080/");
        browser.textbox("user").setValue("rhqadmin");
        browser.password("password").setValue("rhqadmin");
        browser.cell("Login").click();
    }

    private void logout(Browser browser) {
        browser.link("Logout").click();
    }

}
