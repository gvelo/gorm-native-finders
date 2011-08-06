package groovy.org.grails.plugin.nativefinders;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.codehaus.groovy.ast.expr.ClosureExpression;
import org.codehaus.groovy.grails.commons.GrailsDomainClass;
import org.codehaus.groovy.grails.orm.hibernate.cfg.GrailsHibernateUtil;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.HibernateTemplate;

/**
 * Handle methods invocations for native finders.
 * 
 * @author Gabriel Velo
 * 
 */
public class NativeFinderHandler {

	GrailsDomainClass domainClass;
	SessionFactory sessionFactory;

	public NativeFinderHandler( GrailsDomainClass domainClass, SessionFactory sessionFactory ) {
		this.domainClass = domainClass;
		this.sessionFactory = sessionFactory;
	}

	public Object find(ClosureExpression closureExpression, final ArrayList parameters) {

		HibernateTemplate hibernateTemplate = new HibernateTemplate( sessionFactory );

		Closure2HQL closure2HQL = new Closure2HQL();

		// TODO: as closureExpression is cached in the caller's class , we could use a identity
		// map to cache the generated queries to avoid string build overhead.

		final String query = closure2HQL.tranformClosureExpression( domainClass.getName(), closureExpression );

		return hibernateTemplate.execute( new HibernateCallback<Object>() {
			public Object doInHibernate(Session session) throws HibernateException, SQLException {

				Query q = session.createQuery( query );

				for (int i = 0; i < parameters.size(); i++) {
					q.setParameter( i, parameters.get( i ) );
				}

				// only want one result, could have used uniqueObject here
				// but it throws an exception if its not unique which is undesirable
				q.setMaxResults( 1 );

				// TODO: add support for q.setCacheable()
				// q.setCacheable(useCache);

				List results = q.list();

				if (results.size() > 0) {
					return GrailsHibernateUtil.unwrapIfProxy( results.get( 0 ) );
				}

				return null;

			}
		} );

	}

	public Object findAll(ClosureExpression closureExpression, final ArrayList parameters) {

		HibernateTemplate hibernateTemplate = new HibernateTemplate( sessionFactory );

		Closure2HQL closure2HQL = new Closure2HQL();

		// TODO: as closureExpression is cached in the caller's class , we could use a identity
		// map to cache the generated queries to avoid string build overhead.

		final String query = closure2HQL.tranformClosureExpression( domainClass.getName(), closureExpression );

		return hibernateTemplate.executeFind( new HibernateCallback<Object>() {
			public Object doInHibernate(Session session) throws HibernateException, SQLException {

				Query q = session.createQuery( query );

				for (int i = 0; i < parameters.size(); i++) {
					q.setParameter( i, parameters.get( i ) );
				}

				return q.list();

			}
		} );

	}

}
