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

import difflib.DiffUtils;
import difflib.Patch;

import static java.util.Arrays.asList;

public class DiffUtil {

    public static List<String> generateUnifiedDiff(String oldContent, String newContent) {
        List<String> oldList = asList(oldContent.split("\\n"));
        List<String> newList = asList(newContent.split("\\n"));

        Patch patch = DiffUtils.diff(oldList, newList);

        return DiffUtils.generateUnifiedDiff("test.txt:12", "test.txt:32", oldList, patch, 5);
    }

}
