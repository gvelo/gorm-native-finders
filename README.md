gorm-native-finders
===================

overview
--------

the plugin allows grails developers to query domain objects using native groovy closures.

examples
--------

		Book.find{ Book book -> book.author == 'Ernesto Sabato' && book.state == Book.ACTIVE }

		Book.findAll{ it.author == 'Ernesto Sabato' && it.releaseDate < params.releaseDate }


How the plugin works
--------------------

Using an AST Transformation, the AST Tree for the closure is retained in the generated class. Later , this AST tree is used in runtime to generate the HQL query.


How to use the plugin
---------------------

as usual:

		grails install-plugin gorm-native-finders


What currently works
--------------------

* Currently only  the < > <= >= != operations are supported.
* Only hibernate datasource is supported.


What is  still missing (and hopefully will be added in upcoming releases)
-------------------------------------------------------------------------

* Support for multiple datasources
* spring-data-mapping integration
* "like" and "between" operators
* Pagination and sorting
* Agregate functions
* improbe compilation error reporting.
 

Version and Compatibility
-------------------------

the plugin is tested with Grails 1.3 and Grails 2


releases notes
--------------

0.1 initial release 

        

issues.
-------




