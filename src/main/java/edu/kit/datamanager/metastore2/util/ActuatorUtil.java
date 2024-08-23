/*
 * Copyright 2023 Karlsruhe Institute of Technology.
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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.kit.datamanager.clients.SimpleServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for actuators collecting information details about local
 * directory.
 */
public class ActuatorUtil {

  /**
   * Logger
   */
  private static final Logger LOG = LoggerFactory.getLogger(ActuatorUtil.class);

  private ActuatorUtil() {
    //Utility class
  }

  /**
   * Determine all details for given directory.
   *
   * @param pathUrl URL of directory
   * @return Map with details.
   */
  public static final Map<String, String> testDirectory(URL pathUrl) {
    Map<String, String> properties = new HashMap<>();
    try {
      Path path = Paths.get(pathUrl.toURI());
      properties = determineDetailsForPath(path);
    } catch (URISyntaxException ex) {
      LOG.error("Invalid base path uri of '" + pathUrl + "'.", ex);
    }
    return properties;
  }

  /**
   * Determine all details for given directory.
   *
   * @param elasticUrl URL of directory
   * @return Map with details.
   */
  public static final Map<String, String> testElastic(URL elasticUrl) {
    Map<String, String> properties = new HashMap<>();
    try {
      SimpleServiceClient client = SimpleServiceClient.create(elasticUrl.toString());
      String response = client.getResource(String.class);
      ObjectMapper objectMapper = new ObjectMapper();
      JsonNode node = objectMapper.readTree(response);
      properties.put("tagline", node.get("tagline").asText());
    } catch (Throwable ex) {
      LOG.error("Invalid elastic uri of '" + elasticUrl.toString() + "'.", ex);
    }
    return properties;
  }

  /**
   * Determine all details for given directory.
   *
   * @param path URL of directory
   * @return Map with details.
   */
  private static final Map<String, String> determineDetailsForPath(Path path) {
    Map<String, String> properties = new HashMap<>();
    String totalSpace;
    String freeSpace;
    Path probe = Paths.get(path.toString(), "probe.txt");
    try {
      probe = Files.createFile(probe);
      Files.write(probe, "Success".getBytes(StandardCharsets.UTF_8));
      File repoDir = path.toFile();
      double total = repoDir.getTotalSpace();
      double free = repoDir.getFreeSpace();
      totalSpace = String.format("%.2f GB", total / 1073741824);
      freeSpace = String.format("%.2f GB (%.0f%%)", (double) repoDir.getFreeSpace() / 1073741824, free * 100.0 / total);
      properties.put("Total space", totalSpace);
      properties.put("Free space", freeSpace);

    } catch (IOException ioe) {
      LOG.error("Failed to check repository folder at '" + path + "'.");
    } finally {
      try {
        Files.deleteIfExists(probe);
      } catch (IOException ignored) {
        LOG.error("Can't delete file '{}'.", probe);
      }
    }
    return properties;
  }

}
