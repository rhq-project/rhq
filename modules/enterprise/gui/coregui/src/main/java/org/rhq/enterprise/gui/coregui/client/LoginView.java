/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.rhq.enterprise.gui.coregui.client;

import java.util.EnumSet;
import java.util.Map;

import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.FormErrorOrientation;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.Img;
import com.smartgwt.client.widgets.Window;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.events.SubmitValuesEvent;
import com.smartgwt.client.widgets.form.events.SubmitValuesHandler;
import com.smartgwt.client.widgets.form.fields.CanvasItem;
import com.smartgwt.client.widgets.form.fields.HeaderItem;
import com.smartgwt.client.widgets.form.fields.PasswordItem;
import com.smartgwt.client.widgets.form.fields.RowSpacerItem;
import com.smartgwt.client.widgets.form.fields.SubmitItem;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.form.fields.events.KeyPressEvent;
import com.smartgwt.client.widgets.form.fields.events.KeyPressHandler;

import org.rhq.core.domain.resource.ResourceType;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.type.ResourceTypeRepository;
import org.rhq.enterprise.gui.coregui.client.util.BrowserUtility;

/**
 * @author Greg Hinkle
 * @author Joseph Marques
 */
public class LoginView extends Canvas {

    private static boolean loginShowing = false;

    private Window window;
    private DynamicForm form;

    private SubmitItem loginButton;

    public LoginView() {
    }

    public void showLoginDialog() {
        if (!loginShowing) {
            loginShowing = true;
            UserSessionManager.logout();

            form = new DynamicForm();
            form.setMargin(25);
            form.setAutoFocus(true);
            form.setShowErrorText(true);
            form.setErrorOrientation(FormErrorOrientation.BOTTOM);

            CanvasItem logo = new CanvasItem();
            logo.setCanvas(new Img("header/rhq_logo_28px.png", 80, 28));
            logo.setShowTitle(false);

            HeaderItem header = new HeaderItem();
            header.setValue("Please Login");

            TextItem user = new TextItem("user", "User");
            user.setRequired(true);
            user.setAttribute("autoComplete", "native");

            final PasswordItem password = new PasswordItem("password", "Password");
            password.setRequired(true);
            password.setAttribute("autoComplete", "native");

            loginButton = new SubmitItem("login", "Login");
            loginButton.setAlign(Alignment.CENTER);
            loginButton.setColSpan(2);

            user.addKeyPressHandler(new KeyPressHandler() {
                public void onKeyPress(KeyPressEvent event) {
                    if ((event.getCharacterValue() != null) && (event.getCharacterValue() == KeyCodes.KEY_ENTER)) {
                        password.focusInItem(); // Work around the form not getting auto-fill values until the field is focused
                    }
                }
            });
            password.addKeyPressHandler(new KeyPressHandler() {
                public void onKeyPress(KeyPressEvent event) {
                    if ((event.getCharacterValue() != null) && (event.getCharacterValue() == KeyCodes.KEY_ENTER)) {
                        form.submit();
                    }
                }
            });

            form.setFields(logo, header, new RowSpacerItem(), user, password, loginButton);

            window = new Window();
            window.setWidth(400);
            window.setHeight(275);
            window.setTitle("Welcome");

            // forced focused, static size, can't close / dismiss
            window.setIsModal(true);
            window.setShowModalMask(true);
            window.setCanDragResize(false);
            window.setCanDragReposition(false);
            window.setShowCloseButton(false);
            window.setShowMinimizeButton(false);
            window.setAutoCenter(true);

            window.addItem(form);
            window.show();

            form.addSubmitValuesHandler(new SubmitValuesHandler() {
                public void onSubmitValues(SubmitValuesEvent submitValuesEvent) {
                    if (form.validate()) {
                        login(form.getValueAsString("user"), form.getValueAsString("password"));
                    }
                }
            });
        }
    }

    private void login(String user, String password) {
        BrowserUtility.forceIe6Hacks();

        loginButton.setDisabled(true);

        try {
            RequestBuilder b = new RequestBuilder(RequestBuilder.GET, "/j_security_check.do?j_username=" + user
                + "&j_password=" + password);
            b.setCallback(new RequestCallback() {
                public void onResponseReceived(Request request, Response response) {
                    int statusCode = response.getStatusCode();
                    if (statusCode == 200) {
                        window.destroy();
                        loginShowing = false;
                        UserSessionManager.login();
                    } else {
                        handleError(statusCode);
                    }
                }

                public void onError(Request request, Throwable exception) {
                    handleError(0);
                }
            });
            b.send();
        } catch (Exception e) {
            handleError(0);
        } finally {
            BrowserUtility.unforceIe6Hacks();
        }
    }

    @SuppressWarnings("unused")
    private void preloadAllTypeMetadata() {
        ResourceTypeRepository.Cache.getInstance().getResourceTypes((Integer[]) null,
            EnumSet.allOf(ResourceTypeRepository.MetadataType.class), new ResourceTypeRepository.TypesLoadedCallback() {
                public void onTypesLoaded(Map<Integer, ResourceType> types) {
                    com.allen_sauer.gwt.log.client.Log.info("Preloaded [" + types.size() + "] resource types");
                }
            });
    }

    private void handleError(int statusCode) {
        if (statusCode == 401) {
            form.setFieldErrors("login", "The username or password provided does not match our records", true);
        } else {
            form.setFieldErrors("login", "The backend data source is unavailable", true);
        }
        loginButton.setDisabled(false);
    }
}
