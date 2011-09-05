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


package org.grails.plugin.nativefinders.transform;

import org.codehaus.groovy.ast.GroovyCodeVisitor;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.ExpressionTransformer;

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
