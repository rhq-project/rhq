/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.client.components.wizard;

import java.util.List;

import com.smartgwt.client.widgets.IButton;

import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.Messages;

/**
 * @author Greg Hinkle
 */
public interface Wizard {

    Messages MSG = CoreGUI.getMessages();

    String getWindowTitle();

    String getTitle();

    String getSubtitle();

    List<WizardStep> getSteps();

    List<IButton> getCustomButtons(int step);

    /**
     * This is called when the cancel button or the wizard window's "X" close button is clicked. 
     */
    void cancel();
}