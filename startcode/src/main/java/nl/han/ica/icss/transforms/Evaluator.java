package nl.han.ica.icss.transforms;

import nl.han.ica.datastructures.HANLinkedList;
import nl.han.ica.datastructures.IHANLinkedList;
import nl.han.ica.icss.ast.*;
import nl.han.ica.icss.ast.literals.*;
import nl.han.ica.icss.ast.operations.*;

import java.util.HashMap;
import java.util.Iterator;

public class Evaluator implements Transform {

    private IHANLinkedList<HashMap<String, Literal>> variableValues;

    @Override
    public void apply(AST ast) {
        variableValues = new HANLinkedList<HashMap<String, Literal>>();

        // Top-level scope (Global definition of vars)
        variableValues.addFirst(new HashMap<String, Literal>());

        if (ast.root != null) {
            evaluateNode(ast.root);
        }
    }

    private void evaluateNode(ASTNode node) {
        if (node == null) return; // Guard

        boolean newScopePushed = false;

// TODO
//        if (node instanceof IfClause) {
//        }

        if (node instanceof Stylerule || node instanceof ElseClause) { // Push new scope (Extra scope-push for else-clause here to accommodate for style-rule variables
            variableValues.addFirst(new HashMap<String, Literal>());
            newScopePushed = true;
        } else if (node instanceof Declaration) {
            Declaration decl = (Declaration) node;
            decl.expression = evaluateExpression(decl.expression);
        }

        // Traverse AST (Bit diff than in Checker, could likely implement the same, but im just doing what I think is fine for now)
        Iterator<ASTNode> it = node.getChildren().iterator();
        while (it.hasNext()) {
            ASTNode child = it.next();

            // Handle variable assignments before recursion
            if (child instanceof VariableAssignment) {
                VariableAssignment varAssign = (VariableAssignment) child;
                Literal value = evaluateExpression(varAssign.expression);
                variableValues.getFirst().put(varAssign.name.name, value);

                // Remove the variable assignment node from the AST
                it.remove();
                continue;
            }

            // Recurse
            evaluateNode(child);
        }

        if (newScopePushed) {
            variableValues.removeFirst();
        }
    }


    private Literal evaluateExpression(Expression expr) {
        if (expr == null) return null; // Guard

        // If expression is already evaluated
        if (expr instanceof Literal) {
            return (Literal) expr;
        }

        // Resolve variable
        if (expr instanceof VariableReference) {
            VariableReference ref = (VariableReference) expr;
            Literal resolved = resolveVariableValue(ref.name);

            if (resolved != null) {
                // Cloning to prevent shared refs
                return cloneLiteral(resolved);
            }
            return null;
        }

        // Eval operations
        if (expr instanceof Operation) {
            Operation op = (Operation) expr;

            // Recurse
            Literal lhs = evaluateExpression(op.lhs);
            Literal rhs = evaluateExpression(op.rhs);

            return evaluateOperation(op, lhs, rhs);
        }

        return null;
    }

    private Literal evaluateOperation(Operation op, Literal lhs, Literal rhs) {
        if (lhs == null || rhs == null) return null; // Guard

        if (op instanceof AddOperation)
            return handleAdd(lhs, rhs);
        if (op instanceof SubtractOperation)
            return handleSubtract(lhs, rhs);
        if (op instanceof MultiplyOperation)
            return handleMultiply(lhs, rhs);

        return null;
    }

    private Literal handleAdd(Literal lhs, Literal rhs) {
        if (lhs instanceof PixelLiteral && rhs instanceof PixelLiteral)
            return new PixelLiteral(((PixelLiteral) lhs).value + ((PixelLiteral) rhs).value);
        if (lhs instanceof PercentageLiteral && rhs instanceof PercentageLiteral)
            return new PercentageLiteral(((PercentageLiteral) lhs).value + ((PercentageLiteral) rhs).value);
        if (lhs instanceof ScalarLiteral && rhs instanceof ScalarLiteral)
            return new ScalarLiteral(((ScalarLiteral) lhs).value + ((ScalarLiteral) rhs).value);
        return null;
    }

    private Literal handleSubtract(Literal lhs, Literal rhs) {
        if (lhs instanceof PixelLiteral && rhs instanceof PixelLiteral)
            return new PixelLiteral(((PixelLiteral) lhs).value - ((PixelLiteral) rhs).value);
        if (lhs instanceof PercentageLiteral && rhs instanceof PercentageLiteral)
            return new PercentageLiteral(((PercentageLiteral) lhs).value - ((PercentageLiteral) rhs).value);
        if (lhs instanceof ScalarLiteral && rhs instanceof ScalarLiteral)
            return new ScalarLiteral(((ScalarLiteral) lhs).value - ((ScalarLiteral) rhs).value);
        return null;
    }

    // NOTE: Can use bit-shifting for faster math.
    private Literal handleMultiply(Literal lhs, Literal rhs) {

        if (lhs instanceof ScalarLiteral && rhs instanceof ScalarLiteral)
            return new ScalarLiteral(((ScalarLiteral) lhs).value * ((ScalarLiteral) rhs).value);

        if (lhs instanceof ScalarLiteral && rhs instanceof PixelLiteral)
            return new PixelLiteral(((ScalarLiteral) lhs).value * ((PixelLiteral) rhs).value);
        if (lhs instanceof PixelLiteral && rhs instanceof ScalarLiteral)
            return new PixelLiteral(((PixelLiteral) lhs).value * ((ScalarLiteral) rhs).value);

        if (lhs instanceof ScalarLiteral && rhs instanceof PercentageLiteral)
            return new PercentageLiteral(((ScalarLiteral) lhs).value * ((PercentageLiteral) rhs).value);
        if (lhs instanceof PercentageLiteral && rhs instanceof ScalarLiteral)
            return new PercentageLiteral(((PercentageLiteral) lhs).value * ((ScalarLiteral) rhs).value);

        return null;
    }

    private Literal resolveVariableValue(String name) {
        for (int i = 0; i < variableValues.getSize(); i++) {
            HashMap<String, Literal> scope = variableValues.get(i);
            if (scope.containsKey(name)) {
                return scope.get(name);
            }
        }
        return null; // This should not happen... but, we'll see
    }

    private Literal cloneLiteral(Literal lit) {
        if (lit instanceof PixelLiteral)
            return new PixelLiteral(((PixelLiteral) lit).value);
        if (lit instanceof PercentageLiteral)
            return new PercentageLiteral(((PercentageLiteral) lit).value);
        if (lit instanceof ScalarLiteral)
            return new ScalarLiteral(((ScalarLiteral) lit).value);
        if (lit instanceof ColorLiteral)
            return new ColorLiteral(((ColorLiteral) lit).value);
        if (lit instanceof BoolLiteral)
            return new BoolLiteral(((BoolLiteral) lit).value);
        return null;
    }
}
