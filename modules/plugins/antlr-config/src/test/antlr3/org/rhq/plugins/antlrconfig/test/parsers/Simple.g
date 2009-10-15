grammar Simple;

options {
  language = Java;
  output = AST;
}

tokens {
  FILE;
  ASSIGNMENT;
}

@header {
  package org.rhq.plugins.antlrconfig.test.parsers;
  
  import org.rhq.plugins.antlrconfig.tokens.*;
}

@lexer::header {
  package org.rhq.plugins.antlrconfig.test.parsers;
}

LETTER : 'a'..'z' | 'A'..'Z';
DIGIT : '0'..'9';

EXPORT : 'e' 'x' 'p' 'o' 'r' 't';

WORD : LETTER (LETTER | DIGIT)*;

WS : (' ' | '\t')+ {$channel = HIDDEN;};

NL : '\r'? '\n';

file : assignment* -> ^(FILE<Unordered> assignment*) 
     | EOF!
     ; 

assignment : EXPORT? WORD '=' WORD NL -> ^(ASSIGNMENT WORD<Id> WORD EXPORT?) ;
