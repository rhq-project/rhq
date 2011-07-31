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

import net.sf.sahi.client.Browser;
import net.sf.sahi.config.Configuration;

/**
 *
 */
public class SahiUtility {

    private static final String SAHI_BASE = "/home/ips/Applications/sahi-3.5";
    private static final String SAHI_USER_DATA = SAHI_BASE + "/userdata";

    public static void initSahi() {
        Configuration.initJava(SAHI_BASE, SAHI_USER_DATA);
    }

    public static Browser createBrowser(int profileIndex) {
        System.out.println("Creating browser #" + profileIndex + "...");
        String browserPath = "/usr/bin/firefox";
        String browserProcessName = "firefox";
        String firefoxProfile = "sahi" + profileIndex;

        String browserOptions = "-profile " + SAHI_USER_DATA + "/browser/ff/profiles/" + firefoxProfile
                + " -no-remote";
        System.out.println("Browser options: " + browserOptions);
        Browser browser = new Browser(browserPath, browserProcessName, browserOptions);
        try {
            browser.open();
        } catch (Exception e) {
            throw new RuntimeException("Failed to connect to Sahi proxy - make sure Sahi dashboard is running - cause: "
                    + e);
        }


        return browser;
    }

}
