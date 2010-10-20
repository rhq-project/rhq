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

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Map;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;
import com.google.gwt.user.client.Cookies;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.FormErrorOrientation;
import com.smartgwt.client.types.VerticalAlignment;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.HTMLFlow;
import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.Img;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.Window;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.events.SubmitValuesEvent;
import com.smartgwt.client.widgets.form.events.SubmitValuesHandler;
import com.smartgwt.client.widgets.form.fields.CanvasItem;
import com.smartgwt.client.widgets.form.fields.FormItem;
import com.smartgwt.client.widgets.form.fields.HeaderItem;
import com.smartgwt.client.widgets.form.fields.PasswordItem;
import com.smartgwt.client.widgets.form.fields.RowSpacerItem;
import com.smartgwt.client.widgets.form.fields.SpacerItem;
import com.smartgwt.client.widgets.form.fields.SubmitItem;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.form.fields.events.KeyPressEvent;
import com.smartgwt.client.widgets.form.fields.events.KeyPressHandler;
import com.smartgwt.client.widgets.form.validator.LengthRangeValidator;
import com.smartgwt.client.widgets.form.validator.RegExpValidator;
import com.smartgwt.client.widgets.layout.HStack;
import com.smartgwt.client.widgets.layout.VLayout;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.type.ResourceTypeRepository;
import org.rhq.enterprise.gui.coregui.client.util.BrowserUtility;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;

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

    //registration fields
    private TextItem first;
    private TextItem last;
    private TextItem email;
    private TextItem phone;
    private TextItem department;
    private ArrayList<DynamicForm> forms;
    private static final String FIRST = "first";
    private static final String LAST = "last";
    private static final String USERNAME = "ldap.username";
    private static final String EMAIL = "email";
    private static final String PHONE = "phone";
    private static final String DEPARTMENT = "department";
    private static final String SESSIONID = "ldap.sessionid";
    private static final String PASSWORD = "ldap.password";

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

    /** Duplicate modal Login mechanism to now show last registration screen before launching 
     *  core gui.
     * 
     * @param user prepopulate username field for LDAP registration
     * @param sessionId pass in valid session id for LDAP registration steps.
     * @param callback pass in callback reference to indicate success and launch of coreGUI
     */
    public void showRegistrationDialog(String user, final String sessionId, final String password,
        final AsyncCallback<Subject> callback) {

        if (!loginShowing) {
            if ((user != null) && (!user.trim().isEmpty())) {
                Cookies.setCookie(USERNAME, user);
                Cookies.setCookie(PASSWORD, password);
            }
            loginShowing = true;

            forms = new ArrayList<DynamicForm>();

            form = new DynamicForm();
            form.setMargin(25);
            form.setAutoFocus(true);
            form.setShowErrorText(true);
            form.setErrorOrientation(FormErrorOrientation.BOTTOM);
            int fieldWidth = 120;

            VLayout column = new VLayout();
            HeaderItem header = new HeaderItem();
            header
                .setValue("Welcome to JBoss ON! <br/><br/> Enter/update the following fields to complete your registration process."
                    + "<br/> Once you click \"OK\" you will be logged in.<br/><br/>");
            column.addMember(wrapInDynamicForm(1, header));
            //build ui elements for registration screen
            first = new TextItem(FIRST, "First Name");
            {
                first.setRequired(true);
                first.setWrapTitle(false);
                first.setWidth(fieldWidth);
            }
            last = new TextItem(LAST, "Last Name");
            {
                last.setWrapTitle(false);
                last.setWidth(fieldWidth);
                last.setRequired(true);
            }
            final TextItem username = new TextItem(USERNAME, "Username");
            {
                username.setValue(Cookies.getCookie(USERNAME));

                username.setDisabled(true);
                username.setWidth(fieldWidth);
                //column.addMember(wrapInDynamicForm(6, first, last, username));
            }
            email = new TextItem(EMAIL, "Email");
            email.setRequired(true);
            email.setWidth(fieldWidth);
            phone = new TextItem(PHONE, "Phone");
            phone.setWidth(fieldWidth);
            department = new TextItem(DEPARTMENT, "Department");
            department.setWidth(fieldWidth);
            SpacerItem space = new SpacerItem();
            space.setColSpan(1);
            column.addMember(wrapInDynamicForm(6, first, last, username, email, phone, department));
            HTMLFlow hr = new HTMLFlow("<br/><hr/><br/><br/>");
            hr.setWidth(750);
            hr.setAlign(Alignment.CENTER);
            column.addMember(hr);

            HStack row = new HStack();
            row.setMembersMargin(5);
            row.setAlign(VerticalAlignment.CENTER);
            IButton okButton = new IButton("OK");
            okButton.addClickHandler(new ClickHandler() {
                public void onClick(ClickEvent event) {
                    //check for session timeout
                    if (UserSessionManager.isLoggedOut()) {
                        resetLogin();
                    }

                    //validation
                    if (validateForms(forms)) {
                        Log.trace("Successfully validated all data for user registration.");
                        //populate form
                        form.setValue(FIRST, String.valueOf(first.getValue()));
                        form.setValue(LAST, String.valueOf(last.getValue()));
                        form.setValue(USERNAME, String.valueOf(username.getValue()));
                        form.setValue(EMAIL, String.valueOf(email.getValue()));
                        form.setValue(PHONE, String.valueOf(phone.getValue()));
                        form.setValue(DEPARTMENT, String.valueOf(department.getValue()));
                        form.setValue(SESSIONID, sessionId);
                        form.setValue(PASSWORD, Cookies.getCookie(PASSWORD));
                        registerLdapUser(form, callback);
                    }
                }

            });
            row.addMember(okButton);
            //send request to LDAP server to grab user details for this user. Already sure ldap user exists
            GWTServiceLookup.getLdapService().getLdapDetailsFor(user, new AsyncCallback<Map<String, String>>() {
                public void onSuccess(final Map<String, String> ldapUserDetails) {
                    //now prepopulate UI fields if they exist
                    for (String key : ldapUserDetails.keySet()) {
                        String value;
                        if (key.equalsIgnoreCase("givenName")) {//aka first name
                            value = ldapUserDetails.get(key);
                            first.setValue(value);
                        } else if (key.equalsIgnoreCase("sn")) {//aka Surname
                            value = ldapUserDetails.get(key);
                            if ((value != null) && (!value.isEmpty())) {
                                last.setValue(value);
                            }
                        } else if (key.equalsIgnoreCase("telephoneNumber")) {
                            value = ldapUserDetails.get(key);
                            if ((value != null) && (!value.isEmpty())) {
                                phone.setValue(value);
                            }
                        } else if (key.equalsIgnoreCase("mail")) {
                            value = ldapUserDetails.get(key);
                            if ((value != null) && (!value.isEmpty())) {
                                email.setValue(value);
                            }
                        }
                    }
                }

                public void onFailure(Throwable caught) {
                    Log.debug("Optional LDAP detail retrieval did not succeed. Registration prepopulation will occur.");
                }
            });

            IButton resetButton = new IButton("Reset");
            resetButton.addClickHandler(new ClickHandler() {
                public void onClick(ClickEvent event) {
                    if (UserSessionManager.isLoggedOut()) {
                        resetLogin();
                    }

                    //clear out all validation messages.
                    {
                        String empty = "                  ";
                        first.setValue(empty);
                        last.setValue(empty);
                        email.setValue("test@test.com");
                        validateForms(forms);
                    }
                    first.clearValue();
                    last.clearValue();
                    email.clearValue();
                    phone.clearValue();
                    department.clearValue();
                }
            });
            row.addMember(resetButton);

            IButton logout = new IButton("Logout");
            logout.addClickHandler(new ClickHandler() {
                public void onClick(ClickEvent event) {
                    UserSessionManager.logout();
                    resetLogin();
                }
            });
            row.addMember(logout);
            Label logoutLabel = new Label("(Logout - Complete registration later.)");
            logoutLabel.setWrap(false);
            row.addMember(logoutLabel);
            column.addMember(row);
            form.addChild(column);

            window = new Window();
            window.setWidth(800);
            window.setHeight(300);
            window.setTitle("Register User");

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
        }
    }

    /** Iterates through the dynamic forms populated then calls validate().
     * 
     * @param forms
     * @return
     */
    private boolean validateForms(ArrayList<DynamicForm> forms) {
        boolean allValid = true;
        for (DynamicForm form : forms) {
            if (!form.validate()) {
                allValid = false;
            }
        }
        return allValid;
    }

    /** Go through steps of invalidating this login and piping them back to CoreGUI Login.
     */
    private void resetLogin() {
        window.destroy();
        loginShowing = false;
        new LoginView().showLoginDialog();
    }

    /**Uses the information from the populated form to create the Subject for the new LDAP user.
     * 
     * @param populatedForm - validated data
     * @param callback
     */
    protected void registerLdapUser(DynamicForm populatedForm, final AsyncCallback<Subject> callback) {
        final Subject newSubject = new Subject();

        //insert some required data checking
        boolean proceed = true;
        String retrieved = populatedForm.getValueAsString(USERNAME);
        if ((retrieved == null) || retrieved.isEmpty() || retrieved.equalsIgnoreCase("null")) {
            proceed = false;
        }
        retrieved = populatedForm.getValueAsString(SESSIONID);
        if ((retrieved == null) || retrieved.isEmpty() || retrieved.equalsIgnoreCase("null")) {
            proceed = false;
        }
        retrieved = populatedForm.getValueAsString(PASSWORD);
        if ((retrieved == null) || retrieved.isEmpty() || retrieved.equalsIgnoreCase("null")) {
            proceed = false;
        }

        newSubject.setName(populatedForm.getValueAsString(USERNAME));
        newSubject.setSessionId(Integer.valueOf(populatedForm.getValueAsString(SESSIONID)));
        String password = populatedForm.getValueAsString(PASSWORD);

        //don't load null values not set or returned from ldap server
        retrieved = populatedForm.getValueAsString(FIRST);
        if ((retrieved != null) && (!retrieved.equalsIgnoreCase("null")))
            newSubject.setFirstName(populatedForm.getValueAsString(FIRST));
        retrieved = populatedForm.getValueAsString(LAST);
        if ((retrieved != null) && (!retrieved.equalsIgnoreCase("null")))
            newSubject.setLastName(populatedForm.getValueAsString(LAST));

        retrieved = populatedForm.getValueAsString(DEPARTMENT);
        if ((retrieved != null) && (!retrieved.equalsIgnoreCase("null")))
            newSubject.setDepartment(populatedForm.getValueAsString(DEPARTMENT));

        retrieved = populatedForm.getValueAsString(EMAIL);
        if ((retrieved != null) && (!retrieved.equalsIgnoreCase("null")))
            newSubject.setEmailAddress(populatedForm.getValueAsString(EMAIL));

        retrieved = populatedForm.getValueAsString(PHONE);
        if ((retrieved != null) && (!retrieved.equalsIgnoreCase("null")))
            newSubject.setPhoneNumber(populatedForm.getValueAsString(PHONE));

        //        newSubject.setSmsAddress(populatedForm.getValueAsString("sms"));
        newSubject.setFactive(true);
        newSubject.setFsystem(false);

        if (proceed) {
            GWTServiceLookup.getSubjectService().createSubjectUsingOverlord(newSubject, password,
                new AsyncCallback<Subject>() {
                    public void onSuccess(final Subject newLoggedInSubject) {
                        CoreGUI.getMessageCenter().notify(
                            new Message("Succesfully created new ldap Subject.", Message.Severity.Info));
                        Log.trace("New subject created for ldap user.");
                        //now do group role assignment for initial login
                        GWTServiceLookup.getLdapService().updateLdapGroupAssignmentsForSubject(newLoggedInSubject,
                            new AsyncCallback<Void>() {
                                public void onFailure(Throwable caught) {
                                    CoreGUI.getErrorHandler().handleError("Failed to assign roles for ldap Subject.",
                                        caught);
                                    Log.debug("Failed to assign roles to ldap subject.");
                                }

                                public void onSuccess(Void result) {
                                    CoreGUI.getMessageCenter().notify(
                                        new Message("Succesfully assigned roles for ldap Subject.",
                                            Message.Severity.Info));
                                    Log.trace("Role assignment update for ldap subject complete.");
                                    window.destroy();
                                    loginShowing = false;
                                    callback.onSuccess(newLoggedInSubject);
                                }
                            });
                    }

                    public void onFailure(Throwable caught) {
                        CoreGUI.getErrorHandler().handleError("Failed to create ldap Subject.", caught);
                    }
                });
        } else {//log them out then reload LoginView
            Log.warn("Failed to locate username required to create LDAP subject.");
            UserSessionManager.logout();
            new LoginView().showLoginDialog();
        }
    }

    /**Helper method to wrap N form items one a single line/row represented by a DynamicForm
     * 
     * @param columnCount 
     * @param header
     * @return
     */
    private Canvas wrapInDynamicForm(int columnCount, FormItem... header) {
        DynamicForm form = new DynamicForm();
        if (header != null) {
            if (columnCount < 1) {//default to label and details for each form item
                form.setNumCols(header.length * 2);
            } else {
                form.setNumCols(columnCount);
            }
            form.setFields(header);
            //store away all forms for final validation
            forms.add(form);
            //load validators for form
            loadValidators(form);
        }
        return form;
    }

    /**Build and loads the validators for each of the formItems
     * 
     * @param form
     */
    private void loadValidators(DynamicForm form) {
        if (form != null) {
            for (FormItem item : form.getFields()) {
                String name = item.getName();
                if ((name != null) && (!name.isEmpty())) {
                    if (name.equals(USERNAME)) {
                        LengthRangeValidator validator = new LengthRangeValidator();
                        validator.setMin(6);
                        item.setValidators(validator);
                    }
                    if (name.equals(FIRST)) {
                        LengthRangeValidator validator = new LengthRangeValidator();
                        validator.setMin(1);
                        item.setValidators(validator);
                    }
                    if (name.equals(EMAIL)) {
                        RegExpValidator emailValidator = new RegExpValidator();
                        emailValidator.setErrorMessage("Invalid email address");
                        emailValidator.setExpression("^([a-zA-Z0-9_.\\-+])+@(([a-zA-Z0-9\\-])+\\.)+[a-zA-Z0-9]{2,4}$");
                        item.setValidators(emailValidator);
                    }
                }
            }
        }
    }

    private void login(final String user, final String password) {
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
                        UserSessionManager.login(user, password);
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
                    Log.info("Preloaded [" + types.size() + "] resource types");
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
