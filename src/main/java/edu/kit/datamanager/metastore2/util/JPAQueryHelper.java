/*
 * Copyright 2019 Karlsruhe Institute of Technology.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.kit.datamanager.metastore2.util;

import edu.kit.datamanager.metastore2.domain.MetadataSchemaRecord;
import java.util.Optional;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

/**
 *
 * @author jejkal
 */
public class JPAQueryHelper{

  private final EntityManager entityManager;

  /**
   * Default constructor.
   *
   * @param entityManager The entityManager in order to perform queries.
   */
  public JPAQueryHelper(EntityManager entityManager){
    this.entityManager = entityManager;
  }

  /**
   * Get a single metadata schema record for the provided schemaId and version.
   * If the version is omitted, the schema record with the highest version
   * number is returned.
   *
   * @param schemaId The schema id.
   * @param version The schema version.
   *
   * @return An optional MetadataSchemaRecord which might be empty.
   */
  public Optional<MetadataSchemaRecord> getSchemaRecordBySchemaIdAndVersion(String schemaId, Integer version){
    if(version == null){
      CriteriaBuilder qb = entityManager.getCriteriaBuilder();
      CriteriaQuery<Number> cq = qb.createQuery(Number.class);
      Root<MetadataSchemaRecord> root = cq.from(MetadataSchemaRecord.class);
      cq.select(qb.max(root.get("schemaVersion")));

      cq.where(qb.equal(root.get("schemaId"), schemaId));
      version = (Integer) entityManager.createQuery(cq).getSingleResult();
    }
    CriteriaBuilder builder = entityManager.getCriteriaBuilder();
    CriteriaQuery<MetadataSchemaRecord> query = builder.createQuery(MetadataSchemaRecord.class);

    Root<MetadataSchemaRecord> fromSchemaRecords = query.from(MetadataSchemaRecord.class);
    TypedQuery<MetadataSchemaRecord> typedQuery = entityManager.createQuery(query.where(builder.and(builder.equal(fromSchemaRecords.get("schemaId"), schemaId), builder.equal(fromSchemaRecords.get("schemaVersion"), version))));

    try{
      return Optional.of(typedQuery.getSingleResult());
    } catch(NoResultException ex){
      return Optional.empty();
    }
  }
}
