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
    OP_NULL;
    OP_IN;
    
    OP_NOT_EQUALS;
    OP_NOT_EQUALS_STRICT;
    OP_NOT_NULL;
    OP_NOT_IN;
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
    :   conditionalExpression { System.out.println($conditionalExpression.tree.toStringTree()); }
    ;

conditionalExpression
    :   conds+=conditionalFactor ( WS* ( 'or' ) WS* conds+=conditionalFactor )*     -> { $conds.size() == 1 }? ^($conds)
                                                                                    -> ^(OR conditionalFactor+)
    ; // use rewrite predicates to eliminate superfluous 'or' node if only one child

conditionalFactor 
    :   conds+=conditionalPrimary ( WS* ( 'and' WS* )? conds+=conditionalPrimary )* -> { $conds.size() == 1 }? ^($conds)
                                                                                    -> ^(AND conditionalPrimary+)
    ; // use rewrite predicates to eliminate superfluous 'and' node if only one child

conditionalPrimary
    :   WS* simpleConditionalExpression WS*                                         -> simpleConditionalExpression
    |   '(' WS* conditionalExpression WS* ')'                                       -> conditionalExpression
    ; // avoid building nodes for parens, tree structure implies existence appropriately -- ignore captured WS

simpleConditionalExpression
    :   comparisonConditionalExpression
    |   nullComparisonConditionalExpression
    |   inExpression
    ;

comparisonConditionalExpression
    :   c=context WS* op=comparisonOperator WS* ident=identifier                 -> ^($op $c ^(VALUE $ident))
    ; // rewrite tree output so operator is always the root -- ignore captured WS

nullComparisonConditionalExpression
    :   c=context WS* op=nullOperator                                            -> ^($op $c)
    ; // rewrite tree output so operator is always the root -- ignore captured WS

inExpression
    :   c=context WS* op=inOperator WS* '[' WS* ids+=identifier WS* ( ',' WS* ids+=identifier WS* )* ']' 
                                                                                 -> ^($op $c ^(VALUE $ids+))
    ; // rewrite tree output so operator is always the root -- ignore captured WS

context
    :  ( l=lineage '.' )? p=path ( '[' ident=identifier ']' )?                   -> ^(CONTEXT ^(LINEAGE $l)? ^(PATH $p) ^(PARAM $ident)?)
    ;

lineage
    :   path ( '('! LEVEL ')'! )?
    ; // avoid building nodes for brackets, tree structure implies existence appropriately

path
    :   ID+
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

openEndedvalue
    :   ~(']' | ',' | ')' | '(' | 'or' | 'and' | WS )*
    ; // consume until we find a char to terminate the current phrase ']' ',' ')' or begin the next '(' 'or' 'and'

comparisonOperator  
    :   '='   -> ^(OP_EQUALS)
    |   '=='  -> ^(OP_EQUALS_STRICT)
    |   '!='  -> ^(OP_NOT_EQUALS)
    |   '!==' -> ^(OP_NOT_EQUALS_STRICT)
    ; // use imaginary nodes for all operators, which further removes the AST from the real lexical elements

nullOperator 
    :   'is' WS+ (negation='not' WS+)? 'null' -> { $negation == null }? ^(OP_NULL)
                                              -> ^(OP_NOT_NULL)
    ; // use imaginary nodes for all operators, which further removes the AST from the real lexical elements

inOperator 
    :   (negation+='not' WS+)? 'in'       -> { $negation == null }? ^(OP_IN)
                                          -> ^(OP_NOT_IN)
    ; // use imaginary nodes for all operators, which further removes the AST from the real lexical elements

/* 
 * lexical elements 
 */ 
 
ID
    :   'a'..'z'
    ;

LEVEL
    :   '0'..'5'
    ;

WS
    :   ( ' ' | '\n' | '\r' )+
    ;