/*
 * RHQ Management Platform
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

/*
 * Antlr v3 grammar file to parse RHQL search expressions.
 *
 * @author Joseph Marques
 */
grammar RHQL;

options {
    language=Java;
    backtrack=true;
    memoize=true;
}

@header {
    package org.rhq.enterprise.server.search;
}
@lexer::header {
    package org.rhq.enterprise.server.search;
}

/* 
 * parser rules 
 */
 
searchExpression
    :   conditionalExpression
    ;

conditionalExpression
    :   conditionalFactor ( 'or' conditionalFactor )*
    ;

conditionalFactor 
    :   conditionalPrimary ( ( 'and' )? conditionalPrimary )*
    ;

conditionalPrimary
    :   simpleConditionalExpression
    |   '(' conditionalExpression ')'
    ;

simpleConditionalExpression
    :   comparisonConditionalExpression
    |   nullComparisonConditionalExpression
    |   inExpression
    ;

comparisonConditionalExpression
    :   context comparisonOperator identifier
    ;

nullComparisonConditionalExpression
    :   context nullOperator
    ;

inExpression
    :   context inOperator '[' '='? identifier ( ',' '='? identifier )* ']'
    ;

context
    :  ( lineage '.' )? path ( '[' identifier ']' )?
    ;

lineage
    :   path ( '(' INT ')' )?
    ;

path
    :   ID+
    ;

identifier
    :   quotedValue
    |   value
    ;

quotedValue
    :   '\'' ~('\'')* '\''
    ;

value
    :   ~('\'') ~(']'|','|')')*
    ;

comparisonOperator  
    :   '=' 
    |   '==' 
    |   '!=' 
    |   '!==' 
    ;

nullOperator 
    :   'is' 'not'? 'null'
    ;

inOperator 
    :   'not'? 'in'
    ;

/* 
 * lexical elements 
 */
 
ID
    :   'a'..'z'
    ;

INT
    :   '0'..'9'
    ;

WS
    :   ( ' ' | '\n' | '\r' )+ { $channel = HIDDEN; }
    ;