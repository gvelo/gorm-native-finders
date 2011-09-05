package org.grails.plugin.nativefinders

import grails.test.*

import org.grails.plugin.nativefinders.transform.NativeFinderTransform;

import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.expr.ClosureExpression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.classgen.GeneratorContext;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.grails.compiler.injection.ClassInjector;
import org.codehaus.groovy.grails.compiler.injection.GrailsAwareClassLoader;
import org.codehaus.groovy.ast.ClassCodeVisitorSupport;
import org.codehaus.groovy.ast.ModuleNode;
import org.codehaus.groovy.ast.Parameter;
import org.codehaus.groovy.ast.expr.ClosureExpression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.control.CompilationUnit;
import org.codehaus.groovy.control.MultipleCompilationErrorsException;
import org.codehaus.groovy.control.Phases;
import org.codehaus.groovy.control.SourceUnit;
import org.grails.plugin.nativefinders.Closure2HQL;

class NativeFinderTransformTests extends GrailsUnitTestCase {


	protected void setUp() {
		super.setUp()
	}

	protected void tearDown() {
		super.tearDown()
	}



	void testFindConstant() {

		def obj = parseAndInstance ( """
			
			def test(){
				testFind{ it.name == "someName" }
			}
			
		""" )

		obj.metaClass.static.testFind = { ClosureExpression closureExpression , ArrayList parameters ->

			assert getHQL( closureExpression ) == "from TestClass as testclass where ( testclass.name = 'someName' ) "
			assert parameters.size() == 0
		}

		obj.test()
	}




	void testFindWithParameters() {

		def obj = parseAndInstance ( """
						
			def test(){
			
				def foo = "bar"
								
				testFind{ it.name == foo }
								
			}
			
		""" )

		obj.metaClass.static.testFind = { ClosureExpression closureExpression , ArrayList parameters ->

			assert getHQL( closureExpression ) == "from TestClass as testclass where ( testclass.name = ? ) "
			assert parameters.size() == 1
			assert parameters[0] == "bar"
		}

		obj.test()
	}





	void testParametersOrder() {

		def obj = parseAndInstance ( """
						
			def test(){
			
				def first  = "first"
				def second = "second"
				def third  = "third"
								
				testFind{ it.field == first || it.field == second || it.field == third }
								
			}
			
		""" )

		obj.metaClass.static.testFind = { ClosureExpression closureExpression , ArrayList parameters ->

			assert getHQL( closureExpression ) == "from TestClass as testclass where ( ( ( testclass.field = ? ) or ( testclass.field = ? ) ) or ( testclass.field = ? ) ) "
			assert parameters.size() == 3
			assert parameters[0] == "first"
			assert parameters[1] == "second"
			assert parameters[2] == "third"
		}

		obj.test()
	}




	void testRunTimeEval1() {

		def obj = parseAndInstance ( """
						
			def test(){			
								
				testFind{ it.dateField > new Date() }
								
			}
			
		""" )

		obj.metaClass.static.testFind = { ClosureExpression closureExpression , ArrayList parameters ->

			assert getHQL( closureExpression ) == "from TestClass as testclass where ( testclass.dateField > ? ) "
			assert parameters.size() == 1
			assert parameters[0] instanceof Date
		}

		obj.test()
	}





	void testMaps() {

		def obj = parseAndInstance ( """
						
			def test( params ){
											
				testFind{ it.author == params.author && it.releaseDate > params.releaseDate }
								
			}
			
		""" )

		obj.metaClass.static.testFind = { ClosureExpression closureExpression , ArrayList parameters ->

			assert getHQL( closureExpression ) == "from TestClass as testclass where ( ( testclass.author = ? ) and ( testclass.releaseDate > ? ) ) "
			assert parameters.size() == 2
			assert parameters[0] == "Roberto Arlt"
			assert parameters[1] instanceof Date
		}

		def params = [ author:"Roberto Arlt" , releaseDate: new Date() ]

		obj.test( params )
	}



	void testInvalidRootStatement() {


		try{
			parseAndInstance ( """
					
					def test(){
						testFind{ print it }
					}
					
				""" )
		} catch ( MultipleCompilationErrorsException e ) {

			assert e.getErrorCollector().getErrorCount() == 1
			assert e.getMessage().contains( "Only binary expression are allowed in native finder closures" );
			return;
		}

		fail("MultipleCompilationErrorsException expected");
	}



	void testAssign() {


		try{
			parseAndInstance ( """
							
							def test(){
								testFind{ it.type = "book" }
							}
							
						""" )
		} catch ( MultipleCompilationErrorsException e ) {

			assert e.getErrorCollector().getErrorCount() == 1
			assert e.getMessage().contains( "Cannot assign inside a closure in native finder context" );
			return;
		}

		fail("MultipleCompilationErrorsException expected");
	}
	
	
	
	void testFindAssociations() {
		
		def obj = parseAndInstance ( """
			
			def test(){
				testFind{ it.owner.id.country == 'AU' && it.owner.id.medicareNumber == 123456 }
			}
			
		""" )
		
		obj.metaClass.static.testFind = { ClosureExpression closureExpression , ArrayList parameters ->
		
			assert getHQL( closureExpression ) == "from TestClass as testclass where ( ( testclass.country.id.owner = 'AU' ) and ( testclass.medicareNumber.id.owner = 123456 ) ) "
			assert parameters.size() == 0
		}
		
		obj.test()
	}
	



	def parseAndInstance( source ){

		def gcl = new GrailsAwareClassLoader()
		def transformer = new NativeFinderClassInjector()
		gcl.classInjectors = [transformer]as ClassInjector[]

		def cls = gcl.parseClass( source );

		return cls.newInstance()
	}


	def getHQL( closureExpression ){

		Parameter[] p = closureExpression.getParameters()

		def className

		if ( p.size() != 0 ){
			className = p[0].getType().getName()
		} else {
			className = "TestClass"
		}

		Closure2HQL closure2HQL = new Closure2HQL()
		return  closure2HQL.tranformClosureExpression (className, closureExpression)
	}
}

class NativeFinderClassInjector implements ClassInjector{

	public void performInjection(SourceUnit source, GeneratorContext context, ClassNode classNode) {

		def tranf = new NativeFinderTransform() {
					protected boolean shouldTransform( MethodCallExpression call ){
						call.getMethodAsString() == "testFind"
					}
				}

		tranf.visit(null, source );
	}

	public void performInjection(SourceUnit source, ClassNode classNode) {
		performInjection(source, null, classNode);
	}

	public boolean shouldInject(URL url) {
		return true;
	}
}