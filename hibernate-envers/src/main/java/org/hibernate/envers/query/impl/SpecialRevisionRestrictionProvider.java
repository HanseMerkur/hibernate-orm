package org.hibernate.envers.query.impl;

import org.hibernate.Query;
import org.hibernate.envers.tools.query.Parameters;

/**
 * If an AuditStrategy is implementing this Interface,
 * the strategy will be called to set the revision restriction parameters on querying an audit entity.
 *
 * Created by theOpenBit on 02.02.17.
 */
public interface SpecialRevisionRestrictionProvider {
    void setRevisionRestrictionParameter(Query auditQuery);

    /**
     *
     * @param parameters where to add the restriction
     * @param entityAlias alias of entity in sql string
     */
    void setRevisionRestrictionParameter( String entityAlias, Parameters parameters);
}
