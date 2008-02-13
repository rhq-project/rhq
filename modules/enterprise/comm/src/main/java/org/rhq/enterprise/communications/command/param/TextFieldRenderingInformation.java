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

/**
 * Class used to encapsulate information specific to text fields.
 *
 * @author <a href="ccrouch@jboss.com">Charles Crouch</a>
 * @author <a href="mazz@jboss.com">John Mazzitelli</a>
 */
public class TextFieldRenderingInformation extends ParameterRenderingInformation implements Serializable {
    /**
     * The serialVersionUID
     */
    private static final long serialVersionUID = -4522547613969935854L;

    private final int fieldLength;
    private final int fieldHeight;

    /**
     * Defines a default length and height for the text field and sets the keys to <code>null</code>. See
     * {@link TextFieldRenderingInformation#TextFieldRenderingInformation(String, String, int, int)} for more
     * information.
     */
    public TextFieldRenderingInformation() {
        this(null, null);
    }

    /**
     * Sets the length and height as given but sets the keys to <code>null</code>. See
     * {@link TextFieldRenderingInformation#TextFieldRenderingInformation(String, String, int, int)} for more
     * information.
     *
     * @param length
     * @param height
     */
    public TextFieldRenderingInformation(int length, int height) {
        this(null, null, length, height);
    }

    /**
     * Defines a default length and height and sets the keys to the given values. See
     * {@link TextFieldRenderingInformation#TextFieldRenderingInformation(String, String, int, int)} for more
     * information.
     *
     * @param labelKey
     * @param descriptionKey
     */
    public TextFieldRenderingInformation(String labelKey, String descriptionKey) {
        this(labelKey, descriptionKey, 30, 1);
    }

    /**
     * Defines the keys and dimensions to their given values. See
     * {@link ParameterRenderingInformation#ParameterRenderingInformation(String, String)} for more information.
     *
     * @param labelKey
     * @param descriptionKey
     * @param length
     * @param height
     */
    public TextFieldRenderingInformation(String labelKey, String descriptionKey, int length, int height) {
        super(labelKey, descriptionKey);
        this.fieldLength = length;
        this.fieldHeight = height;
    }

    /**
     * Returns the field length.
     *
     * @return the field length
     */
    public int getFieldLength() {
        return fieldLength;
    }

    /**
     * Returns the field height.
     *
     * @return the field height
     */
    public int getFieldHeight() {
        return fieldHeight;
    }

    /**
     * None of the attributes specific to this class are localizable, so this simply delegates to the superclass's
     * {@link ParameterRenderingInformation#applyResourceBundle(ResourceBundle)} method.
     *
     * @see ParameterRenderingInformation#applyResourceBundle(ResourceBundle)
     */
    public void applyResourceBundle(ResourceBundle resourceBundle) throws MissingResourceException {
        super.applyResourceBundle(resourceBundle);
    }
}