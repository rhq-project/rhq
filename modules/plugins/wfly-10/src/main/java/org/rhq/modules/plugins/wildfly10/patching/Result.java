/*
 * RHQ Management Platform
 * Copyright (C) 2014 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package org.rhq.modules.plugins.wildfly10.patching;

/**
* @author Lukas Krejci
* @since 4.13
*/
final class Result<T> {
    final T result;
    final String errorMessage;

    public Result(T result, String errorMessage) {
        this.result = result;
        this.errorMessage = errorMessage;
    }

    static <T> Result<T> with(T result) {
        return new Result<T>(result, null);
    }

    static <T> Result<T> error(String errorMessage) {
        return new Result<T>(null, errorMessage);
    }

    boolean failed() {
        return errorMessage != null;
    }
}
