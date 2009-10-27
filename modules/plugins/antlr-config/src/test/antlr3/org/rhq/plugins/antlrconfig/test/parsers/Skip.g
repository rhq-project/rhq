grammar Skip;

options {
  language = Java;
  output = AST;
}

tokens {
  FILE;
  SEGMENT;
  ASSIGNMENT;
}

@header {
  package org.rhq.plugins.antlrconfig.test.parsers;
  
  import org.rhq.plugins.antlrconfig.tokens.*;
}

@lexer::header {
  package org.rhq.plugins.antlrconfig.test.parsers;
}

fragment
LETTER : 'a'..'z' | 'A'..'Z' ;

fragment
DIGIT : '0'..'9' ;

SEGMENT : 's' 'e' 'g' 'm' 'e' 'n' 't' ;
 
WORD : LETTER (LETTER | DIGIT)* ;

EQUALS : '=' ;

VALUE : DIGIT+;

WS : (' ' | '\t')+ {$channel = HIDDEN;} ;

NL : '\r'? '\n' ;

file : (segment | NL)* -> ^(FILE<Unordered> segment*) ;

segment : SEGMENT '[' NL assignment* ']' (NL | EOF) -> ^(SEGMENT<Skip> assignment*) ;

assignment : WORD EQUALS VALUE NL -> ^(ASSIGNMENT ^(WORD<Id>) VALUE);
