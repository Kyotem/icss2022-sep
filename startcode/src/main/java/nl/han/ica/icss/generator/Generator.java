package nl.han.ica.icss.generator;

import nl.han.ica.icss.ast.*;
import nl.han.ica.icss.ast.literals.*;

public class Generator {

	// Method to generate the CSS from AST
	public String generate(AST ast) {
		StringBuilder sb = new StringBuilder();

		generateNode(ast.root, sb, 0);
		return sb.toString();
	}

	// Ident used for how much indentation to add before the rule. (Each ident corresponds to 2 spaces)
	private void generateNode(ASTNode node, StringBuilder sb, int indentLevel) {
		if (node == null) return;  // Guard

		String indent = "  ".repeat(indentLevel);  // Two spaces per indent

		if (node instanceof Stylerule) {

			Stylerule rule = (Stylerule) node;

			// Handle Selectors
			for (Selector selector : rule.selectors) {
				sb.append(indent).append(selector.toString()).append(" {\n");
			}

			// Handle Declarations (e.g., width:, color:)
			for (ASTNode child : rule.body) {
				if (child instanceof Declaration) {
					generateDeclaration((Declaration) child, sb, indentLevel + 1);
				}
			}

			sb.append(indent).append("}\n");
		}

		// Recurse
		for (ASTNode child : node.getChildren()) {

			if (child instanceof Declaration) {
				continue;
			}
			generateNode(child, sb, indentLevel);
		}
	}

	// Method to generate a single declaration (property: value;)
	private void generateDeclaration(Declaration decl, StringBuilder sb, int indentLevel) {
		String indent = "  ".repeat(indentLevel);

		sb.append(indent)
				.append(decl.property.name) // Property Name
				.append(": ")
				.append(generateExpression(decl.expression))  // Property Value
				.append(";\n");
	}

	// Convert Expression (From decl) to it's literal value.
	private String generateExpression(Expression expr) {
		if (expr instanceof Literal) {
			return generateLiteral((Literal) expr);
		}
		return "";
	}

	// Convert literals directly to correct String values
	private String generateLiteral(Literal literal) {
		if (literal instanceof PixelLiteral) {
			return ((PixelLiteral) literal).value + "px";
		}
		if (literal instanceof PercentageLiteral) {
			return ((PercentageLiteral) literal).value + "%";
		}
		if (literal instanceof ScalarLiteral) {
			return String.valueOf(((ScalarLiteral) literal).value);
		}
		if (literal instanceof ColorLiteral) {
			return ((ColorLiteral) literal).value;
		}
		if (literal instanceof BoolLiteral) {
			return String.valueOf(((BoolLiteral) literal).value);
		}
		return "ERROR (NO LITERAL FOUND)"; // Shouldn't happen... hopefully
	}
}
