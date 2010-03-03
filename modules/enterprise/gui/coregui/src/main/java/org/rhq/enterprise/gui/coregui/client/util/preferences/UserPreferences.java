/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.client.util.preferences;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.configuration.Configuration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Greg Hinkle
 */
public class UserPreferences {


    private Subject subject;

    private Configuration userConfiguration;


    protected static final String PREF_ITEM_DELIM = "|";

    public static final String PREF_DASH_FAVORITE_RESOURCES = ".dashContent.resourcehealth.resources";


    public UserPreferences(Subject subject) {
        this.subject = subject;
        subject.getUserConfiguration();
    }

    





    public List<Integer> getFavoriteResources() {
        return getPreferenceAsIntegerList(PREF_DASH_FAVORITE_RESOURCES,PREF_ITEM_DELIM);
    }



    protected String getPreference(String name) {
        return userConfiguration.getSimpleValue(name, null);
    }

    protected List<String> getPreferenceAsList(String key, String delimiter) {
        String pref = null;
        try {
            pref = getPreference(key);
        } catch (IllegalArgumentException e) {
//            log.debug("A user preference named '" + key + "' does not exist.");
        }

        return (pref != null) ? Arrays.asList(pref.split(delimiter)) : new ArrayList<String>();
    }

    protected List<Integer> getPreferenceAsIntegerList(String key, String delimiter) {
        try {
            List<String> value = getPreferenceAsList(key, delimiter);

            List<Integer> result = new ArrayList<Integer>(value.size());
            for (int i = 0; i < value.size(); i++) {
                String trimmed = value.get(i).trim();
                if (trimmed.length() > 0) {
                    result.add(Integer.valueOf(trimmed));
                }
            }

            return result;
        } catch (Exception e) {
            return new ArrayList<Integer>();
        }
    }


}
