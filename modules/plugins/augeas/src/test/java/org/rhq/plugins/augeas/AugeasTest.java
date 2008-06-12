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

package org.rhq.plugins.augeas;

import com.sun.jna.ptr.PointerByReference;
import com.sun.jna.Pointer;
import org.rhq.plugins.augeas.Augeas;
import org.rhq.plugins.augeas.LibAugeas;

public class AugeasTest {


    public static void main(String[] args) {
        Augeas aug = new Augeas("/opt/augeas-0.2.0/lenses");

//        List<String> matches = aug.match("/files/etc/ssh/sshd_config/*");
//        for (String match : matches) {
//            System.out.r (match);
//        }
        long start = System.currentTimeMillis();
        aug.outputTree("/etc/ssh");
        System.out.println("Time: " + (System.currentTimeMillis() - start));
    }


    public static void main2(String[] args) {

        LibAugeas.Augeas_T aug =
            LibAugeas.INSTANCE.aug_init(
                "/tmp/augeas-sandbox/",
                "/usr/local/share/augeas",
                LibAugeas.AugFlags.AUG_SAVE_BACKUP.getIndex());

        String[] results = new String[200];
        for (int i = 0; i < 200; i++) {
            results[i] = new String();
        }
        PointerByReference pref = new PointerByReference();
        int matches = LibAugeas.INSTANCE.aug_match(aug, "/files/etc/ssh/sshd_config/", pref);

        Pointer[] refs = pref.getValue().getPointerArray(0, matches);

        for (int i = 0; i < matches; i++) {
            System.out.println("Found: " + refs[i].getString(0));
//            System.out.println("Value: " + LibAugeas.INSTANCE.aug_get(aug, refs[i].getString(0)));
        }
        
    }
}
