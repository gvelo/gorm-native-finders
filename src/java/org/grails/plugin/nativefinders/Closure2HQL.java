/*
* Copyright (c) 2011 the original author or authors.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/


package org.grails.plugin.nativefinders;


import org.codehaus.groovy.ast.expr.BinaryExpression;
import org.codehaus.groovy.ast.expr.ClosureExpression;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.NotExpression;
import org.codehaus.groovy.ast.expr.PropertyExpression;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.syntax.Token;
import org.codehaus.groovy.syntax.Types;
import org.grails.plugin.nativefinders.transform.ParameterPlaceholderExpression;

/**
 * This class build the HQL from the ClosureExpression
 * 
 * @author Gabriel Velo
 * 
 */
public class Closure2HQL {

	private StringBuilder sb;
	private String alias;

	private static final String DISTINCT_CLAUSE = "distinct ";
	private static final String SELECT_CLAUSE = "select ";
	private static final String AS_CLAUSE = " as ";
	private static final String FROM_CLAUSE = "from ";
	private static final String ORDER_BY_CLAUSE = "order by ";
	private static final String WHERE_CLAUSE = " where ";
	private static final String COMMA = ",";
	private static final String RIGHT_PARENTHESIS = ") ";
	private static final String LEFT_PARENTHESIS = "( ";
	private static final String SPACE = " ";
	private static final String QUESTIONMARK = "?";
	private static final String DOT = ".";
	private static final String NOT_CLAUSE = "not ";
	private static final String LOGICAL_AND = "and";
	private static final String LOGICAL_OR = "or";
	private static final String EQUAL = "=";

	public Closure2HQL() {
		sb = new StringBuilder();
	}

	public String tranformClosureExpression(String className, ClosureExpression closureExpression) {

		this.alias = className.toLowerCase();

		sb.append( FROM_CLAUSE );
		sb.append( className );
		sb.append( AS_CLAUSE );
		sb.append( alias );
		sb.append( WHERE_CLAUSE );

		ExpressionStatement exprStmt = (ExpressionStatement) ( (BlockStatement) closureExpression.getCode() ).getStatements().get( 0 );

		transformExpression( exprStmt.getExpression() );

		return sb.toString();

	}

	private void transformExpression(Expression expr) {

		if (expr instanceof PropertyExpression) {
			transformPropertyExpression( (PropertyExpression) expr );
		} else if (expr instanceof BinaryExpression) {
			transformBinaryExpression( (BinaryExpression) expr );
		} else if (expr instanceof NotExpression) {
			transformNotExpression( (NotExpression) expr );
		} else if (expr instanceof ConstantExpression) {
			transformConstantExpression( (ConstantExpression) expr );
		} else if (expr instanceof ParameterPlaceholderExpression) {
			transformParameterPlaceholderExpression( (ParameterPlaceholderExpression) expr );
		} else {
			// WTF ???
		}

	}

	private void transformBinaryExpression(BinaryExpression expr) {
		sb.append( LEFT_PARENTHESIS );
		transformExpression( expr.getLeftExpression() );
		transformToken( expr.getOperation() );
		transformExpression( expr.getRightExpression() );
		sb.append( RIGHT_PARENTHESIS );
	}

	private void transformToken(Token token) {

		switch (token.getType()) {
		case Types.COMPARE_EQUAL:
			sb.append( EQUAL );
			break;
		case Types.LOGICAL_AND:
			sb.append( LOGICAL_AND );
			break;
		case Types.LOGICAL_OR:
			sb.append( LOGICAL_OR );
			break;
		default:
			sb.append( token.getText() );
			break;
		}

		sb.append( SPACE );

	}

	private void transformNotExpression(NotExpression expr) {

		sb.append( NOT_CLAUSE );
		sb.append( LEFT_PARENTHESIS );
		transformExpression( expr.getExpression() );
		sb.append( RIGHT_PARENTHESIS );

	}

	private void transformConstantExpression(ConstantExpression expr) {

		// TODO: booleans ?? null ??

		if (expr.getValue() instanceof String) {
			sb.append( "'" );
			sb.append( expr.getValue() );
			sb.append( "'" );
		} else {
			sb.append( expr.getValue() );
		}

		sb.append( SPACE );

	}

	private void transformParameterPlaceholderExpression(ParameterPlaceholderExpression expr) {

		// TODO: ensure that the AST Tree is traversed in the same order in it
		// was constructed because parameters placeholders should appear in the
		// same order they where extracted from the ClosureExpression.

		sb.append( QUESTIONMARK );
		sb.append( SPACE );

	}

	private void transformPropertyExpression(PropertyExpression expr) {

		// At this point all properties should belongs to the domain class.
		// All other properties were converted to parameters in the AST
		// transformation.

		sb.append( alias );
		sb.append( getPropertyPath(expr) );
		
		sb.append( SPACE );

	}
	
	private String getPropertyPath( PropertyExpression expr ){
		
		return getPropertyPath( expr , new StringBuilder() );
		
	}
	
	/**
	 * Build the property path recursively ie account.owner.id.medicareNumber 
	 * @param expr
	 * @param path
	 * @return
	 */
	private String getPropertyPath( PropertyExpression expr , StringBuilder path ){
		
		path.append( DOT );
		
		path.append( expr.getPropertyAsString() );
		
		if ( expr.getObjectExpression() instanceof PropertyExpression ){
			getPropertyPath( ( PropertyExpression ) expr.getObjectExpression() , path );
		}
		
		return path.toString();
		
	}
		
		
}
