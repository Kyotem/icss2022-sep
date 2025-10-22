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
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;

/**
 * This class extracts the ICSS Abstract Syntax Tree from the Antlr Parse tree.
 */
public class ASTListener extends ICSSBaseListener {
	
	//Accumulator attributes:
	private AST ast;

	//Use this to keep track of the parent nodes when recursively traversing the ast
	private IHANStack<ASTNode> currentContainer;

	public ASTListener() {
		ast = new AST();

		currentContainer = new HANStack();
	}

	// NOTE: Currently I have to typecast on every exit event to the type of node it should be,

	// Root
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
		attachLatestOnStackToParent();
	}

	@Override
	public void enterPropertyexpr(ICSSParser.PropertyexprContext ctx) {
		String propertyName = null; // FIXME: Need a nullcheck

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
		attachLatestOnStackToParent();
	}

	// NOTE: Colorvalue is handled as a separated parser rule, dimensional values is handled under 'factor' per parser.
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
		attachLatestOnStackToParent();
	}


	@Override
	public void enterVariabledef(ICSSParser.VariabledefContext ctx) {

		VariableAssignment variableAssignment = new VariableAssignment();
		currentContainer.push(variableAssignment);

		// NOTE: Uhh was it intended to set the reference under an assignment? Seems redundant.
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
		attachLatestOnStackToParent();
	}

	// if - else statements
	@Override
	public void enterIfstmt(ICSSParser.IfstmtContext ctx) {

		IfClause ifClause = new IfClause();

		// Parse either a Variable or a Boolean value.
		if (ctx.CAPITAL_IDENT() != null) {
			ifClause.addChild(new VariableReference(ctx.CAPITAL_IDENT().getText()));
		} else if (ctx.BOOLEAN() != null) {
			ifClause.addChild(new BoolLiteral(ctx.BOOLEAN().getText()));
		}

		currentContainer.push(ifClause);
	}

	@Override
	public void exitIfstmt(ICSSParser.IfstmtContext ctx) {
		attachLatestOnStackToParent();
	}

	@Override
	public void enterElsestmt(ICSSParser.ElsestmtContext ctx) {
		ElseClause elseClause = new ElseClause();
		currentContainer.push(elseClause);
	}

	@Override
	public void exitElsestmt(ICSSParser.ElsestmtContext ctx) {
		attachLatestOnStackToParent();
	}
	// ----

	@Override
	public void enterExpr(ICSSParser.ExprContext ctx) {
		Operation op = createOperation(ctx);
		if (op != null) currentContainer.push(op);
	}

	// Math Handling
	@Override
	public void exitExpr(ICSSParser.ExprContext ctx) {

		if (ctx.MUL() != null || ctx.PLUS() != null || ctx.MIN() != null) {
			attachLatestOnStackToParent();
		}
	}


	/* FIXME: This is stupid.
	Practically duplicating a lot of checking code, either separate concerns per parser rules (abstract more)
	Or introduce helper functions.

	Not to mention this breaks the entry-exit structure on the stack. Not adhering to pattern and likely not flexible.
	 */
	@Override
	public void enterFactor(ICSSParser.FactorContext ctx) {
		currentContainer.push(buildLiteralFromContext(ctx));
	}

	@Override
	public void exitFactor(ICSSParser.FactorContext ctx) {
		attachLatestOnStackToParent();
	}
	// ----


	// Helper functions

	// Just a separation in-case for re-usability & just readability in general.
	private Operation createOperation(ICSSParser.ExprContext ctx) {
		if (ctx.MUL() != null) return new MultiplyOperation();
		if (ctx.PLUS() != null) return new AddOperation();
		if (ctx.MIN() != null) return new SubtractOperation();
		return null;
	}


	// Currently only implemented on 'factor', can this be used on other places?
	// Can't use ParserRuleContext + getToken because it would result in walking the parse tree multiple times (Not fast!)
	// NOTE: So using it only for factorcontext, calling an extra function takes up more stack space, but is more readable!
	private ASTNode buildLiteralFromContext(ICSSParser.FactorContext ctx) {
		if (ctx == null) return null;

		if (ctx.PIXELSIZE() != null) {
			return new PixelLiteral(ctx.PIXELSIZE().getText());
		} else if (ctx.PERCENTAGE() != null) {
			return new PercentageLiteral(ctx.PERCENTAGE().getText());
		} else if (ctx.SCALAR() != null) {
			return new ScalarLiteral(ctx.SCALAR().getText());
		} else if (ctx.CAPITAL_IDENT() != null) {
			return new VariableReference(ctx.CAPITAL_IDENT().getText());
		}

		return null;
	}

	// Doing this so I don't have to typecast in every entry/exit conditions to centralize it a bit more.
	// This means I won't do specific typecasting to classes that extend ASTNode (etc), so it might make it less readable, but this significantly reduces code count.
	private void attachLatestOnStackToParent() {
		ASTNode node = (ASTNode) currentContainer.pop();
		currentContainer.peek().addChild(node);
	}


	public AST getAST() {
        return ast;
    }
    
}