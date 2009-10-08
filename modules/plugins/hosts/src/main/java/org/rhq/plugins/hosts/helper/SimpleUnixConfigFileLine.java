/*
 * RHQ Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
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
package org.rhq.plugins.hosts.helper;

import org.jetbrains.annotations.Nullable;

/**
 * A line in a simple Unix config file, which consists of an optional non-comment portion and an optional comment
 * portion.
 *
 * @author Ian Springer
 */
public class SimpleUnixConfigFileLine {
    private String nonComment;
    private String comment;

    public SimpleUnixConfigFileLine(@Nullable String nonComment, @Nullable String comment) {
        this.nonComment = nonComment;
        this.comment = comment;
    }

    @Nullable
    public String getNonComment() {
        return this.nonComment;
    }

    @Nullable
    public String getComment() {
        return this.comment;
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        if (this.nonComment != null) {
            stringBuilder.append(this.nonComment);
            if (this.comment != null) {
                stringBuilder.append(" ");
            }
        }
        if (this.comment != null) {
            stringBuilder.append("#").append(this.comment);
        }
        return stringBuilder.toString();
    }
}
