/*
 * Copyright 2008 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.rhq.enterprise.gui.coregui.client.components.upload;

import com.google.gwt.dom.client.Element;

/**
 * Implementation class used by {@link com.google.gwt.user.client.ui.FormPanel}.
 */
public class DynamicCallbackFormImpl {

    /**
     * Gets the response html from the loaded iframe.
     *
     * @param iframe the iframe from which the response html is to be extracted
     * @return the response html
     */
    public native String getContents(Element iframe) /*-{
    try {
      // Make sure the iframe's window & document are loaded.
      if (!iframe.contentWindow || !iframe.contentWindow.document)
        return null;

      // Get the body's entire inner HTML.
      return iframe.contentWindow.document.body.innerHTML;
    } catch (e) {
      return null;
    }
  }-*/;

    /**
     * Gets the form element's encoding.
     *
     * @param form the form whose encoding is to be retrieved
     * @return the form's encoding type
     */
    public native String getEncoding(Element form) /*-{
    // We can always get 'enctype', no matter which browser, because we set
    // both 'encoding' and 'enctype' in setEncoding().
    return form.enctype;
  }-*/;

    /**
     * Hooks the iframe's onLoad event and the form's onSubmit event.
     *
     * @param iframe   the iframe whose onLoad event is to be hooked
     * @param listener the listener to receive notification
     */
    public native void hookEvents(Element iframe,
                                  DynamicCallbackFormImplHost listener) /*-{
    if (iframe) {
      iframe.onload = function() {
        // If there is no __formAction yet, this is a spurious onload
        // generated when the iframe is first added to the DOM.
        if (!iframe.__formAction)
          return;

        listener.@org.rhq.enterprise.gui.coregui.client.components.upload.DynamicCallbackFormImplHost::onFrameLoad()();
      };
    }

  }-*/;

    /**
     * Submits a form.
     *
     * @param action the submit action
     * @param iframe the iframe that is targetted, or <code>null</code>
     */
    public native void submit(String action, Element iframe) /*-{
    // Hang on to the form's action url, needed in the
    // onload/onreadystatechange handler.
    if (iframe)
      iframe.__formAction = action;
  }-*/;

    /**
     * Unhooks the iframe's onLoad event.
     *
     * @param iframe the iframe whose onLoad event is to be unhooked
     */
    public native void unhookEvents(Element iframe) /*-{
    if (iframe)
      iframe.onload = null;
  }-*/;
}