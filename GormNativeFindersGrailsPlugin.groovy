import org.grails.plugin.nativefinders.NativeFinderHandler;

import org.codehaus.groovy.ast.expr.ClosureExpression;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.GrailsDomainClass;
import org.springframework.context.ApplicationContext;

import org.hibernate.EmptyInterceptor
import org.hibernate.FlushMode
import org.hibernate.Session
import org.hibernate.SessionFactory


class GormNativeFindersGrailsPlugin {
	// the plugin version
	def version = "0.1"
	// the version or versions of Grails the plugin is designed for
	def grailsVersion = "1.3.7 > *"
	// the other plugins this plugin depends on
	def dependsOn = [:]
	// resources that are excluded from plugin packaging
	def pluginExcludes = [
		"grails-app/views/error.gsp"
	]

	// TODO Fill in these fields
	def author = "Gabriel Velo"
	def authorEmail = "gabriel.velo@gmail.com"
	def title = "Gorm Native Finders Plugin"
	def description = '''\\
the plugin allows grails developers to query domain objects using native groovy closures.
'''

	// URL to the plugin's documentation
	def documentation = "http://grails.org/plugin/gorm-native-finders"

	def doWithWebDescriptor = { xml ->
		// TODO Implement additions to web.xml (optional), this event occurs before
	}

	def doWithSpring = {
		// TODO Implement runtime spring config (optional)
	}

	def doWithDynamicMethods = { ApplicationContext ctx ->

		SessionFactory sf = getSessionFactory( ctx );

		for (GrailsDomainClass dc in application.domainClasses) {
			NativeFinderHandler nf = new NativeFinderHandler(dc,sf);
			dc.metaClass.static.find = { ClosureExpression closureExpression , ArrayList parameters -> nf.find( closureExpression , parameters )  }
			dc.metaClass.static.findAll = { ClosureExpression closureExpression , ArrayList parameters -> nf.findAll( closureExpression , parameters )  }
		}

	}

	static SessionFactory getSessionFactory( ApplicationContext ctx ){
		//TODO: Add support for multiple datasources.
		for (entry in ctx.getBeansOfType(SessionFactory)) {
			return entry.value
		}
	}

	def doWithApplicationContext = { applicationContext ->
		// TODO Implement post initialization spring config (optional)
	}

	def onChange = { event ->
		// TODO Implement code that is executed when any artefact that this plugin is
		// watching is modified and reloaded. The event contains: event.source,
		// event.application, event.manager, event.ctx, and event.plugin.
	}

	def onConfigChange = { event ->
		// TODO Implement code that is executed when the project configuration changes.
		// The event is the same as for 'onChange'.
	}
}
