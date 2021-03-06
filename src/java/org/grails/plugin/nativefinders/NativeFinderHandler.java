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

		final String query = getQuery(domainClass.getName(), closureExpression, false );

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

		final String query = getQuery(domainClass.getName(), closureExpression, false);

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
	
	public Object count(ClosureExpression closureExpression, final ArrayList parameters) {

		HibernateTemplate hibernateTemplate = new HibernateTemplate(sessionFactory);

		final String query = getQuery(domainClass.getName(), closureExpression, true);

		return hibernateTemplate.execute(new HibernateCallback<Object>() {
			public Object doInHibernate(Session session) throws HibernateException, SQLException {

				Query q = session.createQuery(query);

				for (int i = 0; i < parameters.size(); i++) {
					q.setParameter(i, parameters.get(i));
				}
				
				return q.uniqueResult();

			}
		});

	}


	private String getQuery(String className, ClosureExpression closureExpression , boolean isCount ) {

		String query = null;

		synchronized (queryCache) {
			query = queryCache.get(closureExpression);
		}

		if (query == null) {

			Closure2HQL closure2HQL = new Closure2HQL();
			
			query = closure2HQL.buildQuery( className, closureExpression, isCount );

			synchronized (queryCache) {
				queryCache.put(closureExpression, query);
			}

		}

		return query;

	}

}
