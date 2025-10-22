package nl.han.ica.icss.checker;

import nl.han.ica.datastructures.HANLinkedList;
import nl.han.ica.datastructures.IHANLinkedList;
import nl.han.ica.icss.ast.*;
import nl.han.ica.icss.ast.types.ExpressionType;

import java.util.HashMap;

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

        if (node instanceof Stylerule || node instanceof ElseClause) { // Push new scope
            variableTypes.addFirst(new HashMap<String, ExpressionType>());
            newScopePushed = true;
        }

        // In-Order traversal
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

        if (newScopePushed) { // Pop scope when leaving the scope-context
            variableTypes.removeFirst();
        }
    }

    // Using a separated func for readability, checks if-else bodies for any problems. (Works in separated scopes)
    private void checkIfClause(IfClause ifClause) {
        // Check condition
        for (ASTNode child : ifClause.getChildren()) {
            if (!(child instanceof ElseClause) && !(child instanceof IfClause)) {
                checkNode(child);
            }
        }

        // Check if body
        for (ASTNode child : ifClause.getChildren()) {
            if (child instanceof IfClause) { // Nested If
                checkIfClause((IfClause) child);
            } else if (child instanceof VariableAssignment) { // Std body.
                variableTypes.addFirst(new HashMap<String, ExpressionType>());
                checkNode(child);
                variableTypes.removeFirst();
            }
        }

        // Check else body
        for (ASTNode child : ifClause.getChildren()) {
            if (child instanceof ElseClause) {
                variableTypes.addFirst(new HashMap<String, ExpressionType>());
                checkNode(child);
                variableTypes.removeFirst();
            }
        }
    }

    // Adds variable assignment to current scope
    private void handleVariableAssignment(VariableAssignment node) {
        if (node.name == null || node.name.name == null) return;

        String varName = node.name.name;
        HashMap<String, ExpressionType> currentScope = variableTypes.getFirst();

        // Add variable to current (top) scope
        currentScope.put(varName, ExpressionType.UNDEFINED); // FIXME: CURRENTLY USING UNDEFINED AS A PLACEHOLDER, WILL HAVE TO SET THE ACTUAL VALUE AT THIS SECTION LATER FOR OTHER IMPLEMENTAITONS (e.g, CH04, CH05
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
}
