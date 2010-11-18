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
package org.rhq.enterprise.gui.coregui.client.test.i18n;

import com.google.gwt.core.client.GWT;
import com.smartgwt.client.widgets.Label;

import org.rhq.enterprise.gui.coregui.client.i18n.TestMessages;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

/**
 * A test class to show how GWT-based i18n can support pluralization
 *  
 * @author Joseph Marques
 */
public class TestPluralizationView extends LocatableVLayout {

    public TestPluralizationView(String locatorId) {
        super(locatorId);
    }

    @Override
    protected void onDraw() {
        super.onDraw();
        build();
    }

    public void build() {
        TestMessages testMessages = GWT.create(TestMessages.class);

        StringBuilder text = new StringBuilder();
        String subject = "rhqadmin";
        for (int cartItems = 0; cartItems < 5; cartItems++) {
            if (cartItems != 0) {
                text.append("<br/>");
            }
            String nextText = "Count " + cartItems + " --> " + testMessages.cartLabel(subject, cartItems);
            text.append(nextText);
        }

        Label label = new Label(text.toString());
        label.setWrap(false);
        addMember(label);
    }

}
