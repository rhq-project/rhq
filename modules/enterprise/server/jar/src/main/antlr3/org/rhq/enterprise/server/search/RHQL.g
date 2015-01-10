/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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
    output=AST;
    ASTLabelType=CommonTree;
}

/*
 * Imaginary nodes serve to fully abstract the parse tree from the AST.  This
 * allows us, for example, to support the conjunctive ('and') operator between
 * conditional expressions, but gives us the flexibility to modify the lexical
 * element that represents that operation.  In the future, we might want to
 * support '&&' instead of 'and', or even support them both.  The inclusion of
 * imaginary tokens creates a more stable AST that downstream grammars can use
 * without fear that every change to the syntax will break their tree parser.
 */
tokens {
    OR;
    AND;

    CONTEXT;
    LINEAGE;
    PATH;
    PARAM;
    IDENT;
    VALUE;
    
    OP_EQUALS;
    OP_EQUALS_STRICT;
    
    OP_NOT_EQUALS;
    OP_NOT_EQUALS_STRICT;
    
    OP_LESS_THAN;
    OP_GREATER_THAN;
}

@header {
    package org.rhq.enterprise.server.search;
}
@lexer::header {
    package org.rhq.enterprise.server.search;
}

@parser::members {
  @Override
  public void reportError(RecognitionException e) {
    throw new org.rhq.enterprise.server.search.SearchExpressionException("search pattern error");
  }
}


/* 
 * parser rules 
 */
 
searchExpression
    :   conditionalExpression
    ;

conditionalExpression
    :   conds+=conditionalFactor ( WS+ ( '|' ) WS+ conds+=conditionalFactor )*        -> { $conds.size() == 1 }? {$conds.get(0)}
                                                                                      -> ^(OR conditionalFactor+)
    ; // use rewrite predicates to eliminate superfluous 'or' node if only one child

conditionalFactor 
    :   conds+=conditionalPrimary ( WS+ conds+=conditionalPrimary )*                  -> { $conds.size() == 1 }? {$conds.get(0)}
                                                                                      -> ^(AND conditionalPrimary+)
    ; // use rewrite predicates to eliminate superfluous 'and' node if only one child

conditionalPrimary
    :   WS* simpleConditionalExpression                                               -> simpleConditionalExpression
    |   '(' WS* conditionalExpression WS* ')'                                         -> conditionalExpression
    ; // avoid building nodes for parens, tree structure implies existence appropriately -- ignore captured WS

simpleConditionalExpression
    :   c=context WS* op=comparisonOperator WS* ident=identifier                      -> ^($op $c ^(VALUE $ident))
    |   ident=identifier
    ; // rewrite tree output so operator is always the root -- ignore captured WS

context
    :  ( l=lineage '.' )? p=path ( '[' ident=parameter ']' )?                         -> ^(CONTEXT ^(LINEAGE $l)? ^(PATH $p) ^(PARAM $ident)?)
    ;

lineage
    :   path ( '['! LEVEL ']'! )?
    ; // avoid building nodes for brackets, tree structure implies existence appropriately

path
    :   ( 'or' | ID )+
    ; // path is any character or token...

parameter
    :   doubleQuotedValue -> ^(IDENT doubleQuotedValue)
    |   quotedValue       -> ^(IDENT quotedValue)
    |   boundedValue      -> ^(IDENT boundedValue)
    ;

identifier
    :   doubleQuotedValue -> ^(IDENT doubleQuotedValue)
    |   quotedValue       -> ^(IDENT quotedValue)
    |   openEndedvalue    -> ^(IDENT openEndedvalue)
    ;

doubleQuotedValue
    :   '"'! ~('"')* '"'!
    ; // avoid building nodes for the double-quote characters

    
quotedValue
    :   '\''! ~('\'')* '\''!
    ; // avoid building nodes for the sinngle-quote characters

boundedValue
    :   ~( ']' | WS )*
    ; // consume until we find a whitespace char or ']' to terminate the current phrase
    
openEndedvalue
    :   ~( '|' ) ~( '(' | ')' | WS )*
    ; // consume until we find a whitespace char to ')' terminate the current phrase, or '(' begin the next phrase

comparisonOperator  
    :   '='   -> ^(OP_EQUALS)
    |   '=='  -> ^(OP_EQUALS_STRICT)
    |   '!='  -> ^(OP_NOT_EQUALS)
    |   '!==' -> ^(OP_NOT_EQUALS_STRICT)
    |   '<'   -> ^(OP_LESS_THAN)
    |   '>'   -> ^(OP_GREATER_THAN)
    ; // use imaginary nodes for all operators, which further removes the AST from the real lexical elements

/* 
 * lexical elements 
 */ 

ID
    :   'a'..'z' | 'A'..'Z'
    ;

LEVEL
    :   '0'..'9'
    ;

SYMBOL
    :   '!' | '@' | '#' | '$' | '%' | '^' | '&' | '*' | '-' | '_' | '+' | '|' | '?' | '/' | ',' | '<' | '>' | '`' | '~' | ':'
    ;

WS
    :   ( ' ' | '\n' | '\r' )+
    ;
