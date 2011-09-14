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

package gorm.plugin.nativefinder

import org.grails.plugin.nativefinders.Author;
import org.grails.plugin.nativefinders.Book;

class BootStrap {
	
	def init = { servletContext ->
			
		def richard = new Author( name:"Richard Dawkins" ).save(failOnError: true)
		def carl = new Author( name:"Carl Sagan" ).save(failOnError: true)
		def steven = new Author( name:"Steven Pinker" ).save(failOnError: true)
		
		
		new Book(	author: steven, 
					title: "The Language Instinct: How the Mind Creates Language (P.S.)", 
					releaseDate:new Date().parse('yyyy/MM/dd', '2007/09/04'),
					ISBN:"0061336467").save(failOnError: true)
				  

		new Book( 	author: steven,
					title: "The Better Angels of Our Nature: Why Violence Has Declined",
					releaseDate:new Date().parse('yyyy/MM/dd', '2011/10/04'),
					ISBN:"0670022950").save(failOnError: true)
		
	  
		
	}

}
