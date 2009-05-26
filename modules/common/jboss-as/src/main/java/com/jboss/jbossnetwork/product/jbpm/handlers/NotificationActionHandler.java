 /*
  * Jopr Management Platform
  * Copyright (C) 2005-2008 Red Hat, Inc.
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
package com.jboss.jbossnetwork.product.jbpm.handlers;

import org.jbpm.graph.exe.ExecutionContext;

/**
 * JBPM action handler that simply holds a message from the workflow.
 *
 * @author Jason Dobies
 */
public class NotificationActionHandler extends BaseHandler {
    /**
     * The notification message that should make up this action's description.
     */
    private String notification;

    public void run(ExecutionContext executionContext) {
        notRun(executionContext, notification);
    }

    public String getDescription() {
        return notification;
    }

    protected void checkProperties() throws ActionHandlerException {
        HandlerUtils.checkIsSet("notification", notification);
    }

    public void substituteVariables(ExecutionContext executionContext) throws ActionHandlerException {
        setNotification(substituteVariable(notification, executionContext));
    }

    public String getNotification() {
        return notification;
    }

    public void setNotification(String notification) {
        this.notification = notification;
    }
}