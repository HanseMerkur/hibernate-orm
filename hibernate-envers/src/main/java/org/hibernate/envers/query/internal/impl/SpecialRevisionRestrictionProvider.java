/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.query.internal.impl;

import org.hibernate.envers.internal.tools.query.Parameters;
import org.hibernate.query.Query;

/**
 * If an AuditStrategy is implementing this Interface,
 * the strategy will be called to set the revision restriction parameters on querying an audit entity.
 * <p>
 * Created by theOpenBit on 02.02.17.
 */
public interface SpecialRevisionRestrictionProvider {
	void setRevisionRestrictionParameter(Query auditQuery);

	/**
	 * @param parameters where to add the restriction
	 * @param entityAlias alias of entity in sql string
	 */
	void setRevisionRestrictionParameter(String entityAlias, Parameters parameters);
}
