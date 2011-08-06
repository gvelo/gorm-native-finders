package groovy.org.grails.plugin.nativefinders.transform;

import org.codehaus.groovy.ast.GroovyCodeVisitor;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.ExpressionTransformer;
import org.codehaus.groovy.ast.expr.UnaryMinusExpression;

public class ParameterPlaceholderExpression extends Expression {

	private int parameterIndex;

	public ParameterPlaceholderExpression( int parameterIndex ) {
		this.parameterIndex = parameterIndex;
	}

	public int getParameterIndex() {
		return parameterIndex;
	}

	@Override
	public Expression transformExpression(ExpressionTransformer transformer) {
		return new ParameterPlaceholderExpression( parameterIndex );
	}

	@Override
	public void visit(GroovyCodeVisitor visitor) {
		return;
	}

}
