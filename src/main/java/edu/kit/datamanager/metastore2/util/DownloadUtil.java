/*
 * Copyright 2020 KIT
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

import edu.kit.datamanager.clients.SimpleServiceClient;
import edu.kit.datamanager.exceptions.CustomInternalServerError;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;

/**
 * Utility class for downloading resources from internet.
 */
public class DownloadUtil {

  /**
   * Default value for suffix of temporary files.
   */
  public static final String DEFAULT_SUFFIX = ".tmp";
  /**
   * Default value for prefix of temporary files.
   */
  public static final String DEFAULT_PREFIX = "DownloadUtil_";
  /**
   * Logger for this class.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(DownloadUtil.class);

  private static final int MAX_LENGTH_OF_HEADER = 100;

  private static final Pattern JSON_FIRST_BYTE = Pattern.compile("(\\R\\s)*\\s*\\{\\s*\"(.|\\s)*", Pattern.MULTILINE);//^\\s{\\s*\".*");
  private static final Pattern XML_FIRST_BYTE = Pattern.compile("((.|\\s)*<\\?xml[^<]*)?\\s*<\\s*(\\w+:)?\\w+(.|\\s)*", Pattern.MULTILINE);

  /**
   * Downloads or copy the file behind the given URI and returns its path on
   * local disc. You should delete or move the file to another location afterwards.
   *
   * @param resourceURL the given URI
   * @return the path to the created file.
   */
  public static Optional<Path> downloadResource(URI resourceURL) {
    String content = null;
    Path downloadedFile = null;
    try {
      if (resourceURL != null) {
        String suffix = FilenameUtils.getExtension(resourceURL.getPath());
        suffix = suffix.trim().isEmpty() ? DEFAULT_SUFFIX : "." + suffix;
        if (resourceURL.getHost() != null) {
          content = SimpleServiceClient
                  .create(resourceURL.toString())
                  .accept(MediaType.TEXT_PLAIN)
                  .getResource(String.class);
          downloadedFile = createTempFile("download", suffix);
          FileUtils.writeStringToFile(downloadedFile.toFile(), content, StandardCharsets.UTF_8);
        } else {
          // copy local file to new place.
          File srcFile = new File(resourceURL.getPath());
          File destFile = DownloadUtil.createTempFile("local", suffix).toFile();
          FileUtils.copyFile(srcFile, destFile);
          downloadedFile = destFile.toPath();
        }
      }
    } catch (Throwable tw) {
      LOGGER.error("Error reading URI '" + resourceURL.toString() + "'", tw);
      throw new CustomInternalServerError("Error downloading resource from '" + resourceURL.toString() + "'!");
    }
    downloadedFile = fixFileExtension(downloadedFile);

    return Optional.ofNullable(downloadedFile);
  }

  /**
   * Fix extension of file if possible.
   *
   * @param pathToFile the given URI
   * @return the path to the (renamed) file.
   */
  public static Path fixFileExtension(Path pathToFile) {
    Path returnFile = pathToFile;
    Path renamedFile = pathToFile;
    try {
      if ((pathToFile != null) && (pathToFile.toFile().exists())) {
        String contentOfFile = FileUtils.readFileToString(pathToFile.toFile(), StandardCharsets.UTF_8);
        String newExtension = guessFileExtension(contentOfFile.getBytes());
        if (newExtension != null) {
          if (!pathToFile.toString().endsWith(newExtension)) {
            renamedFile = Paths.get(pathToFile.toString() + newExtension);
            FileUtils.moveFile(pathToFile.toFile(), renamedFile.toFile());
            returnFile = renamedFile;
          }
        }
      }
    } catch (IOException ex) {
      LOGGER.error("Error moving file '{}' to '{}'.", pathToFile.toString(), renamedFile.toString());
    }
    return returnFile;
  }

  /**
   * Create temporary file. Attention: The file will not be removed
   * automatically.
   *
   * @param prefix prefix of the file
   * @param suffix suffix of the file
   * @return Path to file
   * @exception CustomInternalServerError if an error occurs
   */
  public static Path createTempFile(String prefix, String suffix) {
    Path tempFile = null;
    prefix = ((prefix == null) || (prefix.trim().isEmpty())) ? DEFAULT_PREFIX : prefix;
    suffix = ((suffix == null) || (suffix.trim().isEmpty())) ? DEFAULT_SUFFIX : suffix;
    try {
      tempFile = Files.createTempFile(prefix, suffix);
    } catch (IllegalArgumentException | IOException ioe) {
      throw new CustomInternalServerError("Error creating tmp file!");
    }
    return tempFile;
  }

  /**
   * Remove temporary file.
   *
   * @param tempFile Path to file
   */
  public static void removeFile(Path tempFile) {
    LOGGER.trace("remove file --> {}", tempFile);
    try {
      Files.deleteIfExists(tempFile);
    } catch (IOException ioe) {
      throw new CustomInternalServerError("Error removing file '" + tempFile.toString() + "'!");
    }
    return;
  }

  private static String guessFileExtension(byte[] schema) {
    // Cut schema to a maximum of MAX_LENGTH_OF_HEADER characters.
    int length = schema.length > MAX_LENGTH_OF_HEADER ? MAX_LENGTH_OF_HEADER : schema.length;
    String schemaAsString = new String(schema, 0, length);
    LOGGER.trace("Guess type for '{}'", schemaAsString);

    Matcher m = JSON_FIRST_BYTE.matcher(schemaAsString);
    if (m.matches()) {
      return ".json";
    } else {
      m = XML_FIRST_BYTE.matcher(schemaAsString);
      if (m.matches()) {
        return ".xml";
      }
    }
    return null;
  }
}
