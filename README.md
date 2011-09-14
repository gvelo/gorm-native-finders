gorm-native-finders
===================

overview
--------

the plugin allows grails developers to query domain objects using native groovy closures.

examples
--------

        Book.findAll{ book -> book.author.name.lower().like("%dawkins%") && book.state == Book.ACTIVE }
        
        Book.count{ book -> book.author.name.like("%Dawkins%") && book.releaseDate.year() > 2009  }


How the plugin works
--------------------

Using an AST Transformation, the AST Tree for the closure is retained in the generated class. Later , this AST tree is used in runtime to generate the HQL query.


How to use the plugin
---------------------

as usual:

		grails install-plugin gorm-native-finders


What currently works
--------------------

* mathematical operators: +, -, *, /
* binary comparison operators: =, >=, <=, <>, !=, like
* logical operations &&, ||, !
* Parentheses ( ) that indicates grouping 
* second(...), minute(...), hour(...), day(...), month(...), and year(...) 
* Any function or operator defined by EJB-QL 3.0: substring(), trim(), lower(), upper(), length(), locate(), abs(), sqrt(), bit_length(), mod()
* str() for converting numeric or temporal values to a readable string 
* Only hibernate datasource is supported.


What is still missing (and hopefully will be added in upcoming releases)
-------------------------------------------------------------------------

* Support for multiple datasources
* spring-data-mapping integration
* Pagination and sorting
* improve compilation error reporting.
 

Version and Compatibility
-------------------------

the plugin is tested with Grails 1.3 and Grails 2


releases notes
--------------

0.2
* add support for HQL functions ( substring(), trim(), lower(), day() ... etc )
* add count method e.g. Account.count{ account -> account.branch == "London" && account.state == 1 }
* add support for implicit joins e.g. find { Account account -> account.owner.id.medicareNumber = 123456 }

0.1 
* initial release 

        

issues.
-------

http://jira.grails.org/browse/GPGORMNATIVEFIND


