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

package org.rhq.core.util.file;

import java.util.List;

import org.testng.annotations.Test;

public class DiffUtilTest {

    @Test
    public void generateUnifiedDiff() throws Exception {
        String original = "This is a test for diffing.\n" +
                          "I want to see what it generates for a unified diff.\n" +
                          "\n" +
                          "- John";

        String modified = "This a test for diffing files.\n" +
                          "I want to see what it generates for a unified diff.\n" +
                          "I will eventually test other capabilities as well.\n" +
                          "\n" +
                          "- John";

        List<String> deltas = DiffUtil.generateUnifiedDiff(original, modified);
        for (String delta : deltas) {
            System.out.println(delta);
        }
    }

}
