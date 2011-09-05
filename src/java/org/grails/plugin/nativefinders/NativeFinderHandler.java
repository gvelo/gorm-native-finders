package org.grails.plugin.nativefinders;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.IdentityHashMap;
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

	private GrailsDomainClass domainClass;
	private SessionFactory sessionFactory;
	private IdentityHashMap<ClosureExpression, String> queryCache;

	public NativeFinderHandler(GrailsDomainClass domainClass, SessionFactory sessionFactory) {
		this.domainClass = domainClass;
		this.sessionFactory = sessionFactory;
		queryCache = new IdentityHashMap<ClosureExpression, String>();
	}

	public Object find(ClosureExpression closureExpression, final ArrayList parameters) {

		HibernateTemplate hibernateTemplate = new HibernateTemplate(sessionFactory);

		final String query = getQuery(domainClass.getName(), closureExpression);

		return hibernateTemplate.execute(new HibernateCallback<Object>() {
			public Object doInHibernate(Session session) throws HibernateException, SQLException {

				Query q = session.createQuery(query);

				for (int i = 0; i < parameters.size(); i++) {
					q.setParameter(i, parameters.get(i));
				}

				// only want one result, could have used uniqueObject here
				// but it throws an exception if its not unique which is
				// undesirable
				q.setMaxResults(1);

				// TODO: add support for q.setCacheable()
				// q.setCacheable(useCache);

				List results = q.list();

				if (results.size() > 0) {
					return GrailsHibernateUtil.unwrapIfProxy(results.get(0));
				}

				return null;

			}
		});

	}

	public Object findAll(ClosureExpression closureExpression, final ArrayList parameters) {

		HibernateTemplate hibernateTemplate = new HibernateTemplate(sessionFactory);

		final String query = getQuery(domainClass.getName(), closureExpression);

		return hibernateTemplate.executeFind(new HibernateCallback<Object>() {
			public Object doInHibernate(Session session) throws HibernateException, SQLException {

				Query q = session.createQuery(query);

				for (int i = 0; i < parameters.size(); i++) {
					q.setParameter(i, parameters.get(i));
				}

				return q.list();

			}
		});

	}

	private String getQuery(String className, ClosureExpression closureExpression) {

		String query = null;

		synchronized (queryCache) {
			query = queryCache.get(closureExpression);
		}

		if (query == null) {

			Closure2HQL closure2HQL = new Closure2HQL();

			query = closure2HQL.tranformClosureExpression(className, closureExpression);

			synchronized (queryCache) {
				queryCache.put(closureExpression, query);
			}

		}

		return query;

	}

}
