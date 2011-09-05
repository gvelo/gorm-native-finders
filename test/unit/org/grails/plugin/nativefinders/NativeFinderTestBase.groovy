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


package org.grails.plugin.nativefinders

import grails.test.*

import org.codehaus.groovy.ast.ClassCodeVisitorSupport;
import org.codehaus.groovy.ast.ModuleNode;
import org.codehaus.groovy.ast.expr.ClosureExpression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.control.CompilationUnit;
import org.codehaus.groovy.control.Phases;
import org.codehaus.groovy.control.SourceUnit;

class NativeFinderTestBase extends GrailsUnitTestCase{

	public ClosureExpression getAST( String source ) {
		SourceUnit unit = SourceUnit.create("TestClass", source);
		CompilationUnit compUnit = new CompilationUnit();
		compUnit.addSource(unit);
		compUnit.compile(Phases.SEMANTIC_ANALYSIS);
		def helper = new ClosureASTHelper();
		helper.getClosureExpression( unit.getAST() );
	}
}

class ClosureASTHelper extends ClassCodeVisitorSupport {

	ClosureExpression closureExpression;

	public ClosureExpression getClosureExpression( ModuleNode module ){
		module.getClasses().each { visitClass(it) }
		return closureExpression;
	}

	public void visitMethodCallExpression(MethodCallExpression methodCall ) {

		if ( methodCall.getMethodAsString() == "testMethod" ){
			closureExpression = methodCall.getArguments().getExpression(0);
		}
	}

	protected SourceUnit getSourceUnit() {
		null
	}
}
