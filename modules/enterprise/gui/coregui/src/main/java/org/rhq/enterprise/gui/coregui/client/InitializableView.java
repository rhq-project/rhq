/*
 * RHQ Management Platform
 * Copyright (C) 2010 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.client;

import com.smartgwt.client.widgets.BaseWidget;

/**
 * The SmartGWT BaseWidget class provides no way to check if the widget has completed its init()/onInit(), so
 * components that wish to provide that information can implement this interface. The widget that implements this
 * class should define a member variable (e.g. <tt>isInitialized</tt>) which is set to false at class construction time
 * and then flipped to true as the last line of the widget's implementation of {@link BaseWidget#onInit()}.
 * The widget's implementation of {@link #isInitialized()} would then simply return <tt>isInitialized</tt>.
 *
 * @author Ian Springer
 */
public interface InitializableView {

    /**
     * Return true if this widget's {@link BaseWidget#init() initialization} has completed, or false otherwise.
     *
     * @return true if this widget's {@link BaseWidget#init() initialization} has completed, or false otherwise
     */
    boolean isInitialized();

}