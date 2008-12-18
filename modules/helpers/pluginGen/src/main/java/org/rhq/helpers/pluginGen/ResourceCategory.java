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
package org.rhq.helpers.pluginGen;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ArrayList;

/**
 * Possible categories
 * @author Heiko W. Rupp
 */
public enum ResourceCategory {
    /** Possible categories */
    PLATFORM('P'), SERVER('S'), SERVICE('I');

    char abbrevLetter;

    private ResourceCategory(char abbrev) {
        abbrevLetter = abbrev;
    }

    private static Map<ResourceCategory,List<ResourceCategory>> enumMap =
            new EnumMap<ResourceCategory, List<ResourceCategory>>(ResourceCategory.class);

    static {
        for (ResourceCategory cat : ResourceCategory.values()) {
            List<ResourceCategory> catList = new ArrayList<ResourceCategory>();
            switch (cat) {
                case PLATFORM:
                    catList.addAll(Arrays.asList(PLATFORM, SERVER, SERVICE));
                    break;
                case SERVER:
                    catList.addAll(Arrays.asList(SERVER, SERVICE));
                    break;
                case SERVICE:
                    catList.addAll(Arrays.asList(SERVICE));
                    break;
            }
            enumMap.put(cat,catList);
        }
    }

    public static List<ResourceCategory> getPossibleChildren(ResourceCategory parent) {

        if (parent == null)
            return enumMap.get(PLATFORM);
        else
            return enumMap.get(parent);

    }

    public char getAbbrev() {
        return abbrevLetter;
    }

    public static ResourceCategory getByAbbrv(char abbrev) {
        EnumSet<ResourceCategory> set = EnumSet.allOf(ResourceCategory.class);
        for (ResourceCategory cat : set) {
            if (cat.getAbbrev()==abbrev)
                return cat;
        }
        return null;
    }

    public String getLowerName() {
        return toString().toLowerCase(Locale.getDefault());
    }
}
