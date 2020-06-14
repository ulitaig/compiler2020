grammar MX;

program
    :   programUnit*
    ;

programUnit
    : classDef | functionDef | variableDef
    ;


// ------------- DEFINITION -------------
classDef
    : Class Identifier '{' (functionDef | variableDef | constructorDef)* '}' ';'
    ;

functionDef
    : (type | Void) Identifier '(' parameterList? ')' block
    ;

constructorDef
    :   Identifier '(' parameterList? ')' block
    ;

variableDef
    : type variableList ';'
    ;

variableList
	:   singleVariable (',' singleVariable)*
	;

singleVariable
	:   Identifier ('=' expr)?
	;

parameterList
	:   parameter (',' parameter)*
	;

parameter
	:   type Identifier
	;

type
    :   type '[' ']'
    |   nonArrayType
    ;

nonArrayType
    :   Bool
    |   Int
    |   String
    |   Identifier
    ;

// ------------- STATEMENT -------------

block
    : '{' statement* '}'
    ;

statement
    :   block                                           #blockStmt
    |   variableDef                                     #variableStmt
    |   expr ';'                                        #exprStmt
    |   controlStatement                                #controlStmt
    |   ';'                                             #emptyStmt
    ;


controlStatement
	:   If '(' expr ')' statement (Else statement)?     #ifStmt
    |   While '(' expr ')' statement                    #whileStmt
    |   For '(' init=expr? ';'
                cond=expr? ';'
                step=expr? ')' statement                #forStmt
    |   Return expr? ';'                                #returnStmt
    |   Break ';'                                       #breakStmt
    |   Continue ';'                                    #continueStmt
	;

// ------------- EXPRESSION -------------


expr
    :   This                                            #thisExpr
    |   constant                                        #constExpr
    |   Identifier                                      #idExpr
    |   '(' expr ')'                                    #subExpr
    |   expr '[' expr ']'                               #subscriptExpr
    |   expr '.' Identifier                             #memberExpr
    |   expr '(' exprList? ')'                          #funcCallExpr
    |   <assoc=right> New creator                       #newExpr

    |   expr op=('++' | '--')                           #postfixExpr
    |   <assoc=right> op=('++' | '--') expr             #unaryExpr
    |   <assoc=right> op=( '+' | '-' ) expr             #unaryExpr
    |   <assoc=right> op=( '!' | '~' ) expr             #unaryExpr

    |   lhs=expr op=('*' | '/' | '%') rhs=expr          #binaryExpr
    |   lhs=expr op=('+' | '-') rhs=expr                #binaryExpr
    |   lhs=expr op=('<<' | '>>') rhs=expr              #binaryExpr
    |   lhs=expr op=('<' | '>') rhs=expr                #binaryExpr
    |   lhs=expr op=('<=' | '>=') rhs=expr              #binaryExpr
    |   lhs=expr op=('==' | '!=') rhs=expr              #binaryExpr
    |   lhs=expr op='&' rhs=expr                        #binaryExpr
    |   lhs=expr op='^' rhs=expr                        #binaryExpr
    |   lhs=expr op='|' rhs=expr                        #binaryExpr
    |   lhs=expr op='&&' rhs=expr                       #binaryExpr
    |   lhs=expr op='||' rhs=expr                       #binaryExpr
    |   <assoc=right> lhs=expr op='=' rhs=expr          #binaryExpr

    ;

exprList
    :   expr (',' expr)*
    ;

creator
	:   nonArrayType ('[' expr ']')* ('[' ']')+ ('[' expr ']')+     #noCreator
	|   nonArrayType ('[' expr ']')+ ('[' ']')*                     #arrayCreator
	|   nonArrayType '('  ')'                                       #classCreator
	|   nonArrayType                                                #singleCreator
	;

constant
	:   BoolConstant
	|   IntegerConstant
	|   StringConstant
	|   Null
	;

// ----------- RESERVED WORDS -----------
Int     : 'int';
Bool    : 'bool';
String  : 'string';
Void    : 'void';
If      : 'if';
Else    : 'else';
For     : 'for';
While   : 'while';
Break   : 'break';
Continue: 'continue';
Return  : 'return';
New     : 'new';
Class   : 'class';
This    : 'this';
Null    : 'null';

fragment True    : 'true';
fragment False   : 'false';
fragment Esc: '\\"' | '\\n' | '\\\\';


BoolConstant: True | False;
IntegerConstant: [0-9]+;
StringConstant: '"' (Esc|.)*? '"';
Identifier  : [a-zA-Z] [a-zA-Z0-9_]*;

// ------------- SKIPS --------------
WhiteSpace
	:   [ \t\n\r]+
		-> skip
	;

NewLine
	:   ('\r' | '\n' | '\r' '\n')
		-> skip
	;

BlockComment
	:   '/*' .*? '*/'
		-> skip
	;

LineComment
	:   '//' ~[\r\n]*
		-> skip
	;