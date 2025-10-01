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

		// FIXME: Currently not making any difference between the TAG and CLASS or ID, can check starting point of string.. why not use one class?
		// Ask about it (Can I modify test / classes to have just ONE?)
		if (ctx.LOWER_IDENT() != null) {
			TagSelector selector = new TagSelector(ctx.LOWER_IDENT().getText());
			stylerule.addChild(selector);
		} else if (ctx.CAPITAL_IDENT() != null) {
			TagSelector selector = new TagSelector(ctx.CAPITAL_IDENT().getText());
			stylerule.addChild(selector);
		}
		currentContainer.push(stylerule);
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
		Declaration declaration = (Declaration) currentContainer.peek();
		declaration.addChild(value);
	}



	@Override
	public void enterDimensionValue(ICSSParser.DimensionValueContext ctx) {
		ASTNode value = null;

		if (ctx.PIXELSIZE() != null) {
			value = new PixelLiteral(ctx.PIXELSIZE().getText());
		} else if (ctx.SCALAR() != null) {
			value = new ScalarLiteral(ctx.SCALAR().getText());
		} else if (ctx.PERCENTAGE() != null) {
			value = new PercentageLiteral(ctx.PERCENTAGE().getText());
		} else if (ctx.CAPITAL_IDENT() != null) {
			value = new VariableReference(ctx.CAPITAL_IDENT().getText());
		} else if (ctx.mathExpr() != null) {
			// FIXME: Handle mathExpr later, sepaarte func or whatever
		}

		currentContainer.push(value);
	}

	@Override
	public void exitDimensionValue(ICSSParser.DimensionValueContext ctx) {
		ASTNode value = currentContainer.pop();
		Declaration declaration = (Declaration) currentContainer.peek();
		declaration.addChild(value);
	}

//	@Override
//	public void enterValue(ICSSParser.ValueContext ctx) {
//		ASTNode value = null;
//
//		// FIXME: Can this be done easier with generics? God knows.
//		if (ctx.BOOLEAN() != null) {
//			value = new BoolLiteral(ctx.BOOLEAN().getText());
//		} else if (ctx.PIXELSIZE() != null) {
//			value = new PixelLiteral(ctx.PIXELSIZE().getText());
//		} else if (ctx.PERCENTAGE() != null) {
//			value = new PercentageLiteral(ctx.PERCENTAGE().getText());
//		} else if (ctx.SCALAR() != null) {
//			value = new ScalarLiteral(ctx.SCALAR().getText());
//		} else
//
//		// TODO: Add support for mathexpressions (Not sure if it has to be handled in this declaration)
//		currentContainer.push(value);
//	}
//
//	@Override
//	public void exitValue(ICSSParser.ValueContext ctx) {
//		ASTNode value = currentContainer.pop();
//		Declaration declaration = (Declaration) currentContainer.peek();
//		declaration.addChild(value);
//	}











//	@Override
//	public void enterVariabledef(ICSSParser.VariabledefContext ctx) {
//
//		Token capitalIdentToken = ctx.CAPITAL_IDENT().getSymbol();
//
//		String varname = capitalIdentToken.getText()
//
//
//		System.out.println("Found variable: " + varname);
//	}

//	@Override
//	public void enterValue(ICSSParser.ValueContext ctx) {
//		String value = extractValue(ctx);
//
//		System.out.println("Found value: " + value);
//	}
//
//	// This feels stupid
//	public String extractValue(ICSSParser.ValueContext ctx) {
//		if (ctx.BOOLEAN() != null) {
//			return ctx.BOOLEAN().getText();
//		} else if (ctx.PIXELSIZE() != null) {
//			return ctx.PIXELSIZE().getText();
//		} else if (ctx.PERCENTAGE() != null) {
//			return ctx.PERCENTAGE().getText();
//		} else if (ctx.SCALAR() != null) {
//			return ctx.SCALAR().getText();
//		} else if (ctx.HEXVAL() != null) {
//			return ctx.HEXVAL().getText();
//		}
//
//		return null;
//	}



	public AST getAST() {
        return ast;
    }
    
}