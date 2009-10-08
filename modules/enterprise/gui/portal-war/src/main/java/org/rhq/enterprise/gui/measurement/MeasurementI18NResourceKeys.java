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
package org.rhq.enterprise.gui.measurement;

import mazz.i18n.Msg;
import mazz.i18n.Msg.BundleBaseName;
import mazz.i18n.annotation.I18NMessage;
import mazz.i18n.annotation.I18NMessages;
import mazz.i18n.annotation.I18NResourceBundle;

/**
 * Some I18N messages used for Measurement stuff in the GUI NOTE: THIS FILE NEEDS TO BE ISO-8859-1/-15 encoded!
 *
 * @author Heiko W. Rupp
 */
@I18NResourceBundle(baseName = "MeasurementGuiMessages", defaultLocale = "en")
public interface MeasurementI18NResourceKeys {
    BundleBaseName BUNDLE_BASE_NAME = new Msg.BundleBaseName("MeasurementGuiMessages");

    @I18NMessages( { @I18NMessage(locale = "en", value = "Name"), @I18NMessage(locale = "de", value = "Name"),
        @I18NMessage(locale = "fr", value = "Nom") })
    String NAME = "name";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Value"), @I18NMessage(locale = "de", value = "Wert"),
        @I18NMessage(locale = "fr", value = "Valeur") })
    String VALUE = "value";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Last changed"),
        @I18NMessage(locale = "de", value = "Letzte �nderung"),
        @I18NMessage(locale = "fr", value = "Derni�re change") })
    String LAST_CHANGED = "lastChanged";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Trait"), @I18NMessage(locale = "de", value = "Merkmal"),
        @I18NMessage(locale = "fr", value = "Trait") // there is probably something better
    })
    String TRAIT = "trait";
    @I18NMessages( { @I18NMessage(locale = "de", value = "seit"), @I18NMessage(locale = "en", value = "since"),
        @I18NMessage(locale = "fr", value = "depuis") })
    String SINCE = "since";
}