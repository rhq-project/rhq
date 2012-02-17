/*
 * RHQ Management Platform
 * Copyright (C) 2012 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.client;

/**
 * If a view X is in the process of rendering itself and realizes that the user has navigated to view Y, it should
 * throw this exception, rather than unnecessarily continuing to render itself.
 *
 * @author Ian Springer
 */
public class ViewChangedException extends RuntimeException {

    private String obsoleteView;

    public ViewChangedException() {
    }

    public ViewChangedException(String obsoleteView) {
        super();
        this.obsoleteView = (obsoleteView != null) ? obsoleteView : "?";
    }

    public String getObsoleteView() {
        return obsoleteView;
    }

}
