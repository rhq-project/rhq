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
package org.rhq.enterprise.server.alert.i18n;

import mazz.i18n.annotation.I18NMessage;
import mazz.i18n.annotation.I18NMessages;
import mazz.i18n.annotation.I18NResourceBundle;

@I18NResourceBundle(baseName = "alert-messages", defaultLocale = "en")
public interface AlertI18NResourceKeys {
    @I18NMessages( { @I18NMessage("Availability goes {0}") })
    String ALERT_CONFIG_PROPS_CB_AVAILABILITY = "alert.config.props.CB.Availability";

    @I18NMessages( { @I18NMessage("Event/Log Level: {0}") })
    String ALERT_CONFIG_PROPS_CB_LOG_CONDITION = "alert.config.props.CB.LogCondition";

    @I18NMessages( { @I18NMessage("Event/Log Level: {0} and matching substring \"{1}\"") })
    String ALERT_CONFIG_PROPS_CB_LOG_CONDITION_STRING_MATCH = "alert.config.props.CB.LogCondition.StringMatch";

    @I18NMessages( { @I18NMessage("value changed") })
    String ALERT_CURRENT_LIST_VALUE_CHANGED = "alert.current.list.ValueChanged";

    @I18NMessages( { @I18NMessage("\\  - Condition {0}: {1}\\n\\\n" + "\\  - Date/Time: {2}\\n\\\n"
        + "\\  - Log Value: {3}\\n\\\n") })
    String ALERT_EMAIL_CONDITION_LOG_FORMAT = "alert.email.condition.log.format";
}