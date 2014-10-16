/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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
package org.rhq.coregui.client;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.InputElement;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;
import com.google.gwt.http.client.URL;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FormPanel;
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
import com.smartgwt.client.widgets.layout.HStack;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.common.ProductInfo;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.coregui.client.gwt.GWTServiceLookup;
import org.rhq.coregui.client.inventory.resource.type.ResourceTypeRepository;
import org.rhq.coregui.client.util.Log;
import org.rhq.coregui.client.util.enhanced.EnhancedIButton;
import org.rhq.coregui.client.util.enhanced.EnhancedVLayout;
import org.rhq.coregui.client.util.message.Message;

/**
 * @author Greg Hinkle
 * @author Joseph Marques
 */
public class LoginView extends Canvas {

    private static boolean loginShowing = false;

    private static final Messages MSG = CoreGUI.getMessages();

    private static final String LOGIN_VIEW = "login";

    private Window window;
    private FormPanel fakeForm;
    private DynamicForm form;
    private DynamicForm inputForm;

    private SubmitItem loginButton;

    // registration fields
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

    // html login form
    private static final String LOGINFORM_ID = "loginForm";
    private static final String LOGINBUTTON_ID = "loginSubmit";
    private static final String USERNAME_ID = "inputUsername";
    private static final String PASSWORD_ID = "inputPassword";
    private static final String LOGIN_DIV_ID = "patternFlyLogin";
    private static final String LOGIN_ERROR_DIV_ID = "loginError";
    private static final String ERROR_FEEDBACK_DIV_ID = "errorFeedback";
    private static final String HTML_ID = "htmlId";
    private String errorMessage;
    private static volatile boolean isLoginView = true;

    private ProductInfo productInfo;

    public void showLoginDialog(String message) {
        if (!loginShowing) {
            errorMessage = message;
            if (!isLoginView()) {
                redirectTo(LOGIN_VIEW);
                return;
            }
            showLoginDialog(false);
        } else {
            form.setErrorsPreamble(message);
            setLoginError(message);
        }
    }

    public void showLoginDialog(boolean isLogout) {
        if (!loginShowing) {
            if (isLogout) {
                UserSessionManager.logout();
            }
            if (!isLoginView()) {
                redirectTo(LOGIN_VIEW);
                return;
            }
            isLoginView = true;
            loginShowing = true;
            form = new DynamicForm();
            form.setMargin(25);
            form.setAutoFocus(true);
            form.setShowErrorText(true);
            form.setErrorOrientation(FormErrorOrientation.BOTTOM);

            // NOTE: This image will either be an RHQ logo or a JON logo.
            //       but must be 80x40
            Img logoImg = new Img("header/rhq_logo_40px.png", 80, 40);

            CanvasItem logo = new CanvasItem();
            logo.setShowTitle(false);
            logo.setCanvas(logoImg);

            HeaderItem header = new HeaderItem();
            header.setValue(MSG.view_login_prompt());

            TextItem user = new TextItem("user", MSG.common_title_user());
            user.setRequired(true);
            user.setAttribute("autoComplete", "native");

            final PasswordItem password = new PasswordItem("password", MSG.common_title_password());
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

            window = new Window();
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

            form.addSubmitValuesHandler(new SubmitValuesHandler() {
                public void onSubmitValues(SubmitValuesEvent submitValuesEvent) {
                    if (form.validate()) {
                        setUsername(form.getValueAsString("user"));
                        setPassword(form.getValueAsString("password"));
                        fakeForm.submit();
                    }
                }
            });

            // Get a handle to the form and set its action to __gwt_login() method
            fakeForm = FormPanel.wrap(Document.get().getElementById(LOGINFORM_ID), false);
            fakeForm.setVisible(true);
            fakeForm.setAction("javascript:__gwt_login()");
            // export the JSNI function
            injectLoginFunction(this);

            if (errorMessage != null) {
                form.setFieldErrors("login", MSG.view_login_noUser(), true);
                errorMessage = null; // hide it next time
            }
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

            //BZ:784873. To fix issue with users logging in by LDAP integration with clean browser cache.
            if (CoreGUI.get().getProductInfo() == null) {
                //We need to explicitly retrieve product info here as can't count on CoreGui to load it
                //during LDAP registration. After registration CoreGui is loaded as usual.
                GWTServiceLookup.getSystemService().getProductInfo(new AsyncCallback<ProductInfo>() {
                    public void onFailure(Throwable caught) {
                        CoreGUI.getErrorHandler().handleError(MSG.view_aboutBox_failedToLoad(), caught);
                        productInfo = null;
                        Log.warn("ProductInfo could not be retrieved for some reason. Proceeding anyway.");
                        buildRegistrationWindow(user, sessionId, password, callback);
                    }

                    public void onSuccess(ProductInfo result) {
                        productInfo = result;
                        Log.info("ProductInfo has been retrieved for LDAP registration.");
                        buildRegistrationWindow(user, sessionId, password, callback);
                    }
                });
            } else {//if productInfo has already been loaded, save a gwt call.
                productInfo = CoreGUI.get().getProductInfo();
                buildRegistrationWindow(user, sessionId, password, callback);
            }
        }
    }

    /** Duplicate modal Login mechanism to now show last registration screen before launching
     *  core gui.
     *
     * @param user prepopulate username field for LDAP registration
     * @param sessionId pass in valid session id for LDAP registration steps.
     * @param callback pass in callback reference to indicate success and launch of coreGUI
     */
    private void buildRegistrationWindow(final String user, final String sessionId, final String password,
        final AsyncCallback<Subject> callback) {
        int fieldWidth = 120;

        //Build registration window.
        EnhancedVLayout column = new EnhancedVLayout();
        column.setMargin(25);
        HeaderItem header = new HeaderItem();
        //Locate product info for registration screen.
        if (productInfo != null) {
            header.setValue(MSG.view_login_welcomeMsg(productInfo.getName()));
        } else {//if not available, let registration continue. Errors already logged and no functionality lost.
            header.setValue(MSG.view_login_welcomeMsg(""));
        }
        header.setWidth("100%");
        //build ui elements for registration screen
        first = new TextItem(FIRST, MSG.dataSource_users_field_firstName());
        first.setRequired(true);
        first.setWrapTitle(false);
        first.setWidth(fieldWidth);
        last = new TextItem(LAST, MSG.dataSource_users_field_lastName());
        last.setWrapTitle(false);
        last.setWidth(fieldWidth);
        last.setRequired(true);
        final TextItem username = new TextItem(USERNAME, MSG.common_title_username());
        username.setValue(user);
        username.setDisabled(true);
        username.setWidth(fieldWidth);
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

        inputForm = new DynamicForm();
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
        IButton okButton = new EnhancedIButton(MSG.common_button_ok());
        okButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {

                //F5 refresh check? If they've reloaded the form for some reason then bail.
                boolean credentialsEmpty = ((user == null) || (user.trim().isEmpty()) || (password == null) || (password
                    .trim().isEmpty()));
                //check for session timeout
                if (UserSessionManager.isLoggedOut() || (credentialsEmpty)) {
                    resetLogin();
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
                    registerLdapUser(inputForm, callback);
                }
            }

        });
        row.addMember(okButton);

        //prepopulate form from user details returned.
        Subject subject = UserSessionManager.getSessionSubject();
        first.setValue(subject.getFirstName());
        last.setValue(subject.getLastName());
        email.setValue(subject.getEmailAddress());
        phone.setValue(subject.getPhoneNumber());
        department.setValue(subject.getDepartment());

        IButton resetButton = new EnhancedIButton(MSG.common_button_reset());
        resetButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                //F5 refresh check? If they've reloaded the form for some reason then bail.
                boolean credentialsEmpty = ((user == null) || (user.trim().isEmpty()) || (password == null) || (password
                    .trim().isEmpty()));
                if (UserSessionManager.isLoggedOut() || credentialsEmpty) {
                    resetLogin();
                    return;
                }

                //clear out all validation messages.
                String empty = "                  ";
                first.setValue(empty);
                last.setValue(empty);
                email.setValue("test@test.com");
                inputForm.validate();
                first.clearValue();
                last.clearValue();
                email.clearValue();
                phone.clearValue();
                department.clearValue();
            }
        });
        row.addMember(resetButton);

        IButton cancelButton = new EnhancedIButton(MSG.common_button_cancel());
        cancelButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                UserSessionManager.logout();
                resetLogin();
            }
        });
        row.addMember(cancelButton);
        Label logoutLabel = new Label(MSG.view_login_registerLater());
        logoutLabel.setWrap(false);
        row.addMember(logoutLabel);
        column.addMember(row);

        window = new Window();
        window.setWidth(670);
        window.setHeight(370);
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

    /** Go through steps of invalidating this login and piping them back to CoreGUI Login.
     */
    private void resetLogin() {
        window.destroy();
        loginShowing = false;
        showLoginDialog(true);
    }

    /**Uses the information from the populated form to create the Subject for the new LDAP user.
     *
     * @param populatedForm - validated data
     * @param callback
     */
    protected void registerLdapUser(final DynamicForm populatedForm,
        final AsyncCallback<Subject> callback) {

        final Subject newSubject = UserSessionManager.getSessionSubject();

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

        if (proceed) {
            Log.trace("New LDAP user registration details valid for user '" + newSubject.getName() + "'.");
            //proceed with LDAP processing request.
            //clear out 'isNewUser' flag.
            if (newSubject.getUserConfiguration() != null) {
                PropertySimple simple = new PropertySimple("isNewUser", null);
                newSubject.getUserConfiguration().put(simple);
            }

            Set<String> prefsChanges = new HashSet<String>();
            prefsChanges.add("isNewUser");

            GWTServiceLookup.getSubjectService().updateSubjectAndPreferences(newSubject, prefsChanges, new AsyncCallback<Subject>() {
                public void onFailure(Throwable caught) {
                    Log.error("Failed to register LDAP subject '" + newSubject.getName() + "' " + caught.getMessage(),
                        caught);
                    //TODO: pass in warning message to Login Dialog.
                        showLoginDialog(false);
                }

                public void onSuccess(Subject checked) {
                    Log.info("Successfully registered LDAP subject '" + checked + "'.");
                    checked.setSessionId(Integer.valueOf(populatedForm.getValueAsString(SESSIONID)));

                    CoreGUI.getMessageCenter().notify(
                        new Message(MSG.view_login_registerLdapSuccess(), Message.Severity.Info));
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
            showLoginDialog(true);
        }
    }

    private void loadValidators(DynamicForm form) {
        if (form != null) {
            for (FormItem item : form.getFields()) {
                String name = item.getName();
                if ((name != null) && (!name.isEmpty())) {
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

    private void login(final String username, final String password) {

        loginButton.setDisabled(true);

        try {
            RequestBuilder requestBuilder = new RequestBuilder(RequestBuilder.POST, "/portal/j_security_check.do");
            requestBuilder.setHeader("Content-Type", "application/x-www-form-urlencoded");
            // URL-encode the username and password in case they contain URL special characters ('?', '&', '%', '+',
            // etc.), which would corrupt the request if not encoded.
            String encodedUsername = URL.encodeQueryString(username);
            String encodedPassword = URL.encodeQueryString(password);
            String requestData = "j_username=" + encodedUsername + "&j_password=" + encodedPassword;
            requestBuilder.setRequestData(requestData);
            requestBuilder.setCallback(new RequestCallback() {
                public void onResponseReceived(Request request, Response response) {
                    int statusCode = response.getStatusCode();
                    if (statusCode == 200) {
                        window.destroy();
                        fakeForm.setVisible(false);
                        loginShowing = false;
                        UserSessionManager.login(username, password);
                        setLoginError(null);
                    } else {
                        handleError(statusCode);
                    }
                }

                public void onError(Request request, Throwable exception) {
                    handleError(0);
                }
            });
            requestBuilder.send();
        } catch (Exception e) {
            handleError(0);
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
            setLoginError(MSG.view_login_noUser());
        } else if (statusCode == 503) {
            form.setFieldErrors("login", MSG.view_core_serverInitializing(), true);
            setLoginError(MSG.view_core_serverInitializing());
        } else {
            form.setFieldErrors("login", MSG.view_login_noBackend(), true);
            setLoginError(MSG.view_login_noBackend());
        }
        loginButton.setDisabled(false);
    }

    /**
     * Call this method to find out if the login dialog is shown
     * @return true if it is shown
     */
    public static boolean isLoginShowing() {
        return loginShowing;
    }

    private void doSubmitForm() {
        form.submit();
    }

    // called from EcmaScript method (__gwt_login)
    private void doLogin() {
        login(getUsername(), getPassword());
    }

    private String getPassword() {
        return ((InputElement) Document.get().getElementById(PASSWORD_ID)).getValue();
    }

    private String getUsername() {
        return ((InputElement) Document.get().getElementById(USERNAME_ID)).getValue();
    }

    private void setPassword(String password) {
        ((InputElement) Document.get().getElementById(PASSWORD_ID)).setValue(password);
    }

    private void setUsername(String username) {
        ((InputElement) Document.get().getElementById(USERNAME_ID)).setValue(username);
    }

    private void setLoginError(String error) {
        Element errorDiv = DOM.getElementById(LOGIN_ERROR_DIV_ID);
        Element feedbackDiv = DOM.getElementById(ERROR_FEEDBACK_DIV_ID);
        if (errorDiv != null && feedbackDiv != null) {
            errorDiv.setInnerHTML(error);
            feedbackDiv.setClassName(error != null ? "showError" : "hideError");
        }
    }

    // This is our JSNI method that will be called on form submit
    private native void injectLoginFunction(LoginView view) /*-{
      $wnd.__gwt_login = $entry(function(){
        view.@org.rhq.coregui.client.LoginView::doLogin()();
      });
    }-*/;

    public static boolean isLoginView() {
        return isLoginView && com.google.gwt.user.client.Window.Location.getHref().contains(LOGIN_VIEW);
    }

    public static void redirectTo(final String path) {
        new Timer() {
            @Override
            public void run() {
                if (path != null && !("/coregui/" + path).equals(com.google.gwt.user.client.Window.Location.getPath())) {
                    if (path.isEmpty()) {
                        isLoginView = false;
                    }
                    String destinationFullPath = GWT.getHostPageBaseURL() + path
                        + com.google.gwt.user.client.Window.Location.getQueryString()
                        + com.google.gwt.user.client.Window.Location.getHash();
                    Log.info("redirecting to " + destinationFullPath);
                    com.google.gwt.user.client.Window.Location.assign(destinationFullPath);
                }
            }
        }.schedule(20);
    }
}
