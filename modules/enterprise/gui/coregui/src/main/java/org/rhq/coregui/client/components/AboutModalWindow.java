/*
 * RHQ Management Platform
 * Copyright (C) 2010-2011 Red Hat, Inc.
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
package org.rhq.coregui.client.components;

import com.smartgwt.client.Version;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.LinkItem;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;

import org.rhq.core.domain.common.ProductInfo;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.ImageManager;
import org.rhq.coregui.client.Messages;
import org.rhq.coregui.client.PopupWindow;

/**
 * The "About RHQ" modal window.
 *
 * @author Ian Springer
 * @author Joseph Marques
 */
public class AboutModalWindow extends PopupWindow {

    private static final Messages MSG = CoreGUI.getMessages();

    public AboutModalWindow(ProductInfo productInfo) {
        super(buildAboutCanvas(productInfo));
        setTitle(MSG.view_aboutBox_title(productInfo.getFullName()));
        setHeight(100);
        setWidth(100);
        setAutoSize(true);
    }

    private static Canvas buildAboutCanvas(ProductInfo productInfo) {

        DynamicForm form = new DynamicForm();
        form.setPadding(10);

        StaticTextItem logoItem = new StaticTextItem("logo");
        logoItem.setValue("<img src=\"" + ImageManager.getFullImagePath("header/rhq_logo_28px.png") + "\"/>");
        logoItem.setHeight(28);
        logoItem.setShowTitle(false);
        logoItem.setColSpan(2);
        logoItem.setWrap(false);
        logoItem.setWrapTitle(false);
        logoItem.setAlign(Alignment.CENTER);

        LinkItem productUrl = new LinkItem("url");
        productUrl.setValue(productInfo.getUrl());
        productUrl.setLinkTitle(MSG.common_label_link());
        productUrl.setTitle(productInfo.getFullName());
        productUrl.setTarget("_blank");
        productUrl.setWrap(false);
        productUrl.setWrapTitle(false);

        StaticTextItem version = new StaticTextItem("version", MSG.view_aboutBox_version());
        version.setValue(productInfo.getVersion());
        version.setWrap(false);
        version.setWrapTitle(false);

        StaticTextItem buildNumber = new StaticTextItem("buildnumber", MSG.view_aboutBox_buildNumber());
        buildNumber.setValue(productInfo.getBuildNumber()
            + (CoreGUI.isRHQ() ? " (<a target='_blank' href='https://github.com/rhq-project/rhq/commit/"
                + productInfo.getBuildNumber() + "'>GitHub</a>)" : ""));
        buildNumber.setWrap(false);
        buildNumber.setWrapTitle(false);

        StaticTextItem gwtVersion = new StaticTextItem("gwtversion", "GWT " + MSG.common_title_version());
        gwtVersion.setValue(MSG.common_buildInfo_gwtVersion());
        gwtVersion.setWrap(false);
        gwtVersion.setWrapTitle(false);

        StaticTextItem smartGwtVersion = new StaticTextItem("smartgwtversion", "SmartGWT " + MSG.common_title_version());
        smartGwtVersion.setValue(Version.getVersion());
        smartGwtVersion.setWrap(false);
        smartGwtVersion.setWrapTitle(false);

        StaticTextItem allRightsReserved = new StaticTextItem();
        allRightsReserved.setValue(MSG.view_aboutBox_allRightsReserved());
        allRightsReserved.setShowTitle(false);
        allRightsReserved.setColSpan(2);
        allRightsReserved.setWrap(false);
        allRightsReserved.setWrapTitle(false);
        allRightsReserved.setAlign(Alignment.CENTER);

        form.setItems(logoItem, productUrl, version, buildNumber, gwtVersion, smartGwtVersion, allRightsReserved);
        return form;
    }

}
