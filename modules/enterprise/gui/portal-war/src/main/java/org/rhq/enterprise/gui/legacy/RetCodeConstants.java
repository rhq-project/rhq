/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.enterprise.gui.legacy;

/**
 * Struts return codes
 */

public interface RetCodeConstants {
    //---------------------------------------URLs

    /**
     * The symbolic name of the <code>ActionForward</code> to which control is transferred when the user cancels a form
     * workflow.
     */
    public static final String CANCEL_URL = "cancel";

    /**
     * The symbolic name of the <code>ActionForward</code> to which control is transferred when the user resets a form
     */
    public static final String RESET_URL = "reset";

    /**
     * The symbolic name of the <code>ActionForward</code> to which control is transferred when a form workflow
     * completes abnormally.
     */
    public static final String FAILURE_URL = "failure";

    /**
     * The symbolic name of the <code>ActionForward</code> to which control is transferred when a form workflow
     * completes successfully.
     */
    public static final String SUCCESS_URL = "success";

    /**
     * The symbolic name of the <code>ActionForward</code> to which control is transferred when user has clicked on the
     * okassign button
     */
    public static final String OK_ASSIGN_URL = "okassign";

    /**
     * The symbolic name of the <code>ActionForward</code> to which control is transferred when an new button is clicked
     * to include a "new object" workflow into the current workflow
     */
    public static final String NEW_URL = "new";

    /**
     * The symbolic name of the <code>ActionForward</code> to which control is transferred when an edit button is
     * clicked to submit to a non-edit action.
     */
    public static final String EDIT_URL = "edit";

    /**
     * The symbolic name of the <code>ActionForward</code> to which control is transferred when an "add to list" button
     * is clicked.
     */
    public static final String ADD_URL = "add";

    /**
     * The symbolic name of the <code>ActionForward</code> to which control is transferred when an "remove to list"
     * button is clicked.
     */
    public static final String REMOVE_URL = "remove";

    /**
     * The symbolic name of the <code>ActionForward</code> to which control is transferred when an "remove to list"
     * button is clicked.
     */
    public static final String INSTALL_URL = "install";

    /**
     * The symbolic name of the <code>ActionForward</code> to which control is transferred when an "remove to list"
     * button is clicked.
     */
    public static final String MANUAL_UNINSTALL_URL = "manualUninstall";

    /**
     * The symbolic name of the <code>ActionForward</code> to which control is transferred when a "compare metrics of
     * selected services" button is clicked.
     */
    public static final String COMPARE_URL = "compare";

    /**
     * The symbolic name of the <code>ActionForward</code> to which control is transferred when a "chart selected
     * metrics" button is clicked.
     */
    public static final String CHART_URL = "chart";

    /**
     * The symbolic name of the <code>ActionForward</code> to which control is transferred when an "edit range..."
     * button is clicked.
     */
    public static final String EDIT_RANGE_URL = "editRange";

    /**
     * The symbolic name of the <code>ActionForward</code> to which control is transferred when a "Redraw ..." button is
     * clicked.
     */
    public static final String REDRAW_URL = "redraw";

    /**
     * The symbolic name of the <code>ActionForward</code> to which control is transferred when a "Return to ..." link
     * is clicked.
     */
    public static final String BACK_URL = "back";

    /**
     * The symbolic name of the <code>ActionForward</code> to which control is transferred when the user cancels a form
     * workflow.
     */
    public static final String SCHEDULE_TYPE_CHG_URL = "scheduleTypeChange";

    /**
     * The symbolic name of the <code>ActionForward</code> to which control is transferred when a RSS feed is requested
     */
    public static final String RSS_URL = "rss.feed";

    /**
     * The symbolic name of the <code>ActionForward</code> to which control is transferred when response is AJAX
     */
    public static final String AJAX_URL = "ajax";
}