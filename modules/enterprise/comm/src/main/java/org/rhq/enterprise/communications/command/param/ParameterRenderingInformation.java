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
package org.rhq.enterprise.communications.command.param;

import java.io.Serializable;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import mazz.i18n.Logger;
import org.rhq.enterprise.communications.i18n.CommI18NFactory;
import org.rhq.enterprise.communications.i18n.CommI18NResourceKeys;

/**
 * Class used to encapsulate information about how clients should render parameters when displaying them for
 * reading/editing. Supports general rendering information common to all Parameters.
 *
 * <p>This class uses resource bundles to support internationalization/localization of the information. You must
 * {@link #applyResourceBundle(ResourceBundle) apply a resource bundle} to this object in order to obtain the strings
 * that are to be used to render the parameter information.</p>
 *
 * @author <a href="ccrouch@jboss.com">Charles Crouch</a>
 * @author <a href="mazz@jboss.com">John Mazzitelli</a>
 */
public class ParameterRenderingInformation implements Serializable {
    /**
     * The serialVersionUID
     */
    private static final long serialVersionUID = -7640644266857817767L;

    private static final Logger LOG = CommI18NFactory.getLogger(ParameterRenderingInformation.class);

    // the keys defined when the constructor is called - these are immutable
    private final String labelKey;
    private final String descriptionKey;

    // the values that are either explicitly set or found in the applied resource bundle
    private String label;
    private String description;

    // the common metadata for the parameter
    private boolean hidden;
    private boolean readOnly;
    private boolean obfuscated;

    /**
     * Create a new {@link ParameterRenderingInformation}. This constructor takes keys to labels and descriptions,
     * rather than actual values. Those keys will later be used to look up the values from a
     * {@link #applyResourceBundle(ResourceBundle) resource bundle}.
     *
     * <p>A key can be <code>null</code>; for those keys that are <code>null</code>, they simply will not be looked up
     * in a resource bundle. In this case you need to explicitly call the setter to define the value. For example, if
     * <code>labelKey</code> is <code>null</code>, then the only way for {@link #getLabel()} to return a
     * non-<code>null</code> label string would be if you explicitly call {@link #setLabel(String)} since the label
     * string will not be found in any resource bundle.</p>
     *
     * <p>By default, the parameter is defined as:
     *
     * <ul>
     *   <li>not hidden</li>
     *   <li>not read-only</li>
     *   <li>not obfuscated</li>
     * </ul>
     * </p>
     *
     * @param labelKey       the key into a resource bundle where the resource bundle string is the parameter label (may
     *                       be <code>null</code>)
     * @param descriptionKey the key into a resource bundle where the resource bundle string is the parameter
     *                       description (may be <code>null</code>)
     */
    public ParameterRenderingInformation(String labelKey, String descriptionKey) {
        this.labelKey = labelKey;
        this.descriptionKey = descriptionKey;
        this.hidden = false;
        this.readOnly = false;
        this.obfuscated = false;
    }

    /**
     * Create a new {@link ParameterRenderingInformation} with all keys being <code>null</code>. See
     * {@link ParameterRenderingInformation#ParameterRenderingInformation(String, String)} for more information.
     */
    public ParameterRenderingInformation() {
        this(null, null);
    }

    /**
     * Returns the key that will be used to look up the label in a
     * {@link #applyResourceBundle(ResourceBundle) resource bundle}.
     *
     * @return the description key
     */
    public String getLabelKey() {
        return this.labelKey;
    }

    /**
     * Returns the key that will be used to look up the description in a
     * {@link #applyResourceBundle(ResourceBundle) resource bundle}.
     *
     * @return the description key
     */
    public String getDescriptionKey() {
        return this.descriptionKey;
    }

    /**
     * Get the description string as found in the resource bundle.
     *
     * <p>This will return <code>null</code> until
     * {@link #applyResourceBundle(ResourceBundle) a resource bundle has been applied} to this object. This description
     * string may also be explicitly set via {@link #setDescription(String)}.</p>
     *
     * @return the description string or <code>null</code>
     */
    public String getDescription() {
        return description;
    }

    /**
     * Explicitly sets the description string. This can be used if you want to supply a default description without
     * having to {@link #applyResourceBundle(ResourceBundle) apply a resource bundle}.
     *
     * @param description The description to set.
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Get the label string as found in the resource bundle.
     *
     * <p>This will return <code>null</code> until
     * {@link #applyResourceBundle(ResourceBundle) a resource bundle has been applied} to this object. This label string
     * may also be explicitly set via {@link #setLabel(String)}.</p>
     *
     * @return the label string or <code>null</code>
     */
    public String getLabel() {
        return label;
    }

    /**
     * Explicitly sets the label string. This can be used if you want to supply a default label without having to
     * {@link #applyResourceBundle(ResourceBundle) apply a resource bundle}.
     *
     * @param label The label to set.
     */
    public void setLabel(String label) {
        this.label = label;
    }

    /**
     * Applies the given resource bundle to this object so this object can obtain the actual string values for the label
     * and description. If a {@link #setLabel(String) label was explicitly set} or a
     * {@link #setDescription(String) description was explicitly set}, those values will be overwritten by the values
     * found in the given resource bundle. If the given bundle is <code>null</code>, this method does nothing.
     *
     * <p>Note that if the resource bundle was <code>null</code> but one or more keys were not <code>null</code>, an
     * exception is thrown. In this case, you must either define a resource bundle or you must not define keys.</p>
     *
     * @param  resourceBundle the resource bundle where the key values are found (may be <code>null</code>)
     *
     * @throws MissingResourceException            if a key was not found in the resource bundle
     * @throws InvalidParameterDefinitionException key(s) were non-<code>null</code> but <code>resourceBundle</code> was
     *                                             <code>null</code>
     */
    public void applyResourceBundle(ResourceBundle resourceBundle) throws MissingResourceException {
        if (resourceBundle != null) {
            if (labelKey != null) {
                // will throw MissingResourceException if key not found
                label = resourceBundle.getString(labelKey);
            } else {
                LOG.trace(CommI18NResourceKeys.NOT_USING_LABEL_KEY);
            }

            if (descriptionKey != null) {
                // will throw MissingResourceException if key not found
                description = resourceBundle.getString(descriptionKey);
            } else {
                LOG.trace(CommI18NResourceKeys.NOT_USING_DESC_KEY);
            }
        } else {
            if ((labelKey != null) || (descriptionKey != null)) {
                throw new InvalidParameterDefinitionException(
                    CommI18NResourceKeys.PARAMETER_RENDING_INFORMATION_NO_RESOURCE_BUNDLE);
            }
        }

        return;
    }

    /**
     * Indicates if this parameter should be considered read-only (which usually means a user cannot alter the
     * parameter's value within a user interface).
     *
     * @return the read-only flag
     */
    public boolean isReadOnly() {
        return readOnly;
    }

    /**
     * Sets the read-only flag.
     *
     * @param readOnly
     */
    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    /**
     * Indicates if this parameter should be considered hidden (which usually means this parameter is hidden from view
     * in a user interface).
     *
     * @return the hidden flag
     */
    public boolean isHidden() {
        return hidden;
    }

    /**
     * Set the hidden flag.
     *
     * @param hidden
     */
    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    /**
     * Indicates if this parameter should be obfuscated (which usually means the value should be garbled in a user
     * interface, such as the typical "***" in a password field).
     *
     * @return the obfuscated flag
     */
    public boolean isObfuscated() {
        return obfuscated;
    }

    /**
     * Set the obfuscated flag.
     *
     * @param obfuscated
     */
    public void setObfuscated(boolean obfuscated) {
        this.obfuscated = obfuscated;
    }
}