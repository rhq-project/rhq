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
import java.text.SimpleDateFormat;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.render.Renderer;

import com.sun.faces.util.MessageUtils;

public class HtmlSimpleTriggerRenderer extends Renderer {
    public HtmlSimpleTriggerRenderer() {
        super();
    }

    @Override
    public void decode(FacesContext context, UIComponent component) {
        super.decode(context, component);
        if (context == null) {
            throw new NullPointerException(MessageUtils.getExceptionMessageString(
                MessageUtils.NULL_PARAMETERS_ERROR_MESSAGE_ID, "context"));
        }

        if (component == null) {
            throw new NullPointerException(MessageUtils.getExceptionMessageString(
                MessageUtils.NULL_PARAMETERS_ERROR_MESSAGE_ID, "component"));
        }

        if (!component.isRendered()) {
            return;
        }

        HtmlSimpleTrigger trigger;
        if (component instanceof HtmlSimpleTrigger) {
            trigger = (HtmlSimpleTrigger) component;
        } else {
            return;
        }

        if (trigger.getReadOnly()) {
            // no work to be done when the user can't edit the trigger
            return;
        }

        String dateFormat = trigger.getDateFormat();
        SimpleDateFormat dateFormatter = new SimpleDateFormat(dateFormat);
        HtmlSimpleTriggerRendererModel renderModel = getRenderModel(trigger);

        if (renderModel.isAvailable() == false) {
            /*
             * model knows it doesn't have everything it needs to decode itself.  this is usually the result of using
             * this component inside optionally renderable panels or similar device
             */
            return;
        }

        // deferred
        trigger.setDeferred(renderModel.getDeferred());
        if (trigger.getDeferred() == false) {
            // immediate trigger
            return;
        }

        // startDateTime
        trigger.setStartDateTime(renderModel.getStartDateTime(dateFormatter));

        // repeat
        trigger.setRepeat(renderModel.getRepeat());
        if (trigger.getRepeat() == false) {
            // deferred trigger that executes once
            return;
        }

        // repeat interval
        trigger.setRepeatInterval(renderModel.getRepeatInterval());

        // repeat units
        trigger.setRepeatUnits(renderModel.getRepeatUnits());

        // terminate
        trigger.setTerminate(renderModel.getTerminate());
        if (trigger.getTerminate() == false) {
            // deferred trigger that repeats forever
            return;
        }

        // endDateTime
        trigger.setEndDateTime(renderModel.getEndDateTime(dateFormatter));
    }

    private HtmlSimpleTriggerRendererModel getRenderModel(HtmlSimpleTrigger trigger) {
        HtmlSimpleTriggerRendererType renderType = trigger.getRenderType();
        Class<? extends HtmlSimpleTriggerRendererModel> renderModelClass = renderType.getRenderModel();
        HtmlSimpleTriggerRendererModel renderModel = null;

        try {
            renderModel = renderModelClass.newInstance();
        } catch (InstantiationException ie) {
            throw new RuntimeException(MessageUtils.getExceptionMessageString(
                MessageUtils.RENDERER_NOT_FOUND_ERROR_MESSAGE_ID, "component"));
        } catch (IllegalAccessException iae) {
            throw new RuntimeException(MessageUtils.getExceptionMessageString(
                MessageUtils.RENDERER_NOT_FOUND_ERROR_MESSAGE_ID, "component"));
        }

        return renderModel;
    }

    @Override
    public void encodeBegin(FacesContext context, UIComponent component) throws IOException {
        super.encodeBegin(context, component);

        ResponseWriter writer = context.getResponseWriter();

        HtmlSimpleTrigger trigger = (HtmlSimpleTrigger) component;
        HtmlSimpleTriggerRendererModel renderModel = getRenderModel(trigger);

        writer.startElement("input", component);
        writer.writeAttribute("type", "hidden", null);
        writer.writeAttribute("name", "renderType", null);
        writer.writeAttribute("value", renderModel.getClass().getSimpleName(), "renderType");
        writer.endElement("input");

        trigger.getDynamicBinding(); // initialize this stuff

        renderModel.encode(context, trigger);
    }
}