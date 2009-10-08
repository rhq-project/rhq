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
package org.rhq.enterprise.gui.common.time.converter;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;

import org.rhq.enterprise.gui.legacy.WebUser;
import org.rhq.enterprise.gui.legacy.WebUserPreferences;
import org.rhq.enterprise.gui.legacy.WebUserPreferences.DateTimeDisplayPreferences;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;

/**
 * @author Joseph Marques
 */
public class UserDateTimeConverter implements Converter {

    public SimpleDateFormat getFormatter() {
        WebUser user = EnterpriseFacesContextUtility.getWebUser();
        WebUserPreferences preferences = user.getWebPreferences();
        DateTimeDisplayPreferences displayPreferences = preferences.getDateTimeDisplayPreferences();

        String pattern = displayPreferences.dateTimeFormat;
        SimpleDateFormat df = new SimpleDateFormat(pattern);

        return df;
    }

    public Object getAsObject(FacesContext context, UIComponent component, String value) {
        SimpleDateFormat df = getFormatter();

        try {
            Date timestampAsDate = df.parse(value);
            Long timestampAsLong = timestampAsDate.getTime();
            return timestampAsLong;
        } catch (ParseException pe) {
            // should never happen because we rendered the string-i-fied value using the same formatter
            return 0L;
        }
    }

    public String getAsString(FacesContext context, UIComponent component, Object value) {
        SimpleDateFormat df = getFormatter();

        Long timestampAsLong = (Long) value;
        String timestampAsString = df.format(timestampAsLong);

        return timestampAsString;
    }
}
