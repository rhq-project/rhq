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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import mazz.i18n.Logger;
import org.rhq.enterprise.communications.i18n.CommI18NFactory;
import org.rhq.enterprise.communications.i18n.CommI18NResourceKeys;

/**
 * Class used to encapsulate information specific to lists of options.
 *
 * @author <a href="ccrouch@jboss.com">Charles Crouch</a>
 */
public class OptionListRenderingInformation extends ParameterRenderingInformation implements Serializable {
    /**
     * The serialVersionUID
     */
    private static final long serialVersionUID = -8125953068553352997L;

    private static final Logger LOG = CommI18NFactory.getLogger(OptionListRenderingInformation.class);

    private final List optionLabelKeys;
    private List<String> optionLabels;

    /**
     * Defines all keys as <code>null</code>. See
     * {@link OptionListRenderingInformation#OptionListRenderingInformation(String, String, List)} for more information.
     */
    public OptionListRenderingInformation() {
        this(null, null, null);
    }

    /**
     * Create a new {@link OptionListRenderingInformation}. This constructor takes keys to labels and descriptions, and
     * also keys to labels for displaying the rows of a dropdown.
     *
     * @param labelKey
     * @param descriptionKey
     * @param optionLabelKeys list of keys that represent each option - the items in the list must be of type String
     *
     * @see   ParameterRenderingInformation#ParameterRenderingInformation(String, String)
     */
    public OptionListRenderingInformation(String labelKey, String descriptionKey, List optionLabelKeys) {
        super(labelKey, descriptionKey);
        this.optionLabelKeys = optionLabelKeys;
    }

    /**
     * Returns the keys that will be used to look up each option item's label in a
     * {@link #applyResourceBundle(ResourceBundle) resource bundle}.
     *
     * @return the option label keys (the list contains Strings)
     */
    public List getOptionLabelKeys() {
        return optionLabelKeys;
    }

    /**
     * Gets the option label strings as found in the resource bundle.
     *
     * <p>This will return <code>null</code> until
     * {@link #applyResourceBundle(ResourceBundle) a resource bundle has been applied} to this object. The returned list
     * may also be explicitly set via {@link #setOptionLabels(List)}.</p>
     *
     * @return the description string or <code>null</code>
     */
    public List getOptionLabels() {
        return optionLabels;
    }

    /**
     * Explicitly sets the option labels strings. This can be used if you want to supply a default list of option labels
     * without having to {@link #applyResourceBundle(ResourceBundle) apply a resource bundle}.
     *
     * @param optionLabels
     */
    public void setOptionLabels(List<String> optionLabels) {
        this.optionLabels = optionLabels;
    }

    /**
     * Applies the given resource bundle to this object so this object can obtain the actual string values for its keys.
     * See {@link ParameterRenderingInformation#applyResourceBundle(ResourceBundle)} for more information on how this
     * method works.
     *
     * @see ParameterRenderingInformation#applyResourceBundle(ResourceBundle)
     */
    public void applyResourceBundle(ResourceBundle resourceBundle) throws MissingResourceException {
        super.applyResourceBundle(resourceBundle);

        if (resourceBundle != null) {
            if (optionLabelKeys != null) {
                String optionLabelKey = null;
                optionLabels = new ArrayList<String>();
                for (Iterator iter = optionLabelKeys.iterator(); iter.hasNext();) {
                    optionLabelKey = (String) iter.next();

                    // will throw MissingResourceException if key not found
                    String optionLabel = resourceBundle.getString(optionLabelKey);
                    optionLabels.add(optionLabel);
                }
            } else {
                LOG.trace(CommI18NResourceKeys.NOT_USING_OPTION_LABELS_KEY);
            }
        } else {
            if ((optionLabelKeys != null) && !optionLabelKeys.isEmpty()) {
                throw new InvalidParameterDefinitionException(
                    CommI18NResourceKeys.OPTION_LIST_RENDING_INFORMATION_NO_RESOURCE_BUNDLE);
            }
        }
    }
}