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
package org.rhq.enterprise.gui.common.time;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.render.Renderer;

import com.sun.faces.util.MessageUtils;

import org.rhq.enterprise.gui.common.framework.ServerInfoUIBean;
import org.rhq.enterprise.gui.legacy.WebUser;
import org.rhq.enterprise.gui.legacy.WebUserPreferences;
import org.rhq.enterprise.gui.legacy.WebUserPreferences.DateTimeDisplayPreferences;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;

/**
 * @author Joseph Marques
 */
public class UserFormatterRenderer extends Renderer {

    @Override
    public void decode(FacesContext context, UIComponent component) {
        super.decode(context, component);
        if (context == null) {
            throw new NullPointerException(MessageUtils.getExceptionMessageString(
                MessageUtils.NULL_PARAMETERS_ERROR_MESSAGE_ID, "context"));
        }

        if (component == null) {
            throw new NullPointerException(MessageUtils.getExceptionMessageString(
                MessageUtils.NULL_PARAMETERS_ERROR_MESSAGE_ID, "component"));
        }

        if (!component.isRendered()) {
            return;
        }

        UserFormatterComponent formatter;
        if (component instanceof UserFormatterComponent) {
            formatter = (UserFormatterComponent) component;
        } else {
            return;
        }

    }

    @Override
    public void encodeBegin(FacesContext context, UIComponent component) throws IOException {
        super.encodeBegin(context, component);

        ResponseWriter writer = context.getResponseWriter();

        UserFormatterComponent formatter = (UserFormatterComponent) component;
        UserFormatterComponent.Type type = formatter.getEnumType();

        WebUser user = EnterpriseFacesContextUtility.getWebUser();
        WebUserPreferences preferences = user.getWebPreferences();
        DateTimeDisplayPreferences displayPreferences = preferences.getDateTimeDisplayPreferences();

        String pattern;
        if (type == UserFormatterComponent.Type.DATE) {
            pattern = displayPreferences.dateFormat;
        } else if (type == UserFormatterComponent.Type.TIME) {
            pattern = displayPreferences.timeFormat;
        } else if (type == UserFormatterComponent.Type.DATETIME) {
            pattern = displayPreferences.dateTimeFormat;
        } else {
            throw new IllegalArgumentException("Unknown or unsupported type for UserDateTime component: '" + type + "'");
        }

        long dateTimeUTC;
        if (formatter.isLocal()) {
            // if time is already local, get the offset and calculate UTC time as input to the formatter
            TimeZone tz = new ServerInfoUIBean().getTimeZone();
            long localTime = formatter.getValue();
            int offset = tz.getOffset(localTime);
            dateTimeUTC = localTime + offset; // localTime = dateTimeUTC - offset
        } else {
            dateTimeUTC = formatter.getValue();
        }
        SimpleDateFormat df = new SimpleDateFormat(pattern);
        String formatted = df.format(new Date(dateTimeUTC));

        writer.write(formatted);
    }
}
