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
package org.rhq.core.system;

import java.util.Arrays;
import org.testng.annotations.Test;

@Test
public class NetworkAdapterInfoTest {
    public void testMacAddressParsing() {
        NetworkAdapterInfo info;
        byte[] bytes;

        info = new NetworkAdapterInfo("", "", "", "aa:bb:cc:dd:ee:ff", "", "UP", false, null, null, null);

        assert info.getMacAddressString() != null;
        assert info.getMacAddressString().equals("aa:bb:cc:dd:ee:ff");
        bytes = info.getMacAddressBytes();
        System.out.println("MAC Address bytes: " + Arrays.toString(bytes));
        assert bytes[0] == (byte) 0xAA;
        assert bytes[1] == (byte) 0xBB;
        assert bytes[2] == (byte) 0xCC;
        assert bytes[3] == (byte) 0xDD;
        assert bytes[4] == (byte) 0xEE;
        assert bytes[5] == (byte) 0xFF;

        info = new NetworkAdapterInfo("", "", "", "01-23-45-67-89-ab", "", "UP", false, null, null, null);

        assert info.getMacAddressString() != null;
        assert info.getMacAddressString().equals("01-23-45-67-89-ab");
        bytes = info.getMacAddressBytes();
        System.out.println("MAC Address bytes: " + Arrays.toString(bytes));
        assert bytes[0] == (byte) 0x01;
        assert bytes[1] == (byte) 0x23;
        assert bytes[2] == (byte) 0x45;
        assert bytes[3] == (byte) 0x67;
        assert bytes[4] == (byte) 0x89;
        assert bytes[5] == (byte) 0xAB;

        info = new NetworkAdapterInfo("", "", "", "00:00:00:gg:00:00", "", "UP", false, null, null, null);
        assert info.getMacAddressString() != null;
        assert info.getMacAddressString().equals("00:00:00:gg:00:00");

        try {
            info.getMacAddressBytes();
        } catch (IllegalArgumentException expected) {
        }

        info = new NetworkAdapterInfo("", "", "", null, "", "UP", false, null, null, null);
        assert info.getMacAddressString() == null;
        assert info.getMacAddressBytes() == null;
    }
}