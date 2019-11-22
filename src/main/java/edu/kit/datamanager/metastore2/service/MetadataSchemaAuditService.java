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
package edu.kit.datamanager.metastore2.service;

import edu.kit.datamanager.metastore2.domain.MetadataSchemaRecord;
import edu.kit.datamanager.service.IAuditService;
import java.util.List;
import java.util.Optional;
import org.apache.commons.collections4.CollectionUtils;
import org.javers.core.Changes;
import org.javers.core.Javers;
import org.javers.core.metamodel.object.CdoSnapshot;
import org.javers.repository.jql.JqlQuery;
import org.javers.repository.jql.QueryBuilder;
import org.javers.repository.jql.ShadowScope;
import org.javers.shadow.Shadow;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 *
 * @author jejkal
 */
@Service
public class MetadataSchemaAuditService implements IAuditService<MetadataSchemaRecord>{

  @Autowired
  private Logger LOGGER;
  private final Javers javers;

  @Autowired
  public MetadataSchemaAuditService(Javers javers){
    this.javers = javers;
  }

  @Override
  public void captureAuditInformation(MetadataSchemaRecord resource, String principal){
    LOGGER.trace("Calling captureAuditInformation(MetadataSchemaRecord#{}, {}).", resource.getSchemaId(), principal);
    javers.commit(principal, resource);
    LOGGER.trace("Successfully committed audit information for resource with id {}.", resource.getSchemaId());
  }

  @Override
  public Optional<String> getAuditInformationAsJson(String resourceId, int page, int resultsPerPage){
    LOGGER.trace("Calling getAuditInformationAsJson({}, {}, {}).", resourceId, page, resultsPerPage);

    JqlQuery query = QueryBuilder.byInstanceId(resourceId, MetadataSchemaRecord.class).limit(resultsPerPage).skip(page * resultsPerPage).build();
    Changes result = javers.findChanges(query);

    LOGGER.trace("Obtained {} change elements. Returning them in serialized format.", result.size());
    return Optional.of(javers.getJsonConverter().toJson(result));
  }

  @Override
  public Optional<MetadataSchemaRecord> getResourceByVersion(String resourceId, long version){
    LOGGER.trace("Calling getResourceByVersion({}, {}).", resourceId, version);

    JqlQuery query = QueryBuilder.byInstanceId(resourceId, MetadataSchemaRecord.class).withVersion(version).withShadowScope(ShadowScope.DEEP_PLUS).build();
    LOGGER.trace("Obtaining shadows from Javers repository.");
    List<Shadow<MetadataSchemaRecord>> shadows = javers.findShadows(query);

    if(CollectionUtils.isEmpty(shadows)){
      LOGGER.warn("No version information found for resource id {}. Returning empty result.", resourceId);
      return Optional.empty();
    }

    LOGGER.trace("Shadow for resource id {} and version {} found. Returning result.", resourceId, version);
    Shadow<MetadataSchemaRecord> versionShadow = shadows.get(0);
    LOGGER.trace("Returning shadow at index 0 with commit metadata {}.", versionShadow.getCommitMetadata());
    return Optional.of(versionShadow.get());

  }

  @Override
  public long getCurrentVersion(String resourceId){
    LOGGER.trace("Calling getCurrentVersion({}).", resourceId);

    JqlQuery query = QueryBuilder.byInstanceId(resourceId, MetadataSchemaRecord.class).limit(1).build();
    LOGGER.trace("Obtaining snapshots from Javers repository.");
    List<CdoSnapshot> snapshots = javers.findSnapshots(query);

    if(CollectionUtils.isEmpty(snapshots)){
      LOGGER.warn("No version information found for resource id {}. Returning 0.", resourceId);
      return 0;
    }

    long version = snapshots.get(0).getVersion();
    LOGGER.trace("Snapshot for resource id {} found. Returning version {}.", resourceId, version);
    return version;
  }

  @Override
  public void deleteAuditInformation(String authorId, MetadataSchemaRecord resource){
    LOGGER.trace("Calling deleteAuditInformation({}, <resource>).", authorId);

    javers.commitShallowDelete(authorId, resource);
    LOGGER.trace("Shallow delete executed.");
  }
}
