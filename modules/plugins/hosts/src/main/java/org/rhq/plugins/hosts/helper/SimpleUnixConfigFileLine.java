/*
 * JBoss, a division of Red Hat.
 * Copyright 2006, Red Hat Middleware, LLC. All rights reserved.
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
