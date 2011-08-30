package groovy.org.grails.plugin.nativefinders

import grails.test.*

import groovy.org.grails.plugin.nativefinders.Closure2HQL;

import org.codehaus.groovy.ast.ClassCodeVisitorSupport;
import org.codehaus.groovy.ast.ModuleNode;
import org.codehaus.groovy.ast.Parameter;
import org.codehaus.groovy.ast.expr.ClosureExpression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.control.CompilationUnit;
import org.codehaus.groovy.control.Phases;
import org.codehaus.groovy.control.SourceUnit;


class Closure2HQLTests extends NativeFinderTestBase {

	protected void setUp() {
		super.setUp()
	}

	protected void tearDown() {
		super.tearDown()
	}


	void testConstantString() {

		def source = """
		testMethod{ it.name == "nameStr" }
		"""

		assert  getHQL(source) == "from TestClass as testclass where ( testclass.name = 'nameStr' ) "
	}


	void testConstantBoolean() {

		def source = """
		testMethod{ it.name == true }
		"""

		assert  getHQL(source) == "from TestClass as testclass where ( testclass.name = true ) "
	}


	void testConstantNull() {

		def source = """
		testMethod{ it.name == null }
		"""

		assert  getHQL(source) == "from TestClass as testclass where ( testclass.name = null ) "
	}


	void testVariable() {

		def source = """
		testMethod{ it.name == foo }
		"""

		// variable expressions should never be transformed because are evaluated
		// at runtime and passed as parameters.
		assert  getHQL(source) != "from TestClass as testclass where ( testclass.name = foo )"
	}


	void testConstantNumeric() {

		def source = """
		testMethod{ it.value >= 1000 }
		"""

		assert  getHQL(source) == "from TestClass as testclass where ( testclass.value >= 1000 ) "
	}


	void testNotExpression() {

		def source = """
		testMethod{ !( it.value > 1000) }
		"""

		assert  getHQL(source) == "from TestClass as testclass where not ( ( testclass.value > 1000 ) ) "
	}


	void testBinaryExpressions() {

		def source = """
		testMethod{ it.price > 1000 && it.type == "house" && it.state==10 }
		"""

		assert  getHQL(source) == "from TestClass as testclass where ( ( ( testclass.price > 1000 ) and ( testclass.type = 'house' ) ) and ( testclass.state = 10 ) ) "
	}


	void testMethodParameters() {

		def source = """
		class Book{}
		testMethod{ Book book -> book.author == "Sabato" || book.printYear > 1970 }
		"""

		assert  getHQL(source) == "from Book as book where ( ( book.author = 'Sabato' ) or ( book.printYear > 1970 ) ) "
	}
	
	void testAssociations() {
		
				def source = """
				class Account{}
				testMethod{ Account account -> account.owner.id.country == 'AU' && account.owner.id.medicareNumber == 123456 }
				"""
		
				assert  getHQL(source) == "from Account as account where ( ( account.country.id.owner = 'AU' ) and ( account.medicareNumber.id.owner = 123456 ) ) "
			}


	public String getHQL( String source ){

		def closureExpression = getAST ( source );

		Parameter[] p = closureExpression.getParameters()

		def className;

		if ( p.size() != 0 ){
			className = p[0].getType().getName()
		} else {
			className = "TestClass"
		}

		Closure2HQL closure2HQL = new Closure2HQL();
		return  closure2HQL.tranformClosureExpression (className, closureExpression)
	}
}
