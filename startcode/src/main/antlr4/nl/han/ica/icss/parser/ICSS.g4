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


mathExpr
    : additionExpr
    | multiplicationExpr
    ;

// Allows for multiplication at the start, or just directly a plus or minus, and multiplication at the end. (Repeating it so you can build it as long as you want)
additionExpr
    : multiplicationExpr ((PLUS | MIN) multiplicationExpr)+
    ;

// Define the starting operand to work with, and add the MUL operator if it's there.
// So technically even it's the 'multiplicationExpr', we aren't always appending a * operator.
multiplicationExpr
    : mathOperand (MUL mathOperand)*
    ;

// Base units or variables
mathOperand
    : mathValue
    | CAPITAL_IDENT  // variables
    ;

// Only values allowed for mathmathical evaluations (Can this be condensed? feels like im duplicating code)
mathValue
    : SCALAR
    | PERCENTAGE
    | PIXELSIZE
    ;

// Top-level math expression (entrypoint)

// Define variables (Can include math expressions, or just a plain literal)
// Will be handling checking of the type a bit better in the checker, otherwise the parse tree might be a bit... fun to navigate.
variabledef
    : CAPITAL_IDENT ASSIGNMENT_OPERATOR (colorValue | dimensionValue | mathExpr) SEMICOLON
    ;

// Wrapping values for re-usability
colorValue
    : HEXVAL
    | CAPITAL_IDENT
    ;

dimensionValue
    : PIXELSIZE
    | SCALAR
    | PERCENTAGE
    | CAPITAL_IDENT
    | mathExpr
    ;

// Selector (CSS) block
selectorstmt
    : (LOWER_IDENT | CAPITAL_IDENT) OPEN_BRACE (ifstmt | propertyexpr | variabledef)* CLOSE_BRACE
    ;

// Checks for the property to set (width, height, color, background-color) & only allows the correct values to be used
propertyexpr
    : COLOR_PROPERTY COLON colorValue SEMICOLON
    | DIM_PROPERTY   COLON (dimensionValue | mathExpr) SEMICOLON
    ;






// if-else statements (Currently not expecting nested logic, need to ask)
ifstmt
    : IF BOX_BRACKET_OPEN (LOWER_IDENT | CAPITAL_IDENT) BOX_BRACKET_CLOSE OPEN_BRACE
        (propertyexpr | variabledef | ifstmt)* CLOSE_BRACE
      (ELSE OPEN_BRACE (propertyexpr | variabledef | ifstmt)* CLOSE_BRACE)?
    ;