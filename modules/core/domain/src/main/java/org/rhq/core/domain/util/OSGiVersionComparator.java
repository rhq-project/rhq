 /*
  * RHQ Management Platform
  * Copyright (C) 2005-2008 Red Hat, Inc.
  * All rights reserved.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of the GNU General Public License, version 2, as
  * published by the Free Software Foundation, and/or the GNU Lesser
  * General Public License, version 2.1, also as published by the Free
  * Software Foundation.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
  * GNU General Public License and the GNU Lesser General Public License
  * for more details.
  *
  * You should have received a copy of the GNU General Public License
  * and the GNU Lesser General Public License along with this program;
  * if not, write to the Free Software Foundation, Inc.,
  * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
  */
package org.rhq.core.domain.util;

import java.util.Comparator;

/**
 * The specification for what makes up an OSGi compliant version string and how they can be compared was taken from
 * here:
 * http://help.eclipse.org/help31/index.jsp?topic=/org.eclipse.platform.doc.isv/reference/osgi/org/osgi/framework/Version.html
 *
 * @author Charles Crouch
 *
 */
public class OSGiVersionComparator implements Comparator<String> {
    public OSGiVersionComparator() {
    }

    public int compare(String string1, String string2) {
        OSGiVersion ver1 = new OSGiVersion(string1);
        OSGiVersion ver2 = new OSGiVersion(string2);

        int result = ver1.getMajor() - ver2.getMajor();
        if (result == 0) {
            result = ver1.getMinor() - ver2.getMinor();
            if (result == 0) {
                result = ver1.getMicro() - ver2.getMicro();
                if (result == 0) {
                    if (ver1.getQualifier() != null) {
                        if (ver2.getQualifier() == null) {
                            result = 1;
                        } else {
                            result = ver1.getQualifier().compareTo(ver2.getQualifier());
                        }
                    } else {
                        if (ver2.getQualifier() == null) {
                            result = 0;
                        } else {
                            result = -1;
                        }
                    }
                }
            }
        }

        return result;
    }
}