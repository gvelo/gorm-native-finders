package groovy.org.grails.plugin.nativefinders

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
