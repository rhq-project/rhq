/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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
package org.rhq.coregui.client.components.wizard;

import com.smartgwt.client.widgets.Canvas;

import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.Messages;

/**
 * A step in a {@link Wizard wizard}.
 * 
 * @author Greg Hinkle
 */
public interface WizardStep {

    Messages MSG = CoreGUI.getMessages();

    /**
     * Returns the canvas containing this step's widget content. This method should <b>not</b> add the canvas to the
     * parent canvas - the wizard framework will take care of that.
     *
     * @return the canvas containing this step's widget content
     */
    Canvas getCanvas();

    /**
     * Returns true if this step's Next or Finish button should be enabled, or false if it should be disabled.
     *
     * @return true if this step's Next or Finish button should be enabled, or false if it should be disabled
     */
    boolean isNextButtonEnabled();

    /**
     * Called when the user clicks this step's Next or Finish button. If all required data has been entered for the
     * step and is valid, true should be returned, and the wizard will advance to the next step, otherwise false
     * should be returned, and the wizard will not advance.
     *
     * @return true if all required data has been entered for the step and is valid, or false otherwise
     */
    boolean nextPage();

    /**
     * Called when the user clicks this step's Previous button. Should return true if the wizard should go to the
     * previous step, or false if it should not.
     *
     * @return true if the wizard should go to the previous step, or false if it should not.
     */
    boolean previousPage();

    /**
     * Returns the title of this step, to be displayed in the wizard modal's title bar.
     *
     * @return the title of this step, to be displayed in the wizard modal's title bar
     */
    String getName();

}
