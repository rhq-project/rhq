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
package org.apache.maven.artifact.versioning;

import java.util.ArrayList;
import java.util.List;

import org.testng.annotations.Test;

@Test
public class ComparableVersionTest {

    private static List<String> RHQ_VERSIONS = new ArrayList<String>();
    static {
        RHQ_VERSIONS.add("4.0.0.GA");
        RHQ_VERSIONS.add("4.1.0.GA");
        RHQ_VERSIONS.add("4.2.0.GA");
        RHQ_VERSIONS.add("4.2.1.GA");
        RHQ_VERSIONS.add("4.3.0.GA");
        RHQ_VERSIONS.add("4.3.1.RC1");
        RHQ_VERSIONS.add("4.3.1.RC2");
        RHQ_VERSIONS.add("4.3.1.GA");
        RHQ_VERSIONS.add("4.4.0.RC1");
        RHQ_VERSIONS.add("4.4.0.RC2");
        RHQ_VERSIONS.add("4.4.0.GA");
    }

    private static List<String> JON_VERSIONS = new ArrayList<String>();
    static {
        JON_VERSIONS.add("2.4.0.GA");
        JON_VERSIONS.add("2.4.1.GA");
        JON_VERSIONS.add("4.2.0.JON300.GA");
        JON_VERSIONS.add("4.2.0.JON.3.0.1.GA");
        JON_VERSIONS.add("4.2.0.JON302GA");
        JON_VERSIONS.add("4.4.0.JON310BETA1");
        JON_VERSIONS.add("4.4.0.JON310CR1");
        JON_VERSIONS.add("4.4.0.JON310GA");
        JON_VERSIONS.add("4.4.0.JON311GA");
        JON_VERSIONS.add("4.4.0.JON312CR1");
        JON_VERSIONS.add("4.4.0.JON312GA");
    }

    public void testRHQVersions() {
        testListOfVersionsStepByStep(RHQ_VERSIONS);
        testListOfVersionsSkippingVersions(RHQ_VERSIONS);
    }

    public void testJONVersions() {
        testListOfVersionsStepByStep(JON_VERSIONS);
        testListOfVersionsSkippingVersions(JON_VERSIONS);
    }

    private void testListOfVersionsStepByStep(List<String> versions) {
        String oldVer = null;
        String newVer = null;
        for (String ver : versions) {
            if (newVer != null) {
                oldVer = newVer;
                newVer = ver;
                compareVersions(oldVer, newVer);
            } else {
                newVer = ver;
            }
        }
    }

    private void testListOfVersionsSkippingVersions(List<String> versions) {
        // Start with the oldest, and test all versions after it making sure they are all newer.
        // After testing the oldest, go to the next oldest and do it again.
        // This tests a person skipping one or more versions when upgrading (say, upgrading from 2.0 to 4.0, skipping 3.0)
        if (versions.size() <= 1) {
            return;
        }

        String oldest = versions.get(0);
        for (int i = 1; i < versions.size(); i++) {
            compareVersions(oldest, versions.get(i));
        }

        testListOfVersionsSkippingVersions(versions.subList(1, versions.size()));
    }

    public void testBZ_813967() {
        compareVersions("4.2.0.JON300.GA", "4.2.0.JON.3.0.1.GA");
    }

    public void testSimpleVersionCompare() {
        compareVersions("1.0", "1.1");
        compareVersions("1.0", "2.0");
        compareVersions("1.0.0", "1.0.1");
    }

    public void testSnapshotVersionCompare() {
        compareVersions("1.0-SNAPSHOT", "1.1-SNAPSHOT");
        compareVersions("1.1-SNAPSHOT", "1.1.0.RC1");
        compareVersions("1.1-SNAPSHOT", "1.1.0.GA");
        compareVersions("1.1-SNAPSHOT", "1.1.0.GA");
        compareVersions("1.1.1-SNAPSHOT", "1.1.1.GA");
        compareVersions("1.1.0.GA", "1.1.1-SNAPSHOT"); // ver2 version number is still after ver1 version number, although ver2 is not production ready
    }

    public void testRCVersionCompare() {
        compareVersions("1.0.0.RC1", "1.1.0.RC1");
        compareVersions("1.1.0.RC1", "1.1.0.RC2");
        compareVersions("1.1.0.RC1", "1.1.0.RC10");
        compareVersions("1.1.0.RC1", "1.1.0.GA");
        compareVersions("1.1.0.GA", "1.1.1.RC1"); // ver2 version number is still after ver1 version number, although ver2 is not production ready
        compareVersions("4.0.0-SNAPSHOT", "4.0.0.CR2"); // CR is an alias for RC
        compareVersions("4.0.0-SNAPSHOT", "4.0.0.CR"); // CR is an alias for RC
    }

    public void testSnapshotBetaGACompare() {
        compareVersions("4.0.0-SNAPSHOT", "4.0.0.Beta1");
        compareVersions("4.0.0.Beta1", "4.0.0");
        compareVersions("4.0.0.Beta1", "4.0.0.GA");
    }

    public void testBZ916790() {
        new ComparableVersion("3.13.1.201302221022"); // a bug caused this to throw an exception before
        compareVersions("3.13.1.201302221022", "3.13.1.201302221023");
        compareVersions("3.13.1.201202221022", "3.13.1.201302221022");
    }

    private void compareVersions(String ver1String, String ver2String) {
        //System.out.println("Testing: " + ver1String + "\t" + ver2String);

        ComparableVersion ver1 = new ComparableVersion(ver1String);
        ComparableVersion ver2 = new ComparableVersion(ver2String);

        assert !ver1.equals(ver2) : "Should not be equal 1=[" + ver1String + "]; 2=[" + ver2String + "]";
        assert !ver2.equals(ver1) : "Should not be equal 1=[" + ver1String + "]; 2=[" + ver2String + "]";
        assert ver1.compareTo(ver1) == 0 : "Identity compare failed 1=[" + ver1String + "]; 2=[" + ver2String + "]";
        assert ver2.compareTo(ver2) == 0 : "Identity compare failed 1=[" + ver1String + "]; 2=[" + ver2String + "]";
        assert ver1.compareTo(ver2) < 0 : "1 should be before 2: 1=[" + ver1String + "]; 2=[" + ver2String + "]";
        assert ver2.compareTo(ver1) > 0 : "2 should be after 1: 1=[" + ver1String + "]; 2=[" + ver2String + "]";
    }
}
