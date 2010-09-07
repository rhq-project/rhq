/*
 * RHQ Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.client.components.upload;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.GWT.UncaughtExceptionHandler;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;

import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableDynamicForm;

public class DynamicCallbackForm extends LocatableDynamicForm implements DynamicCallbackFormImplHost {

    private static int frameIndex = 0;

    private String frameName;
    private Element synthesizedFrame;
    private DynamicCallbackFormHandlerCollection formHandlers;
    private static DynamicCallbackFormImpl impl = GWT.create(DynamicCallbackFormImpl.class);

    public DynamicCallbackForm(String locatorId, String frameName) {
        super(locatorId);
        this.frameName = frameName + (frameIndex++);
        setTarget(this.frameName);
        init();
    }

    @Override
    protected void onAttach() {
        super.onAttach();
    }

    private void init() {
        if (frameName != null) {
            // Create and attach a hidden iframe to the body element.
            createFrame();
            Document.get().getBody().appendChild(synthesizedFrame);
            //
            // Hook up the underlying iframe's onLoad event when attached to the DOM.
            // Making this connection only when attached avoids memory-leak issues.
            // The FormPanel cannot use the built-in GWT event-handling mechanism
            // because there is no standard onLoad event on iframes that works across browsers.
            impl.hookEvents(synthesizedFrame, this);
        }
    }

    private void createFrame() {
        // Attach a hidden IFrame to the form. This is the target iframe to which
        // the form will be submitted. We have to create the iframe using innerHTML,
        // because setting an iframe's 'name' property dynamically doesn't work on
        // most browsers.
        Element dummy = Document.get().createDivElement();
        dummy.setInnerHTML("<iframe src=\"javascript:''\" name='" + frameName
            + "' style='position:absolute;width:0;height:0;border:0'>");

        synthesizedFrame = dummy.getFirstChildElement();
    }

    @Override
    public void submitForm() {
        impl.submit(getAction(), synthesizedFrame);
        super.submitForm();
    }

    public void onFrameLoad() {
        UncaughtExceptionHandler handler = GWT.getUncaughtExceptionHandler();
        if (handler != null) {
            onFrameLoadAndCatch(handler);
        } else {
            onFrameLoadImpl();
        }
        // Window.alert("Submission ready!");
    }

    private void onFrameLoadAndCatch(UncaughtExceptionHandler handler) {
        try {
            onFrameLoadImpl();
        } catch (Throwable e) {
            handler.onUncaughtException(e);
        }
    }

    private void onFrameLoadImpl() {
        if (formHandlers != null) {
            // Fire onComplete events in a deferred command. This is necessary
            // because clients that detach the form panel when submission is
            // complete can cause some browsers (i.e. Mozilla) to go into an
            // 'infinite loading' state. See issue 916.
            DeferredCommand.addCommand(new Command() {
                public void execute() {
                    formHandlers.fireOnComplete(DynamicCallbackForm.this, impl.getContents(synthesizedFrame));
                }
            });
        }
    }

    @Override
    public void destroy() {
        if (synthesizedFrame != null) {
            // Unhook the iframe's onLoad when detached.
            impl.unhookEvents(synthesizedFrame);

            // And remove it from the document.
            Document.get().getBody().removeChild(synthesizedFrame);
            synthesizedFrame = null;
        }

        super.destroy();
    }

    public synchronized void addFormHandler(DynamicFormHandler handler) {
        if (formHandlers == null) {
            formHandlers = new DynamicCallbackFormHandlerCollection();
        }
        formHandlers.add(handler);
    }

    public synchronized void pushFormHandler(DynamicFormHandler handler) {
        if (formHandlers == null) {
            formHandlers = new DynamicCallbackFormHandlerCollection();
        }
        formHandlers.add(0, handler);
    }

    public void removeFormHandler(DynamicFormHandler handler) {
        if (formHandlers != null) {
            formHandlers.remove(handler);
        }
    }

}
