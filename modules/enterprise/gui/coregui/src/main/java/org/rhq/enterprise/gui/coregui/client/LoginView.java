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

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.FormErrorOrientation;
import com.smartgwt.client.types.VerticalAlignment;
import com.smartgwt.client.widgets.HTMLFlow;
import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.Img;
import com.smartgwt.client.widgets.Label;
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

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.type.ResourceTypeRepository;
import org.rhq.enterprise.gui.coregui.client.util.BrowserUtility;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableCanvas;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableDynamicForm;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableIButton;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableWindow;

/**
 * @author Greg Hinkle
 * @author Joseph Marques
 */
public class LoginView extends LocatableCanvas {

    private static boolean loginShowing = false;

    private LocatableWindow window;
    private LocatableDynamicForm form;
    private LocatableDynamicForm inputForm;

    private SubmitItem loginButton;

    public LoginView(String locatorId) {
        super(locatorId);
    }

    //registration fields
    private TextItem first;
    private TextItem last;
    private TextItem email;
    private TextItem phone;
    private TextItem department;
    private static final String FIRST = "first";
    private static final String LAST = "last";
    static final String USERNAME = "ldap.username";
    private static final String EMAIL = "email";
    private static final String PHONE = "phone";
    private static final String DEPARTMENT = "department";
    private static final String SESSIONID = "ldap.sessionid";
    static final String PASSWORD = "ldap.password";

    public void showLoginDialog(String message) {
        showLoginDialog();
        form.setErrorsPreamble(message);
    }

    public void showLoginDialog() {
        if (!loginShowing) {
            loginShowing = true;
            UserSessionManager.logout();

            form = new LocatableDynamicForm(extendLocatorId("LoginView"));
            form.setMargin(25);
            form.setAutoFocus(true);
            form.setShowErrorText(true);
            form.setErrorOrientation(FormErrorOrientation.BOTTOM);

            CanvasItem logo = new CanvasItem();
            logo.setCanvas(new Img("header/rhq_logo_28px.png", 80, 28));
            logo.setShowTitle(false);

            HeaderItem header = new HeaderItem();
            header.setValue(MSG.view_login_prompt());

            TextItem user = new TextItem("user", MSG.common_title_user());
            user.setRequired(true);
            user.setAttribute("autoComplete", "native");

            final PasswordItem password = new PasswordItem("password", MSG.dataSource_users_field_password());
            password.setRequired(true);
            password.setAttribute("autoComplete", "native");

            loginButton = new SubmitItem("login", MSG.view_login_login());
            loginButton.setAlign(Alignment.CENTER);
            loginButton.setColSpan(2);

            user.addKeyPressHandler(new KeyPressHandler() {
                public void onKeyPress(KeyPressEvent event) {
                    if ((event.getCharacterValue() != null)
                        && (((event.getCharacterValue() == KeyCodes.KEY_ENTER)) || (event.getCharacterValue() == KeyCodes.KEY_TAB))) {
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

            window = new LocatableWindow(extendLocatorId("LoginWindow"));
            window.setWidth(400);
            window.setHeight(275);
            window.setTitle(MSG.common_title_welcome());

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
    public void showRegistrationDialog(final String user, final String sessionId, final String password,
        final AsyncCallback<Subject> callback) {
        if (!loginShowing) {
            loginShowing = true;

            int fieldWidth = 120;

            LocatableVLayout column = new LocatableVLayout(extendLocatorId("NewLdapRegistration"));
            column.setMargin(25);
            HeaderItem header = new HeaderItem();
            header.setValue(MSG.view_login_welcomeMsg());
            header.setWidth("100%");
            //build ui elements for registration screen
            first = new TextItem(FIRST, MSG.dataSource_users_field_firstName());
            {
                first.setRequired(true);
                first.setWrapTitle(false);
                first.setWidth(fieldWidth);
            }
            last = new TextItem(LAST, MSG.dataSource_users_field_lastName());
            {
                last.setWrapTitle(false);
                last.setWidth(fieldWidth);
                last.setRequired(true);
            }
            final TextItem username = new TextItem(USERNAME, MSG.dataSource_users_field_name());
            {
                username.setValue(user);
                username.setDisabled(true);
                username.setWidth(fieldWidth);
            }
            email = new TextItem(EMAIL, MSG.dataSource_users_field_emailAddress());
            email.setRequired(true);
            email.setWidth(fieldWidth);
            email.setWrapTitle(false);
            phone = new TextItem(PHONE, MSG.dataSource_users_field_phoneNumber());
            phone.setWidth(fieldWidth);
            phone.setWrapTitle(false);
            department = new TextItem(DEPARTMENT, MSG.dataSource_users_field_department());
            department.setWidth(fieldWidth);
            SpacerItem space = new SpacerItem();
            space.setColSpan(1);

            inputForm = new LocatableDynamicForm(extendLocatorId("LdapUserRegistrationInput"));
            inputForm.setAutoFocus(true);
            inputForm.setErrorOrientation(FormErrorOrientation.LEFT);
            inputForm.setNumCols(4);
            //moving header to it's own container for proper display. Didn't display right in production mode
            inputForm.setFields(username, first, last, email, phone, department);
            loadValidators(inputForm);
            inputForm.setValidateOnExit(true);
            DynamicForm headerWrapper = new DynamicForm();
            headerWrapper.setFields(header);
            column.addMember(headerWrapper);
            column.addMember(inputForm);

            HTMLFlow hr = new HTMLFlow("<br/><hr/><br/><br/>");
            hr.setWidth(620);
            hr.setAlign(Alignment.CENTER);
            column.addMember(hr);

            HStack row = new HStack();
            row.setMembersMargin(5);
            row.setAlign(VerticalAlignment.CENTER);
            IButton okButton = new LocatableIButton(inputForm.extendLocatorId("OK"), MSG.common_button_ok());
            okButton.addClickHandler(new ClickHandler() {
                public void onClick(ClickEvent event) {

                    //F5 refresh check? If they've reloaded the form for some reason then bail.
                    boolean credentialsEmpty = ((user == null) || (user.trim().isEmpty()) || (password == null) || (password
                        .trim().isEmpty()));
                    //check for session timeout
                    if (UserSessionManager.isLoggedOut() || (credentialsEmpty)) {
                        resetLogin(LoginView.this.extendLocatorId("Register"));
                        return;
                    }

                    //validation
                    if (inputForm.validate()) {
                        Log.trace("Successfully validated all data for user registration.");
                        //populate form
                        if (first.getValue() != null)
                            inputForm.setValue(FIRST, String.valueOf(first.getValue()));
                        if (last.getValue() != null)
                            inputForm.setValue(LAST, String.valueOf(last.getValue()));
                        inputForm.setValue(USERNAME, String.valueOf(username.getValue()));
                        if (email.getValue() != null)
                            inputForm.setValue(EMAIL, String.valueOf(email.getValue()));
                        if (phone.getValue() != null)
                            inputForm.setValue(PHONE, String.valueOf(phone.getValue()));
                        if (department.getValue() != null)
                            inputForm.setValue(DEPARTMENT, String.valueOf(department.getValue()));
                        inputForm.setValue(SESSIONID, sessionId);
                        inputForm.setValue(PASSWORD, password);
                        registerLdapUser(LoginView.this.extendLocatorId("RegisterLdap"), inputForm, callback);
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
                    inputForm.setFieldErrors(FIRST, MSG.view_login_noLdap(), true);
                    Log
                        .debug("Optional LDAP detail retrieval did not succeed. Registration prepopulation will not occur.");
                }
            });

            IButton resetButton = new LocatableIButton(inputForm.extendLocatorId("Reset"), MSG.common_button_reset());
            resetButton.addClickHandler(new ClickHandler() {
                public void onClick(ClickEvent event) {
                    //F5 refresh check? If they've reloaded the form for some reason then bail.
                    boolean credentialsEmpty = ((user == null) || (user.trim().isEmpty()) || (password == null) || (password
                        .trim().isEmpty()));
                    if (UserSessionManager.isLoggedOut() || credentialsEmpty) {
                        resetLogin(LoginView.this.extendLocatorId("Reset"));
                        return;
                    }

                    //clear out all validation messages.
                    {
                        String empty = "                  ";
                        first.setValue(empty);
                        last.setValue(empty);
                        email.setValue("test@test.com");
                        inputForm.validate();
                    }
                    first.clearValue();
                    last.clearValue();
                    email.clearValue();
                    phone.clearValue();
                    department.clearValue();
                }
            });
            row.addMember(resetButton);

            IButton cancelButton = new LocatableIButton(inputForm.extendLocatorId("Cancel"), MSG.common_button_cancel());
            cancelButton.addClickHandler(new ClickHandler() {
                public void onClick(ClickEvent event) {
                    UserSessionManager.logout();
                    resetLogin(LoginView.this.extendLocatorId("Reset"));
                }
            });
            row.addMember(cancelButton);
            Label logoutLabel = new Label(MSG.view_login_registerLater());
            logoutLabel.setWrap(false);
            row.addMember(logoutLabel);
            column.addMember(row);

            window = new LocatableWindow(extendLocatorId("RegistrationWindow"));
            window.setWidth(670);
            window.setHeight(330);
            window.setTitle(MSG.view_login_registerUser());

            // forced focused, static size, can't close / dismiss
            window.setIsModal(true);
            window.setShowModalMask(true);
            window.setCanDragResize(false);
            window.setCanDragReposition(false);
            window.setShowCloseButton(false);
            window.setShowMinimizeButton(false);
            window.setAutoCenter(true);

            window.addItem(column);
            window.show();
        }
    }

    /** Go through steps of invalidating this login and piping them back to CoreGUI Login.
     */
    private void resetLogin(String locatorId) {
        window.destroy();
        loginShowing = false;
        UserSessionManager.logout();
        new LoginView(locatorId).showLoginDialog();
    }

    /**Uses the information from the populated form to create the Subject for the new LDAP user.
     * 
     * @param populatedForm - validated data
     * @param callback
     */
    protected void registerLdapUser(final String locatorId, DynamicForm populatedForm,
        final AsyncCallback<Subject> callback) {

        final Subject newSubject = new Subject();
        newSubject.setId(0);//enforce registration element for LDAP processing

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
            Log.trace("New LDAP user registration details valid for user '" + newSubject.getName() + "'.");
            //proceed with LDAP processing request.
            GWTServiceLookup.getSubjectService().processSubjectForLdap(newSubject, password,
                new AsyncCallback<Subject>() {
                    public void onFailure(Throwable caught) {
                        Log.debug("Failed to register LDAP subject '" + newSubject.getName() + "' "
                            + caught.getMessage());
                        //TODO: pass in warning message to Login Dialog.
                        new LoginView(locatorId).showLoginDialog();
                    }

                    public void onSuccess(Subject checked) {
                        Log.trace("Successfully registered LDAP subject '" + checked + "'.");

                        CoreGUI.getMessageCenter().notify(
                            new Message(MSG.view_login_registerLdapSuccess(), Message.Severity.Info));
                        Log.trace("Successfully registered the new ldap Subject.");
                        window.destroy();
                        loginShowing = false;
                        //indicate to login callback success
                        callback.onSuccess(checked);
                    }
                });

        } else {//log them out then reload LoginView
            Log.warn("Failed to locate required components to create LDAP subject.");
            UserSessionManager.logout();
            window.destroy();
            loginShowing = false;
            //TODO: pass informative message to login.
            new LoginView(locatorId).showLoginDialog();
        }
    }

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
                        emailValidator.setErrorMessage(MSG.view_login_invalidEmail());
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
            RequestBuilder b = new RequestBuilder(RequestBuilder.POST, "/j_security_check.do");
            b.setHeader("Content-Type", "application/x-www-form-urlencoded");
            b.setRequestData("j_username=" + user + "&j_password=" + password);
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
        ResourceTypeRepository.Cache.getInstance().getResourceTypes(null,
            EnumSet.allOf(ResourceTypeRepository.MetadataType.class), new ResourceTypeRepository.TypesLoadedCallback() {
                public void onTypesLoaded(Map<Integer, ResourceType> types) {
                    Log.info("Preloaded [" + types.size() + "] resource types");
                }
            });
    }

    private void handleError(int statusCode) {
        if (statusCode == 401) {
            form.setFieldErrors("login", MSG.view_login_noUser(), true);
        } else {
            form.setFieldErrors("login", MSG.view_login_noBackend(), true);
        }
        loginButton.setDisabled(false);
    }

}
