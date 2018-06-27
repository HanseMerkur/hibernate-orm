/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.internal.entities.mapper.relation.query;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.envers.configuration.internal.AuditEntitiesConfiguration;
import org.hibernate.envers.configuration.internal.GlobalConfiguration;
import org.hibernate.envers.internal.entities.mapper.relation.MiddleComponentData;
import org.hibernate.envers.internal.entities.mapper.relation.MiddleIdData;
import org.hibernate.envers.query.internal.impl.SpecialRevisionRestrictionProvider;
import org.hibernate.envers.internal.tools.query.Parameters;
import org.hibernate.envers.internal.tools.query.QueryBuilder;
import org.hibernate.envers.strategy.AuditStrategy;
import org.hibernate.internal.util.StringHelper;

import static org.hibernate.envers.internal.entities.mapper.relation.query.QueryConstants.DEL_REVISION_TYPE_PARAMETER;
import static org.hibernate.envers.internal.entities.mapper.relation.query.QueryConstants.MIDDLE_ENTITY_ALIAS;
import static org.hibernate.envers.internal.entities.mapper.relation.query.QueryConstants.REFERENCED_ENTITY_ALIAS;
import static org.hibernate.envers.internal.entities.mapper.relation.query.QueryConstants.REFERENCED_ENTITY_ALIAS_DEF_AUD_STR;
import static org.hibernate.envers.internal.entities.mapper.relation.query.QueryConstants.REVISION_PARAMETER;

/**
 * Selects data from a relation middle-table and a related versions entity.
 *
 * @author Adam Warski (adam at warski dot org)
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 * @author Chris Cranford
 */
public final class TwoEntityQueryGenerator extends AbstractRelationQueryGenerator {
	private final MiddleIdData referencedIdData;
	private final MiddleComponentData[] componentDatas;

	public TwoEntityQueryGenerator(
			GlobalConfiguration globalCfg,
			AuditEntitiesConfiguration verEntCfg,
			AuditStrategy auditStrategy,
			String versionsMiddleEntityName,
			MiddleIdData referencingIdData,
			MiddleIdData referencedIdData,
			boolean revisionTypeInId,
			String orderBy,
			MiddleComponentData... componentData) {
		super(
				globalCfg,
				verEntCfg,
				auditStrategy,
				versionsMiddleEntityName,
				referencingIdData,
				revisionTypeInId,
				orderBy
		);

		this.referencedIdData = referencedIdData;
		this.componentDatas = componentData;

		/*
		 * The valid query that we need to create:
		 *   SELECT new list(ee, e) FROM versionsReferencedEntity e, middleEntity ee
		 *   WHERE
		 * (entities referenced by the middle table; id_ref_ed = id of the referenced entity)
		 *     ee.id_ref_ed = e.id_ref_ed AND
		 * (only entities referenced by the association; id_ref_ing = id of the referencing entity)
		 *     ee.id_ref_ing = :id_ref_ing AND
		 *
		 * (selecting e entities at revision :revision)
		 *   --> for DefaultAuditStrategy:
		 *     e.revision = (SELECT max(e2.revision) FROM versionsReferencedEntity e2
		 *       WHERE e2.revision <= :revision AND e2.id = e.id)
		 *
		 *   --> for ValidityAuditStrategy:
		 *     e.revision <= :revision and (e.endRevision > :revision or e.endRevision is null)
		 *
		 *     AND
		 *
		 * (the association at revision :revision)
		 *   --> for DefaultAuditStrategy:
		 *     ee.revision = (SELECT max(ee2.revision) FROM middleEntity ee2
		 *       WHERE ee2.revision <= :revision AND ee2.originalId.* = ee.originalId.*)
		 *
		 *   --> for ValidityAuditStrategy:
		 *     ee.revision <= :revision and (ee.endRevision > :revision or ee.endRevision is null)
		 *
		 * (only non-deleted entities and associations)
		 *     ee.revision_type != DEL AND
		 *     e.revision_type != DEL
		 */
	}

	@Override
	protected QueryBuilder buildQueryBuilderCommon(SessionFactoryImplementor sessionFactory) {
		final String originalIdPropertyName = verEntCfg.getOriginalIdPropName();
		final String eeOriginalIdPropertyPath = MIDDLE_ENTITY_ALIAS + "." + originalIdPropertyName;
		// SELECT new list(ee) FROM middleEntity ee
		QueryBuilder qb = new QueryBuilder( entityName, MIDDLE_ENTITY_ALIAS, sessionFactory );
		qb.addFrom( referencedIdData.getAuditEntityName(), REFERENCED_ENTITY_ALIAS, false );
		qb.addProjection( "new list", MIDDLE_ENTITY_ALIAS + ", " + REFERENCED_ENTITY_ALIAS, null, false );
		// WHERE
		final Parameters rootParameters = qb.getRootParameters();
		// ee.id_ref_ed = e.id_ref_ed
		referencedIdData.getPrefixedMapper().addIdsEqualToQuery(
				rootParameters, eeOriginalIdPropertyPath, referencedIdData.getOriginalMapper(),
				REFERENCED_ENTITY_ALIAS + "." + originalIdPropertyName
		);
		// ee.originalId.id_ref_ing = :id_ref_ing
		referencingIdData.getPrefixedMapper().addNamedIdEqualsToQuery( rootParameters, originalIdPropertyName, true );

		// ORDER BY
		if ( !StringHelper.isEmpty( orderBy ) ) {
			qb.addOrderFragment( REFERENCED_ENTITY_ALIAS, orderBy );
		}

		return qb;
	}

	@Override
	protected void applyValidPredicates(QueryBuilder qb, Parameters rootParameters, boolean inclusive) {
		final String revisionPropertyPath = verEntCfg.getRevisionNumberPath();
		final String originalIdPropertyName = verEntCfg.getOriginalIdPropName();
		final String eeOriginalIdPropertyPath = MIDDLE_ENTITY_ALIAS + "." + originalIdPropertyName;
		final String revisionTypePropName = getRevisionTypePath();
		// (selecting e entities at revision :revision)
		// --> based on auditStrategy (see above)
		auditStrategy.addEntityAtRevisionRestriction(
				globalCfg,
				qb,
				rootParameters,
				REFERENCED_ENTITY_ALIAS + "." + revisionPropertyPath,
				REFERENCED_ENTITY_ALIAS + "." + verEntCfg.getRevisionEndFieldName(),
				false,
				referencedIdData,
				revisionPropertyPath,
				originalIdPropertyName,
				REFERENCED_ENTITY_ALIAS,
				REFERENCED_ENTITY_ALIAS_DEF_AUD_STR,
				true
		);
		// (with ee association at revision :revision)
		// --> based on auditStrategy (see above)
		auditStrategy.addAssociationAtRevisionRestriction(
				qb,
				rootParameters,
				revisionPropertyPath,
				verEntCfg.getRevisionEndFieldName(),
				true,
				referencingIdData,
				entityName,
				eeOriginalIdPropertyPath,
				revisionPropertyPath,
				originalIdPropertyName,
				MIDDLE_ENTITY_ALIAS,
				inclusive,
				componentDatas
		);
		if (verEntCfg.isRevisionTypeInAuditTable()) {
			// ee.revision_type != DEL
			rootParameters.addWhereWithNamedParam(revisionTypePropName, "!=", DEL_REVISION_TYPE_PARAMETER);
			// e.revision_type != DEL
			rootParameters.addWhereWithNamedParam(
					REFERENCED_ENTITY_ALIAS + "." + revisionTypePropName,
					false,
					"!=",
					DEL_REVISION_TYPE_PARAMETER
			);
		}
	}

	@Override
	protected void applyValidAndRemovePredicates(QueryBuilder remQb) {
		final Parameters disjoint = remQb.getRootParameters().addSubParameters( "or" );
		// Restrictions to match all valid rows.
		final Parameters valid = disjoint.addSubParameters( "and" );
		// Restrictions to match all rows deleted at exactly given revision.
		final Parameters removed = disjoint.addSubParameters( "and" );
		final String revisionPropertyPath = verEntCfg.getRevisionNumberPath();
		final String revisionTypePropName = getRevisionTypePath();
		// Excluding current revision, because we need to match data valid at the previous one.
		applyValidPredicates(
                remQb,
				valid,
				false
        );
        if (verEntCfg.isUseGlobalRevisionId()) {
			// ee.revision = :revision
			removed.addWhereWithNamedParam(revisionPropertyPath, "=", REVISION_PARAMETER);
			// e.revision = :revision
			removed.addWhereWithNamedParam(
                    REFERENCED_ENTITY_ALIAS + "." + revisionPropertyPath, 
                    false, 
                    "=", 
                    REVISION_PARAMETER
            );
		}
		if (verEntCfg.isRevisionTypeInAuditTable()) {
			// ee.revision_type = DEL
			removed.addWhereWithNamedParam(revisionTypePropName, "=", DEL_REVISION_TYPE_PARAMETER);
			// e.revision_type = DEL
			removed.addWhereWithNamedParam(
                    REFERENCED_ENTITY_ALIAS + "." + revisionTypePropName, 
                    false, 
                    "=", 
                    DEL_REVISION_TYPE_PARAMETER
            );
		}
		if (auditStrategy instanceof SpecialRevisionRestrictionProvider){
			((SpecialRevisionRestrictionProvider)auditStrategy).setRevisionRestrictionParameter(null,valid);
			((SpecialRevisionRestrictionProvider)auditStrategy).setRevisionRestrictionParameter(REFERENCED_ENTITY_ALIAS,valid);
		}
	}
}
