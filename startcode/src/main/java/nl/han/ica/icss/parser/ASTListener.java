	package nl.han.ica.icss.parser;

	import nl.han.ica.datastructures.HANStack;
	import nl.han.ica.icss.ast.*;
	import nl.han.ica.icss.ast.literals.*;
	import nl.han.ica.icss.ast.operations.AddOperation;
	import nl.han.ica.icss.ast.operations.MultiplyOperation;
	import nl.han.ica.icss.ast.operations.SubtractOperation;
	import nl.han.ica.icss.ast.selectors.ClassSelector;
	import nl.han.ica.icss.ast.selectors.IdSelector;
	import nl.han.ica.icss.ast.selectors.TagSelector;

	/**
	 * This class extracts the ICSS Abstract Syntax Tree from the Antlr Parse tree.
	 */
	public class ASTListener extends ICSSBaseListener {

		//Accumulator attributes:
		private final AST ast;

		//Use this to keep track of the parent nodes when recursively traversing the ast
		private final HANStack<ASTNode> currentContainer;

		public ASTListener() {
			ast = new AST();

			currentContainer = new HANStack<>();
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
			// Q: Is typecasting required? Is there a good way to convert or put it in directly? | A: Not specifically because they already are part of an ASTNode, might be good to typecast? Eh, check attachLatestOnStackToParent(); for more info.
			ast.setRoot( (Stylesheet) currentContainer.pop() );
		}

		@Override
		public void enterSelectorstmt(ICSSParser.SelectorstmtContext ctx) {
			Stylerule stylerule = new Stylerule();

			if (ctx.LOWER_IDENT() != null) {

				String selectorString = ctx.LOWER_IDENT().getText();
				Selector selector;

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
			String propertyName = null;

			if (ctx.COLOR_PROPERTY() != null) {
				propertyName = ctx.COLOR_PROPERTY().getText();
			} else if (ctx.DIM_PROPERTY() != null) {
				propertyName = ctx.DIM_PROPERTY().getText();
			}

			if(propertyName == null) { // NOTE: This should NOT happen, but adding a check to prevent application from entering a bad state.
				throw new IllegalStateException("propertyName from a PropertyExpression is NULL");
			}

			Declaration declaration = new Declaration(propertyName);
			currentContainer.push(declaration);
		}

		@Override
		public void exitPropertyexpr(ICSSParser.PropertyexprContext ctx) {
			attachLatestOnStackToParent();
		}

		// NOTE: Color value is handled as a separated parser rule, dimensional values is handled under 'factor' per parser.
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

			/*
			NOTE: There is no separation of concerns here; it does not make sense to handle BOOLEAN value like so separately here.
			Would be better to have a parser rule for that literal specifically and push, pop, and add as child via the other methods.
			Not implementing this anymore due to time-constraints, but would be a smart improvement for readability.

			 */
			  if (ctx.BOOLEAN() != null) {
				BoolLiteral boolValue = new BoolLiteral(ctx.BOOLEAN().getText());
				variableAssignment.addChild(boolValue);
			}

		}

		@Override
		public void exitVariabledef(ICSSParser.VariabledefContext ctx) {
			attachLatestOnStackToParent();
		}

		// If - Else statements
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
		// NOTE: So using it only for FactorContext, calling an extra function takes up more stack space, but is more readable!
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

		// Using a separate function to centralize this code.
		// I won't do specific typecasting for the type of node, but can be done with if-instanceof checks, but adding it as is, should work fine.
		private void attachLatestOnStackToParent() {
			ASTNode node = currentContainer.pop();
			currentContainer.peek().addChild(node);
		}

		public AST getAST() {
			return ast;
		}

	}