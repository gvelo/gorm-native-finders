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

class NativeFindersTests extends GroovyTestCase {
    protected void setUp() {
        super.setUp()
    }

    protected void tearDown() {
        super.tearDown()
    }	

	void testYearFunc() {
	
		def book = Book.find{ Book book -> book.releaseDate.year() == 2011 }
	
		assert  book.title == "The Better Angels of Our Nature: Why Violence Has Declined"
	
	}
	
	void testYearCount(){
		
		assert Book.count{ book -> book.releaseDate.year() == 2011 } == 1
		
	}
	
	void testLike(){
		
		def book = Book.find{ book -> book.title.like("%Our%") }
		
		assert  book.title == "The Better Angels of Our Nature: Why Violence Has Declined"
		
	}
	
	void testChainFunction(){
		
		def book = Book.find{ book -> book.title.lower().like("%our%") }
		
		assert  book.title == "The Better Angels of Our Nature: Why Violence Has Declined"
		
	}
	
	void testAssociations(){
		
		def books = Book.findAll{ book -> book.author.name.lower().like("%pink%") }
		
		assert books.size() == 2
		
	}

	void testAssociationsWithItParam(){
		
		def books = Book.findAll{ it.author.name.lower().like("%pink%") }
		
		assert books.size() == 2
		
	}
	
	void testAssociationsWithParameters(){
		
		def strLike = "%pink%";
		
		def books = Book.findAll{ book -> book.author.name.lower().like( strLike ) }
		
		assert books.size() == 2
		
	}
	
}
