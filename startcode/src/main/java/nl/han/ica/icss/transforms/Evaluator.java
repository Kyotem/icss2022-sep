package nl.han.ica.icss.transforms;

import nl.han.ica.icss.ast.*;
import nl.han.ica.icss.ast.literals.*;
import nl.han.ica.icss.ast.operations.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

/*

    NOTE:
    Tried to use the same flow as with the Checker, but deviated a bit from it. Current flow seems inefficient and isn't all to great in my opinion.
    Would ideally refactor the way it loops over everything; but it works, so it's fine for now.

 */

public class Evaluator implements Transform {

    // NOTE: Scoping is not relevant in this step anymore as the Parser & Checker have made sure that there aren't any inconsistencies
    private final HashMap<String, Literal> variableValues = new HashMap<>();

    @Override
    public void apply(AST ast) {
        if (ast.root != null) {
            evaluateNode(ast.root, null);
        }
    }

    private void evaluateNode(ASTNode node, ASTNode parent) {
        if (node == null) return; // Guard

        // Handle IfClause replacement first
        if (node instanceof IfClause) {

            IfClause ifNode = (IfClause) node;
            BoolLiteral condition = (BoolLiteral) evaluateExpression(ifNode.getConditionalExpression());

            if (condition == null) { // Should not happen, but doing this as condition.value below could throw a NullPointerException, so doing this for clarity.
                throw new IllegalStateException("The Conditional of a IfClause is NULL whilst it should not be possible (In the Evaluator)");
            }

            // Temporarily store the nodes from the body to keep in here.
            ArrayList<ASTNode> replacementNodes = new ArrayList<>();

            if (condition.value) { // TRUE, so keep the body from the IfClause
                replacementNodes.addAll(ifNode.body);
            } else if (ifNode.getElseClause() != null) { // FALSE && ElseClause exists, so keep body of ElseClause
                replacementNodes.addAll(ifNode.getElseClause().body);
            }
            // FALSE && ElseClause does NOT exist, so we don't store any bodies.

                // Remove IfClause
                parent.removeChild(ifNode);

                // Add remaining body's children to the parent of the IfClause, and check these bodies as well (in-case for nested if-else clauses)
                for (ASTNode child : replacementNodes) {
                    parent.addChild(child);
                    evaluateNode(child, parent); // Recursively handle nested IfClauses
                }
                 return;
            }


        // Evaluate declarations
        if (node instanceof Declaration) {
            Declaration decl = (Declaration) node;
            decl.expression = evaluateExpression(decl.expression);
        }

        // Traverse AST (Bit diff than in Checker, could likely implement the same, but im just doing what I think is fine for now)
        Iterator<ASTNode> it = node.getChildren().iterator();
        while (it.hasNext()) {
            ASTNode child = it.next();

            // Handle variable assignments and recurse
            if (child instanceof VariableAssignment) {
                VariableAssignment varAssign = (VariableAssignment) child;
                Literal value = evaluateExpression(varAssign.expression);
                variableValues.put(varAssign.name.name, value);

                // Remove the variable assignment node from the AST
                it.remove();
                continue;
            }
            // Recurse
            evaluateNode(child, node);
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
            return cloneLiteral(variableValues.get(ref.name));
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

        if (op instanceof AddOperation) // + (ADD)
            return handleAdd(lhs, rhs);
        if (op instanceof SubtractOperation) // - (MIN)
            return handleSubtract(lhs, rhs);
        if (op instanceof MultiplyOperation) // * (MUL)
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

    // NOTE: Can use bit-shifting for faster math?
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
