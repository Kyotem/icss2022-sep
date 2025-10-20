grammar ICSS;

//--- LEXER: ---

// IF
IF: 'if';
ELSE: 'else';

COLOR_PROPERTY : 'color' | 'background-color';
DIM_PROPERTY : 'width' | 'height' ;

//Literals
BOOLEAN : 'TRUE' | 'FALSE' ;
PIXELSIZE: [0-9]+ 'px';
PERCENTAGE: [0-9]+ '%';
SCALAR: [0-9]+;


// Color value takes precedence over id idents
fragment HEXDIGIT : [0-9a-fA-F] ;
HEXVAL : '#' HEXDIGIT HEXDIGIT HEXDIGIT HEXDIGIT HEXDIGIT HEXDIGIT ;

// Specific identifiers for id's and css classes
// Why are these separated? Is there a reason to tokenize these separately? (Syntaxically it shouldn't matter, Only semantically, so why not [a-z0-9#.\-)
//ID_IDENT: '#' [a-z0-9\-]+;
//CLASS_IDENT: '.' [a-z0-9\-]+;

// General identifiers
// Can this be improved or are we just stuck like this?
// Currently using for : selectors
LOWER_IDENT: ([a-z] | '#' | '.' ) [a-z0-9\-]*;
// Currently using for : variable names
CAPITAL_IDENT: [A-Z] [A-Za-z0-9_]*;

// If whitespace is tokenized -> skips it (So the parser doesn't have to do anything with it)
// Or is it just not tokenized at all? (Seems like it)
WS: [ \t\r\n]+ -> skip;

// Generic Syntaxis
OPEN_BRACE: '{';
CLOSE_BRACE: '}';
BOX_BRACKET_OPEN: '[';
BOX_BRACKET_CLOSE: ']';
SEMICOLON: ';';
COLON: ':';
ASSIGNMENT_OPERATOR: ':=';

// Math operations
PLUS: '+';
MIN: '-';
MUL: '*';

//--- PARSER: ---
stylesheet: statement+ EOF;

// Root statement that can be used at the top-level
statement
    : variabledef
    | selectorstmt
    ;

// Math expressions
mathExpr
    : expr
    ;

expr
    : factor | expr MUL expr | expr (PLUS | MIN) expr
    ;

// Really REALLY duplicated, will hav e to refactor big time and make it tidy and neat
factor
    : SCALAR
    | PERCENTAGE
    | PIXELSIZE
    | CAPITAL_IDENT
    ;
// ---

// Define variables (Can include math expressions, or just a plain literal)
variabledef
    : CAPITAL_IDENT ASSIGNMENT_OPERATOR (colorValue | factor | BOOLEAN | mathExpr) SEMICOLON
    ;

// Wrapping values for re-usability
colorValue
    : HEXVAL
    | CAPITAL_IDENT
    ;

/*
 Selector (CSS) block (Takes any lowercase set of characters,
 checking for '#' and '.' is done in the conversion from parser tree to AST
 */
selectorstmt
    : LOWER_IDENT OPEN_BRACE (ifstmt | propertyexpr | variabledef)* CLOSE_BRACE
    ;

// Expressions for setting properties (e.g., width, height) and only allow grouped values. (Allow hexvals for colors, dimvals/math for dimensions)
propertyexpr
    : COLOR_PROPERTY COLON colorValue SEMICOLON
    | DIM_PROPERTY   COLON (factor | mathExpr) SEMICOLON
    ;


// if-else statements (Allow property expressions, variable definitions and nested if-statements)
ifstmt
    : IF BOX_BRACKET_OPEN (BOOLEAN | CAPITAL_IDENT) BOX_BRACKET_CLOSE OPEN_BRACE
        (propertyexpr | variabledef | ifstmt)* CLOSE_BRACE elsestmt?
    ;

elsestmt
    : ELSE OPEN_BRACE (propertyexpr | variabledef | ifstmt)* CLOSE_BRACE
    ;
// ---
