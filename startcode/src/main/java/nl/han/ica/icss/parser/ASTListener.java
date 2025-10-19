package nl.han.ica.icss.parser;

import java.util.Stack;


import nl.han.ica.datastructures.HANStack;
import nl.han.ica.datastructures.IHANStack;
import nl.han.ica.icss.ast.*;
import nl.han.ica.icss.ast.literals.*;
import nl.han.ica.icss.ast.operations.AddOperation;
import nl.han.ica.icss.ast.operations.MultiplyOperation;
import nl.han.ica.icss.ast.operations.SubtractOperation;
import nl.han.ica.icss.ast.selectors.ClassSelector;
import nl.han.ica.icss.ast.selectors.IdSelector;
import nl.han.ica.icss.ast.selectors.TagSelector;
import org.antlr.v4.runtime.Token;

/**
 * This class extracts the ICSS Abstract Syntax Tree from the Antlr Parse tree.
 */
public class ASTListener extends ICSSBaseListener {
	
	//Accumulator attributes:
	private AST ast;

	//Use this to keep track of the parent nodes when recursively traversing the ast
	private IHANStack<ASTNode> currentContainer;

	// Might need pre-order traversal if copying, not sure how this will translate onto the stack, still need to research to convert Parse Tree to AST.
	// Assume with no error nodes in the Parse Tree that it's safe to convert to an AST (So no extra checking required)
	// ! W6L1 ! Important
	public ASTListener() {
		ast = new AST();

		currentContainer = new HANStack();
	}

	// TODO: Currently I don't support class and id selectors, don't see why I should specifically..
	// Will fail the ICSS0 test now, will need to ask whether or not I must implement this. (As there logically isn't
	// any difference between the input and generated content nor within the inner functionality.

	@Override
	public void enterStylesheet(ICSSParser.StylesheetContext ctx) {
		Stylesheet stylesheet = new Stylesheet();
		currentContainer.push(stylesheet);
	}

	@Override
	public void exitStylesheet(ICSSParser.StylesheetContext ctx) {
		// FIXME: Is typecasting required? Is there a good way to convert or put it in directly? (Right now gonna be typecasting, will improve later if possible)
		ast.setRoot( (Stylesheet) currentContainer.pop() );
	}

	@Override
	public void enterSelectorstmt(ICSSParser.SelectorstmtContext ctx) {
		Stylerule stylerule = new Stylerule();

		if (ctx.LOWER_IDENT() != null) {

			String selectorString = ctx.LOWER_IDENT().getText();
			Selector selector = null;

			// Check for selector type
			if (selectorString.startsWith("#")) {
				selector = new IdSelector(selectorString);
			} else if (selectorString.startsWith(".")) {
				selector = new ClassSelector(selectorString);
			} else {
				selector = new TagSelector(selectorString);
			}

			stylerule.addChild(selector);
			currentContainer.push(stylerule);
		}
	}

	@Override
	public void exitSelectorstmt(ICSSParser.SelectorstmtContext ctx) {
		Stylerule stylerule = (Stylerule) currentContainer.pop();
		currentContainer.peek().addChild(stylerule);
	}

	@Override
	public void enterPropertyexpr(ICSSParser.PropertyexprContext ctx) {
		String propertyName = null;

		if (ctx.COLOR_PROPERTY() != null) {
			propertyName = ctx.COLOR_PROPERTY().getText();
		} else if (ctx.DIM_PROPERTY() != null) {
			propertyName = ctx.DIM_PROPERTY().getText();
		}

		Declaration declaration = new Declaration(propertyName);
		currentContainer.push(declaration);
	}

	@Override
	public void exitPropertyexpr(ICSSParser.PropertyexprContext ctx) {
		Declaration declaration = (Declaration) currentContainer.pop();

		// NOTE: Not doing any checks based on the previous node here, och, is a reocurring theme, but make sure that later additions will not mess with it. (SCV:1)
		// Assuming all logic per each section is properly separated this should not be a problem, so set up code efficiëntly.
		currentContainer.peek().addChild(declaration);
	}


	@Override
	public void enterColorValue(ICSSParser.ColorValueContext ctx) {
		ASTNode value = null;

		if (ctx.HEXVAL() != null) {
			value = new ColorLiteral(ctx.HEXVAL().getText());
		} else if (ctx.CAPITAL_IDENT() != null) {
			value = new VariableReference(ctx.CAPITAL_IDENT().getText());
		}

		currentContainer.push(value);
	}



	@Override
	public void exitColorValue(ICSSParser.ColorValueContext ctx) {
		ASTNode value = currentContainer.pop();
		ASTNode parent = currentContainer.peek();

		if (parent instanceof Declaration) {
			((Declaration) parent).addChild(value);
		} else if (parent instanceof VariableAssignment) {
			((VariableAssignment) parent).addChild((Expression) value);
		}
	}



	@Override
	public void enterDimensionValue(ICSSParser.DimensionValueContext ctx) {
		ASTNode value = null;

		if (ctx.numberLiteral() != null) {
			ICSSParser.NumberLiteralContext numCtx = ctx.numberLiteral();

			if (numCtx.PIXELSIZE() != null) {
				value = new PixelLiteral(numCtx.PIXELSIZE().getText());
			} else if (numCtx.SCALAR() != null) {
				value = new ScalarLiteral(numCtx.SCALAR().getText());
			} else if (numCtx.PERCENTAGE() != null) {
				value = new PercentageLiteral(numCtx.PERCENTAGE().getText());
			}

		} else if (ctx.CAPITAL_IDENT() != null) {
			value = new VariableReference(ctx.CAPITAL_IDENT().getText());
		}

		currentContainer.push(value);
	}


	@Override
	public void exitDimensionValue(ICSSParser.DimensionValueContext ctx) {
		ASTNode value = currentContainer.pop();
		ASTNode parent = currentContainer.peek();

		if (parent instanceof Declaration) {
			((Declaration) parent).addChild(value);
		} else if (parent instanceof VariableAssignment) {
			((VariableAssignment) parent).addChild((Expression) value);
		}
	}


	@Override
	public void enterVariabledef(ICSSParser.VariabledefContext ctx) {

		VariableAssignment variableAssignment = new VariableAssignment();
		currentContainer.push(variableAssignment);

		VariableReference varRef = new VariableReference(ctx.CAPITAL_IDENT().getText());
		variableAssignment.addChild(varRef);

		// FIXME: Separation of concern is not handled properly (Don't want ot handle this here because it will result in duplicate & pot inefficient code) -> Look into this when also fixing math expressions
		if (ctx.BOOLEAN() != null) {
			BoolLiteral boolValue = new BoolLiteral(ctx.BOOLEAN().getText());
			variableAssignment.addChild(boolValue);
		}

	}

	@Override
	public void exitVariabledef(ICSSParser.VariabledefContext ctx) {
		VariableAssignment  variableAssignment  = (VariableAssignment ) currentContainer.pop();
		currentContainer.peek().addChild(variableAssignment);

	}

	@Override
	public void enterIfstmt(ICSSParser.IfstmtContext ctx) {

		IfClause ifClause = new IfClause();
		if (ctx.CAPITAL_IDENT() != null) {
			ifClause.addChild(new VariableReference(ctx.CAPITAL_IDENT().getText()));
		} else if (ctx.BOOLEAN() != null) {
			ifClause.addChild(new BoolLiteral(ctx.BOOLEAN().getText()));
		}

		currentContainer.push(ifClause);
	}

	@Override
	public void exitIfstmt(ICSSParser.IfstmtContext ctx) {
		IfClause ifClause = (IfClause) currentContainer.pop();
		currentContainer.peek().addChild(ifClause);
	}

	@Override
	public void enterElsestmt(ICSSParser.ElsestmtContext ctx) {
		ElseClause elseClause = new ElseClause();
		currentContainer.push(elseClause);
	}

	@Override
	public void exitElsestmt(ICSSParser.ElsestmtContext ctx) {
		ElseClause elseClause = (ElseClause) currentContainer.pop();
		currentContainer.peek().addChild(elseClause);
	}


	@Override
	public void enterExpr(ICSSParser.ExprContext ctx) {

		// Grammar indicates that multiplication is always first so keep check in-sequence!
		if (ctx.MUL() != null) {
			currentContainer.push(new MultiplyOperation());
		} else if (ctx.PLUS() != null) {
			currentContainer.push(new AddOperation());
		} else if (ctx.MIN() != null) {
			currentContainer.push(new SubtractOperation());
		}
	}

	@Override
	public void exitExpr(ICSSParser.ExprContext ctx) {

		if (ctx.MUL() != null || ctx.PLUS() != null || ctx.MIN() != null) {
			ASTNode operatorNode = currentContainer.pop(); // Can't instantiate Operator class, so just using ASTNode generically. Prev check is therefore necessary.
			ASTNode parent = currentContainer.peek();

			// Check for context where the expression is being done (e.g., Inside of a property expr, variable, etc)
			if (parent instanceof Expression) { // Nested math expr
				((Expression) parent).addChild(operatorNode);
			} else if (parent instanceof Declaration) { // Under a propertyExpr
				((Declaration) parent).addChild(operatorNode);
			} else if (parent instanceof VariableAssignment) { // Under a variable assignment
				((VariableAssignment) parent).addChild((Expression) operatorNode);
			}
		}
	}


	/* FIXME: This is stupid.
	Practically duplicating a lot of checking code, either separate concerns per parser rules (abstract more)
	Or introduce helper functions.

	Not to mention this breaks the entry-exit structure on the stack. Not adhering to pattern and likely not flexible.
	 */
	@Override
	public void exitFactor(ICSSParser.FactorContext ctx) {

		ASTNode node;

		// Value check
		if (ctx.CAPITAL_IDENT() != null) {
			node = new VariableReference(ctx.CAPITAL_IDENT().getText());
		} else if (ctx.SCALAR() != null) {
			node = new ScalarLiteral(ctx.SCALAR().getText());
		} else if (ctx.PERCENTAGE() != null) {
			node = new PercentageLiteral(ctx.PERCENTAGE().getText());
		} else if (ctx.PIXELSIZE() != null) {
			node = new PixelLiteral(ctx.PIXELSIZE().getText());
		} else {
			return;
		}

		currentContainer.peek().addChild(node);
	}

	public AST getAST() {
        return ast;
    }
    
}