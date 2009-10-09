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

package org.rhq.plugins.antlrconfig;

/**
 * A class representing an element in the Antlr tree query.
 * 
 * @author Lukas Krejci
 */
public class PathElement {

    public enum Type {
        /**
         * The path element represents a (set of) token(s) addressed by their type name.
         */
        NAME_REFERENCE,
        
        /**
         * The path element represents a token addressed by its type name and type relative position.
         */
        POSITION_REFERENCE,
        
        /**
         * The path element represents a (set of) token(s) addressed by their type name and value.
         */
        VALUE_REFERENCE,
        
        /**
         * The path element represents a token addressed by an absolute index among its siblings in a tree.
         */
        INDEX_REFERENCE
    }
    
    private Type type;
    private String tokenTypeName;
    private int absoluteTokenPosition = -1;
    private int typeRelativeTokenPosition = -1;
    private String tokenText;
    
    public Type getType() {
        return type;
    }
    
    public void setType(Type type) {
        this.type = type;
    }
    
    public String getTokenTypeName() {
        return tokenTypeName;
    }
    
    public void setTokenTypeName(String tokenName) {
        this.tokenTypeName = tokenName;
    }
    
    public int getAbsoluteTokenPosition() {
        return absoluteTokenPosition;
    }
    
    public void setAbsoluteTokenPosition(int tokenPosition) {
        this.absoluteTokenPosition = tokenPosition;
    }
    
    public int getTypeRelativeTokenPosition() {
        return typeRelativeTokenPosition;
    }

    public void setTypeRelativeTokenPosition(int typeRelativeTokenPosition) {
        this.typeRelativeTokenPosition = typeRelativeTokenPosition;
    }

    public String getTokenText() {
        return tokenText;
    }
    
    public void setTokenText(String tokenText) {
        this.tokenText = unescape(tokenText);
    }
    
    public String toString() {
        StringBuilder bld = new StringBuilder();
        
        switch(type) {
        case INDEX_REFERENCE:
            bld.append("$").append(absoluteTokenPosition);
            break;
        case NAME_REFERENCE:
            bld.append(tokenTypeName);
            break;
        case POSITION_REFERENCE:
            bld.append(tokenTypeName).append("[").append(typeRelativeTokenPosition).append("]");
            break;
        case VALUE_REFERENCE:
            bld.append(tokenTypeName).append("[=\"").append(escape(tokenText)).append("\"]");
            break;
        }        
        
        return bld.toString();
    }

    private String unescape(String string) {
        if (string != null) {
            return string.replace("\\\"", "\"").replace("\\\n", "\n").replace("\\\t", "\t")
                .replace("\\\\", "\\");
        }
        return null;
    }

    private String escape(String string) {
        if (string != null) {
            return string.replace("\"", "\\\"").replace("\n", "\\\n").replace("\t", "\\\t")
            .replace("\\", "\\\\");
        }
        return null;
    }
}
