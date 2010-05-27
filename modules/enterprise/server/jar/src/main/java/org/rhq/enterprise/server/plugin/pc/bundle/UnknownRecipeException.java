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

package org.rhq.enterprise.server.plugin.pc.bundle;

/**
 * This exception is thrown when a bundle server plugin has been asked to parse a recipe but
 * that recipe is not one that is recognized by the plugin. For example, if a plugin accepts
 * an Ant script as its recipe, this exception should be thrown if the given recipe is
 * not an Ant XML file in the first place. A different exception should be thrown if the
 * recipe was an actual Ant script, but it had a syntax error or some other error that the
 * Ant parser found and reported.
 * 
 * @author John Mazzitelli
 */
public class UnknownRecipeException extends Exception {

    private static final long serialVersionUID = 1L;

    public UnknownRecipeException() {
        super();
    }

    public UnknownRecipeException(String message) {
        super(message);
    }

    public UnknownRecipeException(Throwable cause) {
        super(cause);
    }

    public UnknownRecipeException(String message, Throwable cause) {
        super(message, cause);
    }
}
