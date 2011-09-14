package org.grails.plugin.nativefinders

class Author {
	
	String name
	
	static hasMany = [ books : Book ]
    static constraints = {
    }
	
}
