package org.grails.plugin.nativefinders

class Book {

	String title
	Date releaseDate
	String ISBN
	
	static belongsTo = [author:Author]

    static constraints = {
    }
	
}
