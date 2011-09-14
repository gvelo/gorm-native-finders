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

			assert getHQL( closureExpression ) == "from TestClass as testclass where ( testclass.name = 'someName' )"
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

			assert getHQL( closureExpression ) == "from TestClass as testclass where ( testclass.name = ? )"
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

			assert getHQL( closureExpression ) == "from TestClass as testclass where ( ( ( testclass.field = ? ) or ( testclass.field = ? ) ) or ( testclass.field = ? ) )"
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

			assert getHQL( closureExpression ) == "from TestClass as testclass where ( testclass.dateField > ? )"
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

			assert getHQL( closureExpression ) == "from TestClass as testclass where ( ( testclass.author = ? ) and ( testclass.releaseDate > ? ) )"
			assert parameters.size() == 2
			assert parameters[0] == "Roberto Arlt"
			assert parameters[1] instanceof Date
		}

		def params = [ author:"Roberto Arlt" , releaseDate: new Date() ]

		obj.test( params )
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
		
			assert getHQL( closureExpression ) == "from TestClass as testclass where ( ( testclass.owner.id.country = 'AU' ) and ( testclass.owner.id.medicareNumber = 123456 ) )"
			assert parameters.size() == 0
		}
		
		obj.test()
	}
	

	void testCount() {
		
		def obj = parseAndInstance ( """
			
			def test(){
				testFind{ it.branch == "london" && it.state == 1 }
			}
			
		""" )
		
		obj.metaClass.static.testFind = { ClosureExpression closureExpression , ArrayList parameters ->
		
			assert getHQL( closureExpression, true ) == "select count(*) from TestClass as testclass where ( ( testclass.branch = 'london' ) and ( testclass.state = 1 ) )"
			assert parameters.size() == 0
		}
		
		obj.test()
	}
	
	
	void testLikeMethodWithParam() {
		
		def obj = parseAndInstance ( """
						
			def test( params ){
											
				testFind{ it.author.like( params.author ) && it.releaseDate > params.releaseDate }
								
			}
			
		""" )

		obj.metaClass.static.testFind = { ClosureExpression closureExpression , ArrayList parameters ->

			assert getHQL( closureExpression ) == "from TestClass as testclass where ( testclass.author like ? and ( testclass.releaseDate > ? ) )"
			assert parameters.size() == 2
			assert parameters[0] == "%Borges%"
			assert parameters[1] instanceof Date
		}

		def params = [ author:"%Borges%" , releaseDate: new Date() ]

		obj.test( params )
	}
	
	
	void testMethodChain() {
		
		def obj = parseAndInstance ( """
						
			def test(){
											
				testFind{ it.author.name.substr(2,5).lower() == "est" }
								
			}
			
		""" )

		obj.metaClass.static.testFind = { ClosureExpression closureExpression , ArrayList parameters ->

			assert getHQL( closureExpression ) == "from TestClass as testclass where ( lower( substr( testclass.author.name, 2, 5 ) ) = 'est' )"
			assert parameters.size() == 0
			
		}
				
		obj.test()
	}
	
	
	
	void testMethodChainWithParam() {
		
		def obj = parseAndInstance ( """
						
			def test( params ){
											
				testFind{ it.author.name.substr(3,5).lower() ==  params.authorSubstr  && it.releaseDate.year() > params.releaseYear }
								
			}
			
		""" )

		obj.metaClass.static.testFind = { ClosureExpression closureExpression , ArrayList parameters ->

			assert getHQL( closureExpression ) == "from TestClass as testclass where ( ( lower( substr( testclass.author.name, 3, 5 ) ) = ? ) and ( year( testclass.releaseDate ) > ? ) )"
			assert parameters.size() == 2
			assert parameters[0] == "rges"
			assert parameters[1] == 1972
		}

		def params = [ authorSubstr:"rges" , releaseYear: 1972 ]

		obj.test( params )
	}



	def parseAndInstance( source ){

		def gcl = new GrailsAwareClassLoader()
		def transformer = new NativeFinderClassInjector()
		gcl.classInjectors = [transformer]as ClassInjector[]

		def cls = gcl.parseClass( source );

		return cls.newInstance()
	}


	def getHQL( closureExpression , isCount=false ){

		Parameter[] p = closureExpression.getParameters()

		def className

		if ( p.size() != 0 ){
			className = p[0].getType().getName()
		} else {
			className = "TestClass"
		}

		Closure2HQL closure2HQL = new Closure2HQL()
		return  closure2HQL.buildQuery (className, closureExpression, isCount )
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