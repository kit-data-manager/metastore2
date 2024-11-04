/*
 * Copyright 2018 Karlsruhe Institute of Technology.
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
package edu.kit.datamanager.metastore2.dao.spec;

import edu.kit.datamanager.repo.domain.DataResource;
import edu.kit.datamanager.repo.domain.RelatedIdentifier;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Root;
import org.datacite.schema.kernel_4.Resource.AlternateIdentifiers.AlternateIdentifier;
import org.springframework.data.jpa.domain.Specification;

/**
 *
 * @author jejkal
 */
public class RelatedIdentifier4SchemaSpec {

  /**
   * Hidden constructor.
   */
  private RelatedIdentifier4SchemaSpec() {
  }

  public static Specification<DataResource> toSpecification(final RelatedIdentifier.RELATION_TYPES relationType, final String... identifierValues) {
    Specification<DataResource> newSpec = Specification.where(null);
    if (identifierValues == null || identifierValues.length == 0) {
      return newSpec;
    }

    return (Root<DataResource> root, CriteriaQuery<?> query, CriteriaBuilder builder) -> {
      query.distinct(true);

      //join dataresource table with alternate identifiers table
      Join<DataResource, AlternateIdentifier> altJoin = root.join("relatedIdentifiers", JoinType.INNER);
      //get all alternate identifiers NOT of type INTERNAL with one of the provided values
      return builder.
              and(altJoin.get("value").
                      in((Object[]) identifierValues)), altJoin.get("relationType").in((Object[]) identifierValues)));
    };
  }
}
