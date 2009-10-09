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
        NAME_REFERENCE,
        POSITION_REFERENCE,
        VALUE_REFERENCE,
        INDEX_REFERENCE
    }
    
    private Type type;
    private String tokenName;
    private int tokenPosition = -1;
    private String tokenText;
    
    public Type getType() {
        return type;
    }
    
    public void setType(Type type) {
        this.type = type;
    }
    
    public String getTokenName() {
        return tokenName;
    }
    
    public void setTokenName(String tokenName) {
        this.tokenName = tokenName;
    }
    
    public int getTokenPosition() {
        return tokenPosition;
    }
    
    public void setTokenPosition(int tokenPosition) {
        this.tokenPosition = tokenPosition;
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
            bld.append("$").append(tokenPosition);
            break;
        case NAME_REFERENCE:
            bld.append(tokenName);
            break;
        case POSITION_REFERENCE:
            bld.append(tokenName).append("[").append(tokenPosition).append("]");
            break;
        case VALUE_REFERENCE:
            bld.append(tokenName).append("[=\"").append(escape(tokenText)).append("\"]");
            break;
        }        
        
        return bld.toString();
    }

    private String unescape(String string) {
        return string.replace("\\\"", "\"").replace("\\\n", "\n").replace("\\\t", "\t")
            .replace("\\\\", "\\");
    }

    private String escape(String string) {
        return string.replace("\"", "\\\"").replace("\n", "\\\n").replace("\t", "\\\t")
        .replace("\\", "\\\\");
    }
}
