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

import java.util.Comparator;

/**
 * The specification for what makes up an OSGi compliant version string and how they can be compared was taken from
 * here:
 * http://help.eclipse.org/help31/index.jsp?topic=/org.eclipse.platform.doc.isv/reference/osgi/org/osgi/framework/Version.html
 *
 * @author  <a href="ccrouch@jboss.com">Charles Crouch</a>
 *
 */
public class OSGiVersionComparator implements Comparator {
    public OSGiVersionComparator() {
    }

    public int compare(Object o1, Object o2) {
        if ((o1 instanceof String) && (o2 instanceof String)) {
            Version ver1 = new Version((String) o1);
            Version ver2 = new Version((String) o2);

            int result = ver1.major - ver2.major;
            if (result == 0) {
                result = ver1.minor - ver2.minor;
                if (result == 0) {
                    result = ver1.micro - ver2.micro;
                    if (result == 0) {
                        result = ver1.qualifier.compareTo(ver2.qualifier);
                    }
                }
            }

            return result;
        } else {
            throw new ClassCastException("Arguments must both be Strings.");
        }
    }

    private class Version {
        public int major;
        public int minor;
        public int micro;
        public String qualifier = "";

        public Version(String version) {
            String[] parts = version.split("\\.");

            try {
                switch (parts.length) {
                case 4: {
                    qualifier = parts[3];
                }

                case 3: {
                    micro = Integer.parseInt(parts[2]);
                }

                case 2: {
                    minor = Integer.parseInt(parts[1]);
                }

                case 1: {
                    major = Integer.parseInt(parts[0]);
                    break;
                }

                default: {
                    throw new IllegalArgumentException("Malformed version string [" + version + "]");
                }
                }
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Malformed version string [" + version + "]");
            }
        }
    }
}