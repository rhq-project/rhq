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
package org.rhq.enterprise.installer;

import java.net.InetAddress;
import java.util.List;
import java.util.Locale;

import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;

import mazz.i18n.Msg;

import org.rhq.enterprise.installer.i18n.InstallerI18NResourceKeys;

/**
 * Defines generic information about one particular server property setting (all except its value - see
 * {@link PropertyItemWithValue} for that).
 *
 * <p>If a property change requires the application server to restart in order for the change to take effect,
 * {@link #isRequiresRestart()} will be <code>true</code>.</p>
 *
 * <p>If a property's value is to be considered a secret (like a password), then {@link #isSecret()} will be <code>
 * true</code>.</p>
 *
 * <p>If a property is considered an advanced setting, one that many users, especially beginners, should not be
 * concerned about most times, then {@link #isAdvanced()} will be <code>true</code>.</p>
 *
 * <p>Each property has a name that corresponds to the actual property name as found in the properties file. There is a
 * {@link #getPropertyLabelResourceBundleKey() resource bundle key} associated with each property that corresponds to
 * the property's human readable label. The localized label is usually what you want to display to the user, since the
 * property names themselves may be cryptic to most users.</p>
 *
 * @author John Mazzitelli
 */
public class PropertyItem {
    private String propertyName;
    private Class<?> propertyType;
    private int fieldSize;
    private boolean requiresRestart;
    private boolean secret;
    private boolean advanced;
    private boolean hidden;
    private List<SelectItem> options;
    private String propertyLabelResourceBundleKey;
    private String helpResourceBundleKey;
    private Msg i18nMsg;

    public PropertyItem(String name, Class<?> type, String labelBundleKey, String helpBundleKey,
        boolean requiresRestart, boolean secret, boolean advanced, boolean hidden) {
        setPropertyName(name);
        setPropertyType(type);
        setPropertyLabelResourceBundleKey(labelBundleKey);
        setHelpResourceBundleKey(helpBundleKey);
        setRequiresRestart(requiresRestart);
        setSecret(secret);
        setAdvanced(advanced);
        setHidden(hidden);

        if (Number.class.isAssignableFrom(type)) {
            setFieldSize(6);
        } else if (Boolean.class.isAssignableFrom(type)) {
            setFieldSize(6);
        } else if (InetAddress.class.isAssignableFrom(type)) {
            setFieldSize(40); // support v6 too
        } else {
            setFieldSize(60);
        }
    }

    // for legacy support
    public PropertyItem(String name, Class<?> type, String labelBundleKey, String helpBundleKey,
        boolean requiresRestart, boolean secret, boolean advanced) {

        this(name, type, labelBundleKey, helpBundleKey, requiresRestart, secret, advanced, false);
    }

    /**
     * Use this constructor to define a property that is rendered with a drop down box of options
     * to choose from.
     */
    public PropertyItem(String name, Class<?> type, String labelBundleKey, String helpBundleKey,
        boolean requiresRestart, boolean secret, boolean advanced, List<SelectItem> options) {
        this(name, type, labelBundleKey, helpBundleKey, requiresRestart, secret, advanced, false);
        setOptions(options);
    }

    public String getPropertyName() {
        return propertyName;
    }

    public void setPropertyName(String propertyName) {
        this.propertyName = propertyName;
    }

    public Class<?> getPropertyType() {
        return propertyType;
    }

    public void setPropertyType(Class<?> propertyType) {
        this.propertyType = propertyType;
    }

    public int getFieldSize() {
        return fieldSize;
    }

    public void setFieldSize(int fieldSize) {
        this.fieldSize = fieldSize;
    }

    public boolean isRequiresRestart() {
        return requiresRestart;
    }

    public void setRequiresRestart(boolean requiresRestart) {
        this.requiresRestart = requiresRestart;
    }

    public boolean isSecret() {
        return secret;
    }

    public void setSecret(boolean secret) {
        this.secret = secret;
    }

    public boolean isAdvanced() {
        return advanced;
    }

    public List<SelectItem> getOptions() {
        return options;
    }

    public void setOptions(List<SelectItem> options) {
        this.options = options;
    }

    public void setAdvanced(boolean hidden) {
        this.advanced = hidden;
    }

    public boolean isHidden() {
        return hidden;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    public String getPropertyLabelResourceBundleKey() {
        return propertyLabelResourceBundleKey;
    }

    public void setPropertyLabelResourceBundleKey(String propertyLabelResourceBundleKey) {
        this.propertyLabelResourceBundleKey = propertyLabelResourceBundleKey;
    }

    public String getHelpResourceBundleKey() {
        return helpResourceBundleKey;
    }

    public void setHelpResourceBundleKey(String helpResourceBundleKey) {
        this.helpResourceBundleKey = helpResourceBundleKey;
    }

    public String getPropertyLabel() {
        return getI18nMsg().getMsg(getPropertyLabelResourceBundleKey());
    }

    public String getHelp() {
        return getI18nMsg().getMsg(getHelpResourceBundleKey());
    }

    private Msg getI18nMsg() {
        if (i18nMsg == null) {
            i18nMsg = new Msg(InstallerI18NResourceKeys.BUNDLE_BASE_NAME, getLocale());
        }

        return i18nMsg;
    }

    /**
     * Return the locale of the user who submitted the current request.
     *
     * @return locale
     */
    private Locale getLocale() {
        return FacesContext.getCurrentInstance().getViewRoot().getLocale();
    }
}