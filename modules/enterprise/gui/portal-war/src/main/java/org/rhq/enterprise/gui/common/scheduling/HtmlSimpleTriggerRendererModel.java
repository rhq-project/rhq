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
package org.rhq.enterprise.gui.common.scheduling;

import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;
import javax.faces.context.FacesContext;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.enterprise.gui.common.scheduling.supporting.TimeUnits;

/*
 * use a structured method for retrieving the different parts of the trigger so that the decode method in
 * HtmlSimpleTriggerRenderer can reuse the conditional logic to process the various dependent parts of it
 */
public interface HtmlSimpleTriggerRendererModel {
    /**
     * This method is a marker that helps the renderer decide whether it should even try to decode this model from the
     * context. For example, if the trigger will be placed in a panel that doesn't get rendered the first time some view
     * is hit, it will still be placed in the context. Then, after some user action on the page, when that context is
     * deserialized and the various decode methods of the components are called, this method should return false.
     * isAvailable should only return true when the model knows it has everything it needs for submission and is ready
     * to be properly decoded. Usually, as in the above scenario, if you place this component into an initially
     * non-rendered panel, all you have to do is test for existence of one of your form parameters. The difference
     * between this method and the rest is that this one should NOT fail in any circumstance. In other words, you are
     * encouraged (if not required) to use {@link FacesContextUtility}'s getOptionalRequestParameter set of methods.
     */
    boolean isAvailable();

    boolean getDeferred();

    Date getStartDateTime(DateFormat dateFormatter);

    boolean getRepeat();

    int getRepeatInterval();

    TimeUnits getRepeatUnits();

    boolean getTerminate();

    Date getEndDateTime(DateFormat dateFormatter);

    /*
     * encode is just a big chunk, no need to break this down as the encoding methodologies could vary widely
     */
    void encode(FacesContext context, HtmlSimpleTrigger trigger) throws IOException;
}