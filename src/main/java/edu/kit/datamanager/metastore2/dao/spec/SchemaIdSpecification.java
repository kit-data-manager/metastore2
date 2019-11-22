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

import edu.kit.datamanager.metastore2.domain.MetadataSchemaRecord;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

/**
 *
 * @author jejkal
 */
public class SchemaIdSpecification{

  /**
   * Hidden constructor.
   */
  private SchemaIdSpecification(){
  }

  public static Specification<MetadataSchemaRecord> toSpecification(List<String> schemaIds){
    Specification<MetadataSchemaRecord> newSpec = Specification.where(null);
    if(schemaIds == null || schemaIds.isEmpty()){
      return newSpec;
    }

    return (Root<MetadataSchemaRecord> root, CriteriaQuery<?> query, CriteriaBuilder builder) -> {

      return builder.and(root.get("schemaId").in(schemaIds));

//      CriteriaQuery<MetadataSchemaRecord> cq = builder.createQuery(MetadataSchemaRecord.class);
//      Subquery<Integer> sq = cq.subquery(Integer.class);
//      Root<MetadataSchemaRecord> s1 = cq.from(MetadataSchemaRecord.class);
//      Root<MetadataSchemaRecord> s2 = sq.from(MetadataSchemaRecord.class);
//      sq.select(builder.greatest(s2.get("version").as(Integer.class)));
//      sq.where(builder.equal(s2.get("schemaId").as(String.class), s1.get("schemaId").as(String.class)));
//      cq.where(s1.get("schemaId").in(schemaIds));
//      List<Predicate> preds = new ArrayList<>();
//      for(String schemaId : schemaIds){
//        Subquery<Integer> maxSubQuery = query.subquery(Integer.class);
//        Root<MetadataSchemaRecord> fromEntityX = maxSubQuery.from(MetadataSchemaRecord.class);
//        maxSubQuery.select(builder.greatest(fromEntityX.<Integer>get("schemaVersion"))).where(builder.equal(fromEntityX.get("schemaId"), schemaId));
//        preds.add(builder.and(builder.equal(root.get("schemaId"), schemaId), builder.equal(root.get("schemaVersion"), maxSubQuery)));
//      }
//      Subquery<Integer> maxSubQuery = query.subquery(Integer.class);
//      Root<MetadataSchemaRecord> fromEntityX = maxSubQuery.from(MetadataSchemaRecord.class);
//      maxSubQuery.select(builder.greatest(root.<Integer>get("schemaVersion")));
//      return builder.equal(root.get("schemaId"), maxSubQuery);
//      return builder.or(preds.toArray(new Predicate[]{}));
    };
  }
}
