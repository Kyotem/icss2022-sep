package nl.han.ica.icss.checker;

import nl.han.ica.datastructures.HANLinkedList;
import nl.han.ica.datastructures.IHANLinkedList;
import nl.han.ica.icss.ast.*;
import nl.han.ica.icss.ast.literals.*;
import nl.han.ica.icss.ast.operations.*;
import nl.han.ica.icss.ast.types.ExpressionType;

import java.util.HashMap;

/*
Implemented:
    - CH01
    - CH02
    - CH03 (Most of this is already caught by the parser, checker only really checks via variable references)
    - CH04 (Same as with CH03, can't directly set bad values in a declaration, but can happen via variable ref, so checking for that)
    - CH05
    - CH06

TODO:
    - Document which functions cover which CH(x) functionality fo clarity

NOTES:
 - Code is a bit messy (E.g., not ordered) -> Might have to resolve this down the line for readability.
 - Might be good to document standard flow as well, doubt i'll remember this if I pause for a few days.
 - Maybe make own hashmap to implement?

 */

public class Checker {

    private IHANLinkedList<HashMap<String, ExpressionType>> variableTypes;

    public void check(AST ast) {
        variableTypes = new HANLinkedList<>();

        // Top-level scope (Global definition of vars)
        variableTypes.addFirst(new HashMap<String, ExpressionType>());

        // ! Entrypoint !
        if (ast.root != null) {
            checkNode(ast.root);
        }
    }

    private void checkNode(ASTNode node) {

        if (node == null) return; // Guard

        if (node instanceof IfClause) {
            checkIfClause((IfClause) node);
            return;
        }

        boolean newScopePushed = false;

        if (node instanceof Stylerule || node instanceof ElseClause) { // Push new scope (Extra scope-push for else-clause here to accommodate for style-rule variables
            variableTypes.addFirst(new HashMap<String, ExpressionType>());
            newScopePushed = true;
        }


        // Check math operations
        if (node instanceof Operation) {
            /*
             NOTE:
             We aren't using the 'get' of this function in this context.
             The code that checks for whether or not math is valid per the rules calls the 'getExpressionType();' function recursively.
             So that function DOES need the values returned from it's recursive calls.
             Feels a bit off to call a get function and not doing anything with the value, probably not good convention-wise, but it works... for now.
             */
            getExpressionType(node);
        }

        // Traverse AST
        for (ASTNode child : node.getChildren()) {

            // Skip variablereferences that are nested inside of variableassignments. (Still don't know why var reference has to be defined in an assignment? OH well.
            if (child instanceof VariableReference
                    && !(node instanceof VariableAssignment && node.getChildren().get(0) == child)) {
                handleVariableReference((VariableReference) child);
            }

            // Recurse
            checkNode(child);

            // Registering variable assignment after visiting the right hand side (To get value first)
            // Doing this otherwise the order of eval would be wrong, might need to sketch this out a bit better.
            if (child instanceof VariableAssignment) {
                handleVariableAssignment((VariableAssignment) child);
            }
        }

        // Check properties (e.g., width, color)
        if (node instanceof Declaration) {
            checkDeclaration((Declaration) node);
        }

        if (newScopePushed) { // Pop scope when leaving the scope-context
            variableTypes.removeFirst();
        }
    }


    // Check for declarations (e.g., Width, Color, etc)
    // NOTE: I'm only checking variables used for declarations, the parser makes sure declarations can't have any wrong types (e.g., hexvals in dimensional declarations are not possible, unless done by variable, which is checked here)
    private void checkDeclaration(Declaration decl) {
        if (decl.expression == null) return; // Guard

        // This list contains all allowed types of 'ExpressionType' a property may have. (e.g, Dimension properties can have SCALAR, PERCENTAGE, but if-clause only a BOOLEAN)
        ExpressionType[] expectedTypes = getExpectedPropertyTypes(decl.property.name);

        if (decl.expression instanceof VariableReference) {

            VariableReference ref = (VariableReference) decl.expression;
            ExpressionType actualType = resolveVariableType(ref.name);

            if (actualType == ExpressionType.UNDEFINED) { // Variable not defined or type not properly set IN a 'declaration'
                ref.setError("Variable '" + ref.name + "' is not defined or has unknown type.");
            } else if (!isTypeAllowed(actualType, expectedTypes)) { // Compare type of the variable to the declaration it's being used in.
                // NOTE: Currently you can get an error like 'Property 'height' expects one of PIXEL, PERCENTAGE, SCALAR but got COLOR.'
                // TODO: Properties don't just allow a singular SCALAR value, only in math, so maybe have to change the allowed types or the error.
                decl.setError("Property '" + decl.property.name + "' expects one of " +
                        formatAllowedTypes(expectedTypes) + " but got " + actualType + ".");
            }
        }
    }


    // Mapping property names to their respective expression types that are allowed.
    private ExpressionType[] getExpectedPropertyTypes(String propertyName) {
        if (propertyName == null) return new ExpressionType[]{ExpressionType.UNDEFINED}; // Guard

        // NOTE: Switch is probably most readable in this case, not sure if it's the fastest though... will have to look into it.
        switch (propertyName) {
            case "color":
            case "background-color":
                return new ExpressionType[]{ExpressionType.COLOR};
            case "width":
            case "height":
                return new ExpressionType[]{ExpressionType.PIXEL, ExpressionType.PERCENTAGE, ExpressionType.SCALAR};
            default:
                return new ExpressionType[]{ExpressionType.UNDEFINED};
        }
    }


    // Using a separated func for readability, checks if-else bodies for any problems. (Works in separated scopes)
    private void checkIfClause(IfClause ifClause) {
        if (ifClause.conditionalExpression == null) return; // Guard

        // Check condition type
        ASTNode condition = ifClause.conditionalExpression;
        ExpressionType type = getExpressionType(condition);

        if (type != ExpressionType.BOOL) {
            ifClause.setError("Condition in if-statement must be of type BOOLEAN but got " + type + ".");
        }

        variableTypes.addFirst(new HashMap<>()); // Push new if scope
        // Check if-body (Separate scope)
        for (ASTNode child : ifClause.getChildren()) {
            if (!(child instanceof ElseClause)) { // Skip else statements
                if (child instanceof VariableAssignment) {
                    handleVariableAssignment((VariableAssignment) child);
                }
                checkNode(child);
            }
        }
        variableTypes.removeFirst(); // Pop if scope


        // Check else-body (Separate scope)
        if (ifClause.elseClause != null) {
            variableTypes.addFirst(new HashMap<>()); // Push else scope
            for (ASTNode child : ifClause.elseClause.getChildren()) {
                if (child instanceof VariableAssignment) {
                    handleVariableAssignment((VariableAssignment) child);
                }
                checkNode(child);
            }
            variableTypes.removeFirst(); // Pop else scope
        }
    }


    // Adds variable assignment to current scope
    private void handleVariableAssignment(VariableAssignment node) {
        if (node.name == null || node.name.name == null) return; // Guard

        String varName = node.name.name;
        HashMap<String, ExpressionType> currentScope = variableTypes.getFirst();

        // Get the variable value (else undefined) and pushes to the scope.
        if (node.expression != null) {
            ExpressionType type = getExpressionType(node.expression);
            currentScope.put(varName, type);
        } else {
            currentScope.put(varName, ExpressionType.UNDEFINED);
        }
    }

    // Validates if variable that's being referenced exists / is accessible in respective scope.
    private void handleVariableReference(VariableReference node) {
        String varName = node.name;

        if (!isVariableDefined(varName)) {
            node.setError("Variable '" + varName + "' is not defined in this scope.");
        }
    }

    private boolean isVariableDefined(String name) {
        // Search from innermost -> outermost scope
        for (int i = 0; i < variableTypes.getSize(); i++) {
            HashMap<String, ExpressionType> scope = variableTypes.get(i);
            if (scope.containsKey(name)) {
                return true;
            }
        }
        return false;
    }

    // Practically duplication from 'isVariableDefined' but gets the actual type.
    // TODO: Might be possible to condense both functions into one if it's actually a merit
    private ExpressionType resolveVariableType(String name) {
        for (int i = 0; i < variableTypes.getSize(); i++) {
            HashMap<String, ExpressionType> scope = variableTypes.get(i);
            if (scope.containsKey(name)) {
                return scope.get(name);
            }
        }
        return ExpressionType.UNDEFINED;
    }


    // TODO: Function has grown a bit large, cognitive complexity a tad high, might need to refactor for readability. (Separate math concerns perhaps?)
    private ExpressionType getExpressionType(ASTNode node) {
        if (node instanceof BoolLiteral) return ExpressionType.BOOL;
        if (node instanceof ColorLiteral) return ExpressionType.COLOR;
        if (node instanceof PixelLiteral) return ExpressionType.PIXEL;
        if (node instanceof PercentageLiteral) return ExpressionType.PERCENTAGE;
        if (node instanceof ScalarLiteral) return ExpressionType.SCALAR;

        if (node instanceof VariableReference) {
            return resolveVariableType(((VariableReference) node).name);
        }

        // NOTE: Errors are set on the Operation node, not the actual node that is causing the issue. (e.g., VarRef in the operation)
        if (node instanceof Operation) {

            Operation op = (Operation) node;
            // Recurse
            ExpressionType lhsType = getExpressionType(op.lhs);
            ExpressionType rhsType = getExpressionType(op.rhs);
            // ---

            if (lhsType == ExpressionType.UNDEFINED || rhsType == ExpressionType.UNDEFINED) {
                return ExpressionType.UNDEFINED;
            }

            // Prevent colors from being used in any math operation
            if (lhsType == ExpressionType.COLOR || rhsType == ExpressionType.COLOR) {
                op.setError("Cannot use COLOR in math operations.");
                return ExpressionType.UNDEFINED;
            }

            // Check if types match exactly between + and - operands
            if (op instanceof AddOperation || op instanceof SubtractOperation) {
                if (lhsType != rhsType) {
                    // Dynamically indicate at what operator it's happening. (Discern between + and -, not exact ref)
                    op.setError("Operands of '" + (op instanceof AddOperation ? "+" : "-") + "' must have the same type, got "
                            + lhsType + " and " + rhsType + ".");
                    return ExpressionType.UNDEFINED;
                }
                return lhsType;
            }

            // Check if Multiplication operations include at least ONE SCALAR type.
            if (op instanceof MultiplyOperation) {
                boolean lhsIsScalar = lhsType == ExpressionType.SCALAR;
                boolean rhsIsScalar = rhsType == ExpressionType.SCALAR;

                if (lhsIsScalar && rhsIsScalar) return ExpressionType.SCALAR;
                if (lhsIsScalar) return rhsType;
                if (rhsIsScalar) return lhsType;

                // Reject if no scalar value is found.
                op.setError("Multiplication requires at least one scalar operand, got " + lhsType + " * " + rhsType);
                return ExpressionType.UNDEFINED;
            }
        }

        return ExpressionType.UNDEFINED;
    }


    // Compares to the list of allowed types and the type currently set in the AST, true if allowed, false if not.
    private boolean isTypeAllowed(ExpressionType type, ExpressionType[] allowed) {
        for (ExpressionType allowedType : allowed) {
            if (type == allowedType) return true;
        }
        return false;
    }

    // Used to indicate which allowed expression types are allowed (For error messages)
    private String formatAllowedTypes(ExpressionType[] allowed) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < allowed.length; i++) {
            sb.append(allowed[i]);
            if (i < allowed.length - 1) sb.append(", ");
        }

        return sb.toString();
    }
}
