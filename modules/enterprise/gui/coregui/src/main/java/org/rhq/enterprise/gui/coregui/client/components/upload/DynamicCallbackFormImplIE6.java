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
package org.rhq.enterprise.gui.coregui.client.components.upload;

import com.google.gwt.dom.client.Element;

public class DynamicCallbackFormImplIE6 extends DynamicCallbackFormImpl {
    @Override
    public native void hookEvents(Element iframe,
                                  DynamicCallbackFormImplHost listener) /*-{
	    if (iframe) {
	      iframe.onreadystatechange = function() {
	        // If there is no __formAction yet, this is a spurious onreadystatechange
	        // generated when the iframe is first added to the DOM.
	        if (!iframe.__formAction)
	          return;

	        if (iframe.readyState == 'complete') {
	          // If the iframe's contentWindow has not navigated to the expected action
	          // url, then it must be an error, so we ignore it.
	          listener.@org.rhq.enterprise.gui.coregui.client.components.upload.DynamicCallbackFormImplHost::onFrameLoad()();
	        }
	      };
	    }
	  }-*/;

}
