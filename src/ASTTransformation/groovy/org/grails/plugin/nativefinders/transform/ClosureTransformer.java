package groovy.org.grails.plugin.nativefinders.transform;

import java.util.ArrayList;
import java.util.List;

import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.Parameter;
import org.codehaus.groovy.ast.VariableScope;
import org.codehaus.groovy.ast.expr.ArgumentListExpression;
import org.codehaus.groovy.ast.expr.BinaryExpression;
import org.codehaus.groovy.ast.expr.CastExpression;
import org.codehaus.groovy.ast.expr.ClassExpression;
import org.codehaus.groovy.ast.expr.ClosureExpression;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.ConstructorCallExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.ListExpression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.expr.NotExpression;
import org.codehaus.groovy.ast.expr.PropertyExpression;
import org.codehaus.groovy.ast.expr.VariableExpression;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codehaus.groovy.syntax.Token;

/**
 * Build a AST tree for reconstruct the ClossureExpression at runtime.
 * 
 * @author gvelo
 * 
 */
public class ClosureTransformer {

	private Expression closureASTBuilderExpression;
	private ArrayList<Expression> runtimeEvaluatedParameters;

	// parameters from Closure being tranformed.
	private Parameter[] closureParameters;

	public ClosureTransformer() {
		runtimeEvaluatedParameters = new ArrayList<Expression>();
	}

	public Expression getClosureASTBuilderExpression() {
		return closureASTBuilderExpression;
	}

	public List<Expression> getRuntimeEvaluatedParameters() {
		return runtimeEvaluatedParameters;
	}

	public void transformClosureExpression(ClosureExpression closure) {

		// ClosureExpression(Parameter[] parameters, Statement code)

		ClassNode classNode = ClassHelper.make( ClosureExpression.class );
		ArgumentListExpression arguments = new ArgumentListExpression();
		closureParameters = closure.getParameters();
		arguments.addExpression( transformParameters( closure.getParameters() ) );

		// TODO: add compiler error handling.
		// TODO: should we validate closure structure ??? { binaryExpr } or { NotExpression( BinaryExpression ) }
		arguments.addExpression( transformStatement( closure.getCode() ) );

		closureASTBuilderExpression = new ConstructorCallExpression( classNode, arguments );

	}

	private boolean isClosureParameter(Expression expression) {

		String variableName = null;

		if (expression instanceof PropertyExpression) {

			PropertyExpression property = (PropertyExpression) expression;

			if (property.getObjectExpression() instanceof VariableExpression) {
				variableName = ( (VariableExpression) property.getObjectExpression() ).getName();
			} else {
				return false;
			}

		} else if (expression instanceof VariableExpression) {

			variableName = ( (VariableExpression) expression ).getName();

		} else {
			return false;
		}

		if ("it".equals( variableName )) {
			return true;
		}

		for (Parameter parameter : closureParameters) {

			if (parameter.getName().equals( variableName )) {
				return true;
			}

		}

		return false;

	}

	private Expression transformParameters(Parameter[] parameters) {

		ListExpression parametersList = new ListExpression();

		for (Parameter parameter : parameters) {
			parametersList.addExpression( transformParameter( parameter ) );
		}

		return new CastExpression( ClassHelper.make( Parameter[].class ), parametersList );

	}

	private Expression transformParameter(Parameter parameter) {

		// public Parameter(ClassNode type, String name)

		ClassNode classNode = ClassHelper.make( Parameter.class );
		ArgumentListExpression arguments = new ArgumentListExpression();

		// Parameter Type
		arguments.addExpression( transformClassNode( parameter.getType() ) );

		// parameter Name
		arguments.addExpression( new ConstantExpression( parameter.getName() ) );

		return new ConstructorCallExpression( classNode, arguments );

	}

	private Expression transformClassNode(ClassNode classNode) {

		// ClassHelper.make(String.class)

		ClassExpression objectExpression = new ClassExpression( new ClassNode( ClassHelper.class ) );
		ConstantExpression method = new ConstantExpression( "make" );
		ArgumentListExpression arguments = new ArgumentListExpression();
		PropertyExpression property = new PropertyExpression( new ClassExpression( classNode ), new ConstantExpression( "class" ) );
		arguments.addExpression( property );
		return new MethodCallExpression( objectExpression, method, arguments );

	}

	private Expression transformExpression(Expression expression) {

		if (expression instanceof BinaryExpression) {
			return transformBinaryExpression( (BinaryExpression) expression );
		} else if (expression instanceof ConstantExpression) {
			return transformConstantExpression( (ConstantExpression) expression );
		} else if (expression instanceof VariableExpression) {
			return transformVariableExpression( (VariableExpression) expression );
		} else if (expression instanceof PropertyExpression) {
			return transformPropertyExpression( (PropertyExpression) expression );
		} else if (expression instanceof NotExpression) {
			return transformNotExpression( (NotExpression) expression );
		} else {
			// TODO: Add compiler error reporting.
			System.out.println( "*** Unknow expression: " + expression );
		}

		return null;

	}

	private Expression transformNotExpression(NotExpression notExpression) {

		// public NotExpression(Expression expression)

		ClassNode classNode = ClassHelper.make( NotExpression.class );
		ArgumentListExpression arguments = new ArgumentListExpression();
		arguments.addExpression( transformExpression( notExpression.getExpression() ) );
		return new ConstructorCallExpression( classNode, arguments );

	}

	private Expression transformConstantExpression(ConstantExpression constantExpression) {

		// public ConstantExpression(Object value, boolean keepPrimitive)

		ClassNode classNode = ClassHelper.make( ConstantExpression.class );
		ArgumentListExpression arguments = new ArgumentListExpression();
		arguments.addExpression( transformConstantObject( constantExpression.getValue() ) );
		return new ConstructorCallExpression( classNode, arguments );

	}

	private Expression transformConstantObject(Object o) {

		ClassNode classNode = ClassHelper.make( o.getClass() );
		ArgumentListExpression arguments = new ArgumentListExpression();
		arguments.addExpression( new ConstantExpression( o ) );
		return new ConstructorCallExpression( classNode, arguments );

	}

	private Expression transformVariableExpression(VariableExpression variableExpression) {

		// public VariableExpression(String variable)

		ClassNode classNode = ClassHelper.make( VariableExpression.class );
		ArgumentListExpression arguments = new ArgumentListExpression();
		arguments.addExpression( new ConstantExpression( variableExpression.getName() ) );
		return new ConstructorCallExpression( classNode, arguments );

	}

	private Expression transformPropertyExpression(PropertyExpression propertyExpression) {

		// public PropertyExpression(Expression objectExpression, Expression property)

		ClassNode classNode = ClassHelper.make( PropertyExpression.class );
		ArgumentListExpression arguments = new ArgumentListExpression();
		arguments.addExpression( transformExpression( propertyExpression.getObjectExpression() ) );
		arguments.addExpression( transformExpression( propertyExpression.getProperty() ) );
		return new ConstructorCallExpression( classNode, arguments );

	}

	private Expression transformBinaryExpression(BinaryExpression binaryExpression) {

		// public BinaryExpression(Expression leftExpression, Token operation, Expression rightExpression)

		ClassNode classNode = ClassHelper.make( BinaryExpression.class );
		ArgumentListExpression arguments = new ArgumentListExpression();
		arguments.addExpression( tranformBinaryOperand( binaryExpression.getLeftExpression() ) );
		arguments.addExpression( transformToken( binaryExpression.getOperation() ) );
		arguments.addExpression( tranformBinaryOperand( binaryExpression.getRightExpression() ) );
		return new ConstructorCallExpression( classNode, arguments );

	}

	private Expression tranformBinaryOperand(Expression expression) {

		if (expression instanceof BinaryExpression || expression instanceof NotExpression || expression instanceof ConstantExpression || isClosureParameter( expression )) {
			return transformExpression( expression );
		}

		return createQueryParameter( expression );

	}

	private Expression createQueryParameter(Expression expression) {

		// public ParameterPlaceholderExpression( int parameterIndex )

		ClassNode classNode = ClassHelper.make( ParameterPlaceholderExpression.class );
		ArgumentListExpression arguments = new ArgumentListExpression();
		arguments.addExpression( new ConstantExpression( runtimeEvaluatedParameters.size() ) );
		runtimeEvaluatedParameters.add( expression );
		return new ConstructorCallExpression( classNode, arguments );

	}

	private Expression transformMethodCallExpression(MethodCallExpression methodCallExpression) {

		// public MethodCallExpression(Expression objectExpression, Expression method, Expression arguments)

		ClassNode classNode = ClassHelper.make( MethodCallExpression.class );
		ArgumentListExpression arguments = new ArgumentListExpression();
		arguments.addExpression( transformExpression( methodCallExpression.getObjectExpression() ) );
		arguments.addExpression( transformExpression( methodCallExpression.getMethod() ) );
		arguments.addExpression( transformExpression( methodCallExpression.getArguments() ) );
		return new ConstructorCallExpression( classNode, arguments );

	}

	private Expression transformToken(Token token) {

		// public Token( int type, String text, int startLine, int startColumn )

		ClassNode classNode = ClassHelper.make( Token.class );
		ArgumentListExpression arguments = new ArgumentListExpression();
		arguments.addExpression( new ConstantExpression( token.getType() ) );
		arguments.addExpression( new ConstantExpression( token.getText() ) );
		arguments.addExpression( new ConstantExpression( token.getStartLine() ) );
		arguments.addExpression( new ConstantExpression( token.getStartColumn() ) );
		return new ConstructorCallExpression( classNode, arguments );

	}

	private Expression transformStatement(Statement statement) {

		if (statement instanceof BlockStatement) {
			return transformBlockStatement( (BlockStatement) statement );
		} else if (statement instanceof ExpressionStatement) {
			return tranformExpressionStatement( (ExpressionStatement) statement );
		} else {
			// TODO: Add compiler error reporting.
			System.out.println( " *** Unknow statement type" + statement );
			return null;
		}

	}

	private Expression tranformExpressionStatement(ExpressionStatement expressionStatement) {

		// public ExpressionStatement(Expression expression)

		ClassNode classNode = ClassHelper.make( ExpressionStatement.class );
		ArgumentListExpression arguments = new ArgumentListExpression();
		arguments.addExpression( transformExpression( expressionStatement.getExpression() ) );
		return new ConstructorCallExpression( classNode, arguments );

	}

	private Expression transformBlockStatement(BlockStatement blockStatement) {

		// public BlockStatement(Statement[] statements, VariableScope scope)

		ClassNode classNode = ClassHelper.make( BlockStatement.class );
		ArgumentListExpression arguments = new ArgumentListExpression();

		// Statement[] statements
		arguments.addExpression( transformStatements( blockStatement.getStatements() ) );

		// VariableScope scope
		arguments.addExpression( transformVariableScope( blockStatement.getVariableScope() ) );

		return new ConstructorCallExpression( classNode, arguments );

	}

	private Expression transformVariableScope(VariableScope variableScope) {

		ClassNode variableExprClassNode = ClassHelper.make( VariableScope.class );
		return new ConstructorCallExpression( variableExprClassNode, new ArgumentListExpression() );

	}

	private Expression transformStatements(List<Statement> statements) {

		ListExpression statementList = new ListExpression();

		for (Statement statement : statements) {
			statementList.addExpression( transformStatement( statement ) );
		}

		return new CastExpression( ClassHelper.make( Statement[].class ), statementList );

	}

}
