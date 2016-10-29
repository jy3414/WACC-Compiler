parser grammar WACCParser;

options {
  tokenVocab=WACCLexer;
}

//Binary operators
//binary_oper : PLUS | MINUS | MULT | DIV | MOD | GREATER | GREATER_OR_EQUAL | SMALLER | SMALLER_OR_EQUAL | EQUAL | NOT_EQUAL | LOGICAL_AND |LOGICAL_OR;

//Program definition
program: BEGIN (func)* stat END EOF;

//Function definition
func: type ident OPEN_PARENTHESES (param_list)? CLOSE_PARENTHESES IS func_return END;

func_return: RETURN expr
| IF expr THEN func_return ELSE func_return FI
| stat SEMICOLON RETURN expr ;

//Parameter list
param_list: param (COMMA param)*;

param: type ident;

//Statements
stat: SKIP
| type ident ASSIGN_EQUAL assign_rhs
| assign_lhs ASSIGN_EQUAL assign_rhs
| READ assign_lhs
| FREE expr
| RETURN expr
| EXIT expr
| PRINT expr
| PRINTLN expr
| FOR forcond stat DONE
| IF expr THEN stat ELSE stat FI
| DO stat DOAGAINWHILE expr
| WHILE expr DO stat DONE
| BEGIN stat END
| stat SEMICOLON stat
| BREAK
| CONTINUE;

//for loop
forcond: OPEN_PARENTHESES forass1 SEMICOLON expr SEMICOLON forass2 CLOSE_PARENTHESES;
forass1: type ident ASSIGN_EQUAL assign_rhs;
forass2: ident ASSIGN_EQUAL assign_rhs;

//Assignments
assign_lhs: ident
| array_elem
| pair_elem;

assign_rhs: expr
| array_liter
| NEWPAIR OPEN_PARENTHESES expr COMMA expr CLOSE_PARENTHESES
| pair_elem
| CALL ident OPEN_PARENTHESES (arg_list)?  CLOSE_PARENTHESES;

//Argument list
arg_list: expr (COMMA expr)*;

//Pair type
pair_elem: FST expr
| SND expr;

//Base type
type: base_type
| type OPEN_SQUARE_BRACKET CLOSE_SQUARE_BRACKET
| pair_type;

base_type: INT
| BOOL
| CHAR
| STRING;

//Array type
array_type: type OPEN_SQUARE_BRACKET CLOSE_SQUARE_BRACKET;

pair_type: PAIR OPEN_PARENTHESES pair_elem_type COMMA pair_elem_type CLOSE_PARENTHESES;

pair_elem_type: base_type
| array_type
| PAIR;

//Expressions
expr: unary_oper expr
| bool_liter
| char_liter
| str_liter
| pair_liter
| ident
| array_elem
| expr (GREATER | GREATER_OR_EQUAL | SMALLER | SMALLER_OR_EQUAL ) expr
| expr (EQUAL | NOT_EQUAL) expr
| expr LOGICAL_AND expr
| expr LOGICAL_OR expr
| expr (MULT | DIV | MOD ) expr
| expr (PLUS | MINUS ) expr
//| expr binary_oper expr




| int_liter
| OPEN_PARENTHESES expr CLOSE_PARENTHESES;



int_sign: PLUS | MINUS ;

//Unary operator
unary_oper: LOGICAL_NOT
| MINUS
| LEN
| ORD
| CHR;

//Ident
ident: IDENT;

array_elem: ident (OPEN_SQUARE_BRACKET expr CLOSE_SQUARE_BRACKET)+ ;

digit: SINGLE_DIGIT ;

//Type liter
int_liter: (int_sign)? INTEGER ;

bool_liter: TRUE | FALSE ;

char_liter: CHAR_LITER ;

str_liter: STR_LITER;

array_liter: OPEN_SQUARE_BRACKET (expr (COMMA expr)*)? CLOSE_SQUARE_BRACKET ;

pair_liter: NULL ;
