lexer grammar WACCLexer;

//Skip comments
COMMENT: SHARP ~('\n')* '\n' -> skip;

//Statements
SKIP : 'skip' ;
READ : 'read' ;
FREE: 'free' ;
RETURN: 'return' ;
EXIT: 'exit' ;
PRINT : 'print' ;
PRINTLN : 'println' ;
IF : 'if' ;
THEN : 'then' ;
ELSE : 'else' ;
FI : 'fi' ;
WHILE : 'while' ;
DO : 'do' ;
DOAGAINWHILE : 'doagainwhile' ;
DONE : 'done' ;
BEGIN : 'begin' ;
END : 'end' ;
IS : 'is' ;
FOR: 'for' ;
BREAK: 'break' ;
CONTINUE: 'continues' ;

//Other keywords(from assign-rhs / pair-type)
CALL : 'call' ;
NEWPAIR : 'newpair' ;
PAIR : 'pair' ;

//Pair elem
FST : 'fst' ;
SND : 'snd' ;

//Binary operators
LOGICAL_AND : '&&' ;
PLUS : '+' ;
MINUS : '-' ;
MULT : '*' ;
DIV : '/' ;
MOD : '%' ;
GREATER : '>' ;
GREATER_OR_EQUAL : '>=' ;
SMALLER : '<' ;
SMALLER_OR_EQUAL : '<=' ;
EQUAL : '==' ;
ASSIGN_EQUAL : '=' ;
NOT_EQUAL : '!=' ;
LOGICAL_OR : '||' ;

//Unary operator
LOGICAL_NOT : '!' ;
LEN : 'len' ;
ORD : 'ord' ;
CHR : 'chr' ;

//Types
INT : 'int' ;
BOOL : 'bool' ;
CHAR : 'char' ;
STRING : 'string' ;

//Null & comment symbol
NULL: 'null' ;
SHARP: '#' ;

//Boolean type
TRUE: 'true' ;
FALSE: 'false' ;

//Brackets
OPEN_PARENTHESES : '(' ;
CLOSE_PARENTHESES : ')' ;
OPEN_SQUARE_BRACKET : '[' ;
CLOSE_SQUARE_BRACKET : ']' ;

//Separators
COMMA : ',';
SEMICOLON : ';';

//Numbers
fragment DIGIT : '0'..'9' ;
INTEGER: DIGIT+ ;
SINGLE_DIGIT: DIGIT ;

//Ident
fragment SINGLE_IDENT: (UNDERSCORE | LOWER_CASE_ALPHABET | UPPER_CASE_ALPHABET | SINGLE_DIGIT);
IDENT:  (UNDERSCORE | LOWER_CASE_ALPHABET | UPPER_CASE_ALPHABET) SINGLE_IDENT*;
UNDERSCORE : '_' ;

//letters
LOWER_CASE_ALPHABET : 'a'..'z' ;
UPPER_CASE_ALPHABET : 'A'..'Z' ;

//Skip whitespaces
WS: (' ' | '\t' | '\r' | '\n') -> skip;
NEW_LINE: '\n' -> skip;


//Characters & Strings
CHAR_LITER: '\'' CHARACTER '\'' ;

CHARACTER: ~('\\' | '\'' | '\"')
| '\\' ESCAPED_CHAR;

ESCAPED_CHAR : '0'
| 'b'
| 't'
| 'n'
| 'f'
| 'r'
| '"'
| '\''
| '\\' ;


STR_LITER: '"' (CHARACTER)* '"';

