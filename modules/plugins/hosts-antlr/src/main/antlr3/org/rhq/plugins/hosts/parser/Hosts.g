grammar Hosts;

options {
  language = Java;
  output = AST;
  ASTLabelType = CommonTree;
}

//Each subtree in the generated AST must have a token type assigned to it
//these are "virtual" tokens (without any underlying parseable text assigned to them)
//that we need to add to support that behavior in the rules below
tokens {
  FILE;
  HOST_DEF;
  ALIASES;
}

@header {
  package org.rhq.plugins.hosts.parser;
  
  //these are required so that the parser complies;
  import org.rhq.plugins.antlrconfig.tokens.RhqConfigToken;
  
  //<Unordered> and <Id> token types (applied in the rules below)
  //aren't implemented yet.
  //The idea behind them is to support the scenario that the config file
  //updates are robust enough to transparently handle the reordering of
  //config elements that doesn't change the "meaning" of the configuration.
  import org.rhq.plugins.antlrconfig.tokens.Unordered;
  import org.rhq.plugins.antlrconfig.tokens.Id;
  import org.rhq.plugins.antlrconfig.tokens.Transparent;
}

@lexer::header {
  package org.rhq.plugins.hosts.parser;
}

fragment
LETTER :
  'a'..'z' |
  'A'..'Z' ;
  
fragment
DIGIT :
  '0'..'9';
  
fragment  
BYTE_NUM : ('0'..'9') |  ('0'..'9' '0'..'9') | ('0'..'1' '0'..'9' '0'..'9') | ('2' '0'..'4' '0'..'9') | ('2' '5' '0'..'5') ;

fragment
HEXA_DIGIT : DIGIT | 'a'..'f' | 'A'..'F' ;

fragment
IP6_FRAGMENT : HEXA_DIGIT | (HEXA_DIGIT HEXA_DIGIT) | (HEXA_DIGIT HEXA_DIGIT HEXA_DIGIT) | (HEXA_DIGIT HEXA_DIGIT HEXA_DIGIT HEXA_DIGIT) ;

fragment
HOST_NAME_PART : LETTER (LETTER | DIGIT | '-')* ;

fragment
DOT : '.' ;

//$channel = HIDDEN will make the WS and comments disappear from the parser input
//even though they will be still preserved in the input stream and output
WS :
  (' ' | '\t' | '\f' | '\n')+ {$channel=HIDDEN;};
  
SINGLE_LINE_COMMENT :
  '#' (~('\n'|'\r'))* ('\n'|'\r'('\n')?)? {$channel=HIDDEN;};

IP4_ADDRESS : BYTE_NUM DOT BYTE_NUM DOT BYTE_NUM DOT BYTE_NUM ;

//this is brutal, I know, but I wasn't able to come with anything simpler to capture the correct
//format of an IP6 address
IP6_ADDRESS : 
  ('::' IP6_FRAGMENT) |
  ('::' IP6_FRAGMENT ':' IP6_FRAGMENT) |
  ('::' IP6_FRAGMENT ':' IP6_FRAGMENT ':' IP6_FRAGMENT) |      
  ('::' IP6_FRAGMENT ':' IP6_FRAGMENT ':' IP6_FRAGMENT ':' IP6_FRAGMENT) |
  ('::' IP6_FRAGMENT ':' IP6_FRAGMENT ':' IP6_FRAGMENT ':' IP6_FRAGMENT ':' IP6_FRAGMENT) |
  ('::' IP6_FRAGMENT ':' IP6_FRAGMENT ':' IP6_FRAGMENT ':' IP6_FRAGMENT ':' IP6_FRAGMENT ':' IP6_FRAGMENT) |
  ('::' IP6_FRAGMENT ':' IP6_FRAGMENT ':' IP6_FRAGMENT ':' IP6_FRAGMENT ':' IP6_FRAGMENT ':' IP6_FRAGMENT ':' IP6_FRAGMENT) |
  (IP6_FRAGMENT '::' IP6_FRAGMENT) |
  (IP6_FRAGMENT '::' IP6_FRAGMENT ':' IP6_FRAGMENT) |
  (IP6_FRAGMENT '::' IP6_FRAGMENT ':' IP6_FRAGMENT ':' IP6_FRAGMENT) |
  (IP6_FRAGMENT '::' IP6_FRAGMENT ':' IP6_FRAGMENT ':' IP6_FRAGMENT ':' IP6_FRAGMENT) |
  (IP6_FRAGMENT '::' IP6_FRAGMENT ':' IP6_FRAGMENT ':' IP6_FRAGMENT ':' IP6_FRAGMENT ':' IP6_FRAGMENT) |
  (IP6_FRAGMENT '::' IP6_FRAGMENT ':' IP6_FRAGMENT ':' IP6_FRAGMENT ':' IP6_FRAGMENT ':' IP6_FRAGMENT ':' IP6_FRAGMENT) |
  (IP6_FRAGMENT ':' IP6_FRAGMENT '::' IP6_FRAGMENT) |
  (IP6_FRAGMENT ':' IP6_FRAGMENT '::' IP6_FRAGMENT ':' IP6_FRAGMENT) |
  (IP6_FRAGMENT ':' IP6_FRAGMENT '::' IP6_FRAGMENT ':' IP6_FRAGMENT ':' IP6_FRAGMENT) |
  (IP6_FRAGMENT ':' IP6_FRAGMENT '::' IP6_FRAGMENT ':' IP6_FRAGMENT ':' IP6_FRAGMENT ':' IP6_FRAGMENT) |
  (IP6_FRAGMENT ':' IP6_FRAGMENT '::' IP6_FRAGMENT ':' IP6_FRAGMENT ':' IP6_FRAGMENT ':' IP6_FRAGMENT ':' IP6_FRAGMENT) |
  (IP6_FRAGMENT ':' IP6_FRAGMENT ':' IP6_FRAGMENT '::' IP6_FRAGMENT) |
  (IP6_FRAGMENT ':' IP6_FRAGMENT ':' IP6_FRAGMENT '::' IP6_FRAGMENT ':' IP6_FRAGMENT) |
  (IP6_FRAGMENT ':' IP6_FRAGMENT ':' IP6_FRAGMENT '::' IP6_FRAGMENT ':' IP6_FRAGMENT ':' IP6_FRAGMENT) |
  (IP6_FRAGMENT ':' IP6_FRAGMENT ':' IP6_FRAGMENT '::' IP6_FRAGMENT ':' IP6_FRAGMENT ':' IP6_FRAGMENT ':' IP6_FRAGMENT) |
  (IP6_FRAGMENT ':' IP6_FRAGMENT ':' IP6_FRAGMENT ':' IP6_FRAGMENT '::' IP6_FRAGMENT) |
  (IP6_FRAGMENT ':' IP6_FRAGMENT ':' IP6_FRAGMENT ':' IP6_FRAGMENT '::' IP6_FRAGMENT ':' IP6_FRAGMENT) |
  (IP6_FRAGMENT ':' IP6_FRAGMENT ':' IP6_FRAGMENT ':' IP6_FRAGMENT '::' IP6_FRAGMENT ':' IP6_FRAGMENT ':' IP6_FRAGMENT) |
  (IP6_FRAGMENT ':' IP6_FRAGMENT ':' IP6_FRAGMENT ':' IP6_FRAGMENT ':' IP6_FRAGMENT '::' IP6_FRAGMENT) |
  (IP6_FRAGMENT ':' IP6_FRAGMENT ':' IP6_FRAGMENT ':' IP6_FRAGMENT ':' IP6_FRAGMENT '::' IP6_FRAGMENT ':' IP6_FRAGMENT) |
  (IP6_FRAGMENT ':' IP6_FRAGMENT ':' IP6_FRAGMENT ':' IP6_FRAGMENT ':' IP6_FRAGMENT ':' IP6_FRAGMENT '::' IP6_FRAGMENT) |
  (IP6_FRAGMENT ':' IP6_FRAGMENT ':' IP6_FRAGMENT ':' IP6_FRAGMENT ':' IP6_FRAGMENT ':' IP6_FRAGMENT ':' IP6_FRAGMENT ':' IP6_FRAGMENT) ;
  
HOSTNAME : HOST_NAME_PART (DOT HOST_NAME_PART)* ;

file 
  : line* -> ^(FILE<Unordered> line*)
  | EOF!
  ;

line 
  : address HOSTNAME aliases 
    -> ^(HOST_DEF address HOSTNAME aliases)
  ;

address
  : IP4_ADDRESS -> ^(IP4_ADDRESS<Id>)
  | IP6_ADDRESS -> ^(IP6_ADDRESS<Id>)
  ;
  
aliases
  : HOSTNAME* 
    -> ^(ALIASES<Unordered> ^(HOSTNAME<Id>)*)
  ;

    