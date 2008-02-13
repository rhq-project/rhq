package org.rhq.enterprise.gui.authentication;

import javax.servlet.http.HttpServletRequest;

import org.apache.struts.action.ActionMapping;
import org.apache.struts.validator.ValidatorForm;

/**
 * Form bean for the user profile page.  This form has the following fields,
 * with default values in square brackets:
 * <ul>
 * <li><b>password</b> - Entered password value
 * <li><b>username</b> - Entered username value
 * </ul>
 */

public final class LogonForm extends ValidatorForm {

    // --------------------------------------------------- Instance Variables

    /**
     * The password.
     */
    private String j_password = null;

    /**
     * The username.
     */
    private String j_username = null;

    // ----------------------------------------------------------- Properties

    /**
     * Return the password.
     */
    public String getJ_password() {

        return (this.j_password);

    }

    /**
     * Set the password.
     *
     * @param password The new password
     */
    public void setJ_password(String j_password) {
        this.j_password = j_password;
    }

    /**
     * Return the username.
     */
    public String getJ_username() {

        return (this.j_username);

    }

    /**
     * Set the username.
     *
     * @param username The new username
     */
    public void setJ_username(String j_username) {
        this.j_username = j_username;
    }

    // --------------------------------------------------------- Public Methods

    /**
     * Reset all properties to their default values.
     *
     * @param mapping The mapping used to select this instance
     * @param request The servlet request we are processing
     */
    public void reset(ActionMapping mapping, HttpServletRequest request) {

        this.j_password = null;
        this.j_username = null;

    }

}
