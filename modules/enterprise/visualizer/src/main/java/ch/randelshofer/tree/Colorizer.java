/*
 * @(#)Colorizer.java  1.0  September 25, 2007
 *
 * Copyright (c) 2007 Werner Randelshofer
 * Staldenmattweg 2, CH-6405 Immensee, Switzerland
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Werner Randelshofer. ("Confidential Information").  You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Werner Randelshofer.
 */

package ch.randelshofer.tree;

import java.awt.Color;

/**
 * Colorizer.
 *
 *
 *
 *
 * @author Werner Randelshofer
 * @version 1.0 September 25, 2007 Created.
 */
public interface Colorizer {

    /**
     * Gets a color for the specified value.
     * @param value A value between 0 and 1.
     */
    public Color get(float value);

}
