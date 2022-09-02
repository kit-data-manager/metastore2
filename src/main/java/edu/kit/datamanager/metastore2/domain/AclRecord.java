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
package edu.kit.datamanager.metastore2.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import edu.kit.datamanager.entities.PERMISSION;
import edu.kit.datamanager.repo.domain.acl.AclEntry;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.OneToMany;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.http.MediaType;

/**
 * Record holding metadata document + list of SIDs allowed to at least read the document.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class AclRecord implements Serializable {

  public final static String RESOURCE_TYPE = "application/vnd.datamanager.acl+json";

  public final static MediaType ACL_RECORD_MEDIA_TYPE = MediaType.valueOf(RESOURCE_TYPE);

  @NotNull(message = "A list of access control entries for resticting access for READ.")
  @OneToMany(cascade = javax.persistence.CascadeType.ALL, orphanRemoval = true)
  private final Set<String> read = new HashSet<>();
  @NotNull(message = "A list of access control entries for resticting access for WRITE.")
  @OneToMany(cascade = javax.persistence.CascadeType.ALL, orphanRemoval = true)
  private final Set<String> write = new HashSet<>();
  @NotNull(message = "A list of access control entries for resticting access for ADMINISTRATION.")
  @OneToMany(cascade = javax.persistence.CascadeType.ALL, orphanRemoval = true)
  private final Set<String> admin = new HashSet<>();
  @NotBlank(message = "The metadata document.")
  private Object metadataDocument;

  /**
   * Set new access control list.
   *
   * @param newAclList new list with acls.
   */
  public void setAcl(Set<AclEntry> newAclList) {
    read.clear();
    write.clear();
    admin.clear();
    if (newAclList != null) {
      for (AclEntry item : newAclList) {
        if (item.getPermission().atLeast(PERMISSION.READ)) {
          read.add(item.getSid());
        }
        if (item.getPermission().atLeast(PERMISSION.WRITE)) {
          write.add(item.getSid());
        }
        if (item.getPermission().atLeast(PERMISSION.ADMINISTRATE)) {
          admin.add(item.getSid());
        }
      }
    }
  }
}
