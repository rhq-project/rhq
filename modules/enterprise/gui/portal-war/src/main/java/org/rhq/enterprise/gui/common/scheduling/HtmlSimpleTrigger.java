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

import java.util.Date;
import javax.faces.component.html.HtmlPanelGroup;
import javax.faces.context.FacesContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.SimpleTrigger;
import org.rhq.core.gui.util.FacesComponentUtility;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.enterprise.gui.common.scheduling.supporting.TimeUnits;

public class HtmlSimpleTrigger extends UISimpleTrigger {
    private static final Log LOG = LogFactory.getLog(HtmlSimpleTrigger.class);

    private HtmlSimpleTriggerRendererType renderType = null;
    private boolean printDateFormat = true;
    private Boolean readOnly = null;

    public HtmlSimpleTrigger() {
        super();
    }

    public HtmlSimpleTrigger(SimpleTrigger trigger) {
        // all scheduled triggers are deferred
        this.setDeferred(true);
        this.setStartDateTime(trigger.getStartTime());

        int repeatCount = trigger.getRepeatCount();
        if (repeatCount != 0) {
            this.setRepeat(true);
            if (repeatCount == SimpleTrigger.REPEAT_INDEFINITELY) {
                this.setRepeatCount(-1);
            } else {
                this.setRepeatCount(repeatCount);
            }
        }

        long repeatMillis = trigger.getRepeatInterval();
        if (repeatMillis != 0) {
            this.setRepeat(true);
            long repeatSecs = repeatMillis / 1000;
            this.setRepeatInterval((int) repeatSecs);
            this.setRepeatUnits(TimeUnits.Seconds);
        }

        // null endDate implies it will trigger on the interval for repeatCount (which includes indefinitely)
        Date endDateTime = trigger.getEndTime();
        if (endDateTime != null) {
            this.setTerminate(true);
            this.setEndDateTime(endDateTime);
        }
    }

    public HtmlSimpleTriggerRendererType getRenderType() {
        if (this.renderType == null) {
            String type = FacesContextUtility.getOptionalRequestParameter("renderType");

            try {
                this.renderType = HtmlSimpleTriggerRendererType.valueOf(type);
            } catch (IllegalArgumentException iae) {
                String viewId = FacesContextUtility.getFacesContext().getViewRoot().getViewId();
                LOG.warn("The render type '" + type + "' " + "for the trigger specified on '" + viewId + "' "
                    + "is unknown.");
            }
        }

        return this.renderType;
    }

    public void setRenderType(HtmlSimpleTriggerRendererType renderType) {
        this.renderType = renderType;
    }

    public boolean getPrintDateFormat() {
        return printDateFormat;
    }

    public void setPrintDateFormat(boolean printDateFormat) {
        this.printDateFormat = printDateFormat;
    }

    public boolean getReadOnly() {
        if (this.readOnly == null) {
            this.readOnly = FacesComponentUtility.getExpressionAttribute(this, "readOnly", Boolean.class);
            if (this.readOnly == null) {
                // if the user doesn't specify the readOnly attribute on the tag - perfectly valid
                this.readOnly = false;
            }
        }

        return this.readOnly;
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    @Override
    public Object saveState(FacesContext context) {
        Object[] values = new Object[4];
        values[0] = super.saveState(context);
        values[1] = this.printDateFormat;
        values[2] = this.readOnly;
        values[3] = this.renderType;

        //values[4] = this.dynamicBinding;
        return values;
    }

    @Override
    public void restoreState(FacesContext context, Object state) {
        Object[] values = (Object[]) state;
        super.restoreState(context, values[0]);
        this.printDateFormat = (Boolean) values[1];
        this.readOnly = (Boolean) values[2];
        this.renderType = (HtmlSimpleTriggerRendererType) values[3];
        //this.dynamicBinding = (HtmlPanelGroup)values[4];
    }

    private HtmlPanelGroup dynamicBinding;

    public HtmlPanelGroup getDynamicBinding() {
        if (this.dynamicBinding == null) {
            this.dynamicBinding = FacesComponentUtility.getExpressionAttribute(this, "dynamicBinding",
                HtmlPanelGroup.class);

            if (this.dynamicBinding != null) {
                // using absolute (not relative) expressions
                populate();
            }
        }

        return this.dynamicBinding;
    }

    public void setDynamicBinding(HtmlPanelGroup dynamicBinding) {
        this.dynamicBinding = dynamicBinding;
        populate();
    }

    private void populate() {
        HtmlSimpleTrigger trigger = (HtmlSimpleTrigger) dynamicBinding.getChildren().get(0);

        this.setDeferred(trigger.getDeferred());
        this.setStartDateTime(trigger.getStartDateTime());
        this.setRepeat(trigger.getRepeat());
        this.setRepeatInterval(trigger.getRepeatInterval());
        this.setRepeatUnits(trigger.getRepeatUnits());
        this.setTerminate(trigger.getTerminate());
        this.setRepeatCount(trigger.getRepeatCount());
        this.setEndDateTime(trigger.getEndDateTime());
    }
}