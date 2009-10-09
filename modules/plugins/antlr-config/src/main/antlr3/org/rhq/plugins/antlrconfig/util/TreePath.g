grammar TreePath;

options {
  language = Java;
  output = AST;
}

tokens {
  PATH;
  ELEMENT;
  POSITION_DISCRIMINATOR;
  VALUE_DISCRIMINATOR;
  CHILD_POSITIONAL_REFERENCE;
}

@header {
  package org.rhq.core.antlr;
  
  import java.util.ArrayList;
}

@lexer::header {
  package org.rhq.core.antlr;
}

fragment
LETTER : 'a'..'z' | 'A'..'Z' ;

fragment
DIGIT : '0'..'9' ;

DOLLAR : '$' ;
PATH_SEPARATOR : '/' ;
QUOTE : '"' ;

PATH_ELEMENT : LETTER (LETTER | DIGIT | '-' | '_')* ;
NUMBER : DIGIT+ ;

OPEN_BRACKET : '[' ;
CLOSE_BRACKET : ']' ;
EQUALS : '=' ;

fragment
STRING_ESCAPE_SEQUENCE : '\\' (QUOTE | 'n' | 't' | '\\') ;

fragment
STRING_CONTENTS : (~('\\' | '\n' | '\r' | QUOTE))* (STRING_ESCAPE_SEQUENCE STRING_CONTENTS)? ;  

STRING : '"' STRING_CONTENTS '"' ;

CHILD_REFERENCE : DOLLAR NUMBER ;

path returns [List<PathElement> elements]
@init {
  $elements = new ArrayList<PathElement>();
} 
  : PATH_SEPARATOR p1=pathElement {
      $elements.add($p1.pathElement);
    }(PATH_SEPARATOR p2=pathElement{
      $elements.add($p2.pathElement);
    })*
    -> ^(PATH pathElement+)
  | EOF!
  ;


pathElement returns [PathElement pathElement] throws NumberFormatException
@init {
  $pathElement = new PathElement();
}
  : el=PATH_ELEMENT {
      $pathElement.setTokenName($el.text);
      $pathElement.setType(PathElement.Type.NAME_REFERENCE);
    } (OPEN_BRACKET discriminator {
      if ($discriminator.value != null) {
        //TODO implement
        $pathElement.setType(PathElement.Type.VALUE_REFERENCE);
        $pathElement.setTokenText($discriminator.value);
      } else {
        $pathElement.setType(PathElement.Type.POSITION_REFERENCE);
        $pathElement.setTokenPosition($discriminator.position);
      }  
    } CLOSE_BRACKET)?
    -> ^(ELEMENT PATH_ELEMENT discriminator?)
  | ref=CHILD_REFERENCE { 
      $pathElement.setType(PathElement.Type.INDEX_REFERENCE);
      $pathElement.setTokenPosition(Integer.parseInt($ref.text.substring(1)));
    }
    -> ^(CHILD_POSITIONAL_REFERENCE CHILD_REFERENCE)
  ;
  
discriminator returns [String value, int position]
@init {
  $position = -1;
}
  : num=NUMBER { $position = Integer.parseInt($num.text);}
    -> ^(POSITION_DISCRIMINATOR NUMBER)
  | EQUALS str=STRING { $value = $str.text;}
    -> ^(VALUE_DISCRIMINATOR STRING)
  ;