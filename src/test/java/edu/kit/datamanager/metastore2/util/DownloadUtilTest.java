/*
 * Copyright 2020 hartmann-v.
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

import com.github.tomakehurst.wiremock.WireMockServer;
import com.google.common.io.Files;
import edu.kit.datamanager.exceptions.CustomInternalServerError;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Optional;
import org.apache.commons.lang3.SystemUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

/**
 *
 * @author hartmann-v
 */
@RunWith(SpringRunner.class)
@AutoConfigureWireMock(port = 0)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class DownloadUtilTest {

  int port;

  @Autowired
  WireMockServer wireMockServer;

  public DownloadUtilTest() {
  }

  @BeforeClass
  public static void setUpClass() {
  }

  @AfterClass
  public static void tearDownClass() {
  }

  @Before
  public void setUp() {
  }

  @After
  public void tearDown() {
  }

  /**
   * Test of downloadResource method, of class GemmaMapping.
   */
  @Test
  public void testDownloadResource() throws URISyntaxException {
    System.out.println("downloadResource");
    assertNotNull(new DownloadUtil());
    port = wireMockServer.port();
    System.out.println("port: " + port);
    stubFor(get(urlEqualTo("/any")).willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "text/html")
            .withBody("any content")));
    URI resourceURL = new URI("http://localhost:" + port + "/any");
    Optional<Path> result = DownloadUtil.downloadResource(resourceURL);
    assertTrue("No file available!", result.isPresent());
    assertTrue("File '" + result.get() + "' doesn't exist!", result.get().toFile().exists());
    assertTrue("Wrong suffix for file '" + result.get() + "'!", result.get().toString().endsWith(DownloadUtil.DEFAULT_SUFFIX));
    assertTrue("Can't delete file '" + result.get() + "'!", result.get().toFile().delete());
  }

  /**
   * Test of downloadResource method, of class GemmaMapping.
   */
  @Test
  public void testDownloadResourceWithPath() throws URISyntaxException {
    System.out.println("downloadResourceWithPath");
    port = wireMockServer.port();
    stubFor(get(urlEqualTo("/json")).willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody("{\"author\": \"me\",\"date\": \"today\"}")));
    URI resourceURL = new URI("http://localhost:" + port + "/json");
    Optional<Path> result = DownloadUtil.downloadResource(resourceURL);
    assertTrue("No file available!", result.isPresent());
    assertTrue("File '" + result.get() + "' doesn't exist!", result.get().toFile().exists());
    assertTrue("Wrong suffix for file '" + result.get() + "'!", result.get().toString().endsWith(".json"));
    assertTrue("Can't delete file '" + result.get() + "'!", result.get().toFile().delete());
  }

  /**
   * Test of downloadResource method, of class GemmaMapping.
   */
  @Test
  public void testDownloadInvalidResource() throws URISyntaxException {
    System.out.println("testDownloadInvalidResource");

    try {
      URI resourceURL = new URI("https://invalidhttpaddress.de");
      DownloadUtil.downloadResource(resourceURL);
      fail();
    } catch (CustomInternalServerError ie) {
      assertTrue(true);
      assertTrue(ie.getMessage().contains("Error downloading resource"));
    }
  }

  /**
   * Test of downloadResource method, of class GemmaMapping.
   */
  @Test
  public void testDownloadLocalResource() {
    System.out.println("testDownloadLocalResource");
    File srcFile = new File("src/test/resources/examples/simple.json");
    assertTrue("File doesn't exist: " + srcFile, srcFile.exists());
    URI resourceURL = srcFile.toURI();
    Optional<Path> result = DownloadUtil.downloadResource(resourceURL);
    assertTrue("No file available!", result.isPresent());
    assertTrue("File '" + result.get() + "' doesn't exist!", result.get().toFile().exists());
    assertTrue("Wrong suffix for file '" + result.get() + "'!", result.get().toString().endsWith(".json"));
    assertTrue("Can't delete file '" + result.get() + "'!", result.get().toFile().delete());
  }

  /**
   * Test of downloadResource method, of class GemmaMapping.
   */
  @Test
  public void testDownloadLocalJsonFileWithoutSuffix() throws IOException {
    System.out.println("testDownloadLocalJsonFileWithoutSuffix");
    File srcFile = new File("src/test/resources/examples/simple.json");
    assertTrue("File doesn't exist: " + srcFile, srcFile.exists());
    Path createTempFile = DownloadUtil.createTempFile(null, "nosuffix");
    Files.copy(srcFile, createTempFile.toFile());
    Optional<Path> result = DownloadUtil.downloadResource(createTempFile.toUri());
    assertTrue("No file available!", result.isPresent());
    assertTrue("File '" + result.get() + "' doesn't exist!", result.get().toFile().exists());
    assertTrue("Wrong suffix for file '" + result.get() + "'!", result.get().toString().endsWith(".json"));
    assertTrue("Can't delete file '" + result.get() + "'!", result.get().toFile().delete());
    assertTrue("Can't delete file '" + createTempFile + "'!", createTempFile.toFile().delete());
  }

  /**
   * Test of downloadResource method, of class GemmaMapping.
   */
  @Test
  public void testDownloadLocalXMLFileWithoutSuffix() throws IOException {
    System.out.println("testDownloadLocalXMLFileWithoutSuffix");
    File srcFile = new File("src/test/resources/examples/simple.xml");
    assertTrue("File doesn't exist: " + srcFile, srcFile.exists());
    Path createTempFile = DownloadUtil.createTempFile(null, "nosuffix");
    Files.copy(srcFile, createTempFile.toFile());
    Optional<Path> result = DownloadUtil.downloadResource(createTempFile.toUri());
    assertTrue("No file available!", result.isPresent());
    assertTrue("File '" + result.get() + "' doesn't exist!", result.get().toFile().exists());
    assertTrue("Wrong suffix for file '" + result.get() + "'!", result.get().toString().endsWith(".xml"));
    assertTrue("Can't delete file '" + result.get() + "'!", result.get().toFile().delete());
    assertTrue("Can't delete file '" + createTempFile + "'!", createTempFile.toFile().delete());
  }

  /**
   * Test of downloadResource method, of class GemmaMapping.
   */
  @Test
  public void testDownloadLocalResourceWithoutSuffix() {
    System.out.println("testDownloadLocalResourceWithoutSuffix");
    File srcFile = new File("src/test/resources/examples/anyContentWithoutSuffix");
    assertTrue("File doesn't exist: " + srcFile, srcFile.exists());
    Optional<Path> result = DownloadUtil.downloadResource(srcFile.getAbsoluteFile().toURI());
    assertTrue("No file available!", result.isPresent());
    assertTrue("File '" + result.get() + "' doesn't exist!", result.get().toFile().exists());
    assertTrue("Wrong suffix for file '" + result.get() + "'!", result.get().toString().endsWith(DownloadUtil.DEFAULT_SUFFIX));
    assertTrue("Can't delete file '" + result.get() + "'!", result.get().toFile().delete());
  }

  /**
   * Test of downloadResource method, of class GemmaMapping.
   */
  @Test
  public void testDownloadInvalidLocalResource() {
    System.out.println("testDownloadInvalidLocalResource");
    try {
      URI resourceURL = new File("/invalid/path/to/local/file").toURI();
      DownloadUtil.downloadResource(resourceURL);
      fail();
    } catch (CustomInternalServerError ie) {
      assertTrue(true);
      assertTrue(ie.getMessage().contains("Error downloading resource"));
    }
  }

  /**
   * Test of downloadResource method, of class GemmaMapping.
   */
  @Test
  public void testDownloadResourceNoParameter() {
    System.out.println("downloadResourceNoParameter");
    Optional<Path> result = DownloadUtil.downloadResource(null);
    assertFalse(result.isPresent());
  }

  /**
   * Test of createTempFile method, of class DownloadUtil.
   */
  @Test
  public void testCreateTempFile() {
    System.out.println("createTempFile");
    String[] prefix = {null, null, null, "", "", "", "prefix", "prefix", "prefix"};
    String[] suffix = {null, "", "suffix", null, "", "suffix", null, "", "suffix"};
    HashSet<String> allPaths = new HashSet<>();
    String path = null;
    for (int index = 0; index < prefix.length; index++) {
      Path tmpPath = DownloadUtil.createTempFile(prefix[index], suffix[index]);
      String tmpFile = tmpPath.getFileName().toString();
      path = tmpPath.getParent().toString();
      assertFalse(allPaths.contains(tmpFile));
      allPaths.add(tmpFile);
      if ((prefix[index] != null) && (!prefix[index].trim().isEmpty())) {
        assertTrue(tmpFile.startsWith(prefix[index]));
      } else {
        assertTrue(tmpFile.startsWith(DownloadUtil.DEFAULT_PREFIX));
      }
      if ((suffix[index] != null) && (!suffix[index].trim().isEmpty())) {
        assertTrue(tmpFile.endsWith(suffix[index]));
      } else {
        assertTrue(tmpFile.endsWith(DownloadUtil.DEFAULT_SUFFIX));
      }
    }
    for (String filename : allPaths) {
      DownloadUtil.removeFile(Paths.get(path, filename));
    }
  }

  /**
   * Test of createTempFile method, of class DownloadUtil.
   */
  @Test
  public void testCreateInvalidTempFile() {
    System.out.println("createTempFile");
    String[] prefix = {"/prefix", null, "/prefix"};
    String[] suffix = {null, "/suffix", "/suffix"};
    for (int index = 0; index < prefix.length; index++) {
      try {
        DownloadUtil.createTempFile(prefix[index], suffix[index]);
        fail();
      } catch (CustomInternalServerError cise) {
        assertTrue(true);
      }
    }
  }

  /**
   * Test of removeFile method, of class DownloadUtil.
   */
  @Test
  public void testRemoveFile() {
    System.out.println("removeFile");
    Path createTempFile = DownloadUtil.createTempFile("testRemoveDir", ".txt");
    try {
      DownloadUtil.removeFile(createTempFile.getParent());
      fail();
    } catch (CustomInternalServerError ie) {
      assertTrue(ie.getMessage().contains("Error removing file"));
    }
    assertTrue(createTempFile.toFile().exists());
    DownloadUtil.removeFile(createTempFile);
    assertFalse(createTempFile.toFile().exists());
  }

  /**
   * Test of fixFileExtension method, of class DownloadUtil.
   */
  @Test
  public void testFixFileExtensionXml() throws IOException {
    System.out.println("testFixFileExtensionXml");
    File srcFile = new File("src/test/resources/examples/simple.xml");
    assertTrue("File doesn't exist: " + srcFile, srcFile.exists());
    String[] extensions = {"nosuffix", "xml", ".xml ", ".xsd", ".json"};
    // skip extensions with a '.' at start. No idea why at the moment.
    // works fine in testRemoveFile()!?
    if (SystemUtils.IS_OS_WINDOWS) {
      String[] winExtensions = {"nosuffix", "xml"};
      extensions = winExtensions;
    }
    for (String extension : extensions) {
      Path createTempFile = DownloadUtil.createTempFile(null, extension);
      Files.copy(srcFile, createTempFile.toFile());
      Path result = DownloadUtil.fixFileExtension(createTempFile);
      assertTrue(result.toString().endsWith(".xml"));
      assertTrue("Can't delete file '" + result + "'!", result.toFile().delete());
    }
  }

  @Test
  public void testFixFileExtensionJson() throws IOException {
    System.out.println("testFixFileExtensionJson");
    File srcFile = new File("src/test/resources/examples/simple.json");
    assertTrue("File doesn't exist: " + srcFile, srcFile.exists());
    String[] extensions = {"nosuffix", "json", ".json ", ".xml"};
    // skip extensions with a '.' at start. No idea why at the moment.
    // works fine in testRemoveFile()!?
    if (SystemUtils.IS_OS_WINDOWS) {
      String[] winExtensions = {"nosuffix", "xml"};
      extensions = winExtensions;
    }
    for (String extension : extensions) {
      Path createTempFile = DownloadUtil.createTempFile(null, extension);
      Files.copy(srcFile, createTempFile.toFile());
      Path result = DownloadUtil.fixFileExtension(createTempFile);
      assertTrue(result.toString().endsWith(".json"));
      assertTrue("Can't delete file '" + result + "'!", result.toFile().delete());

    }
  }

  @Test
  public void testFixFileExtensionUnknown() throws IOException {
    System.out.println("testFixFileExtensionUnknown");
    File srcFile = new File("src/test/resources/examples/anyContentWithoutSuffix");
    assertTrue("File doesn't exist: " + srcFile, srcFile.exists());
    String[] extensions = {"nosuffix", "json", ".json ", ".xml"};
    // skip extensions with a '.' at start. No idea why at the moment.
    // works fine in testRemoveFile()!?
    if (SystemUtils.IS_OS_WINDOWS) {
      String[] winExtensions = {"nosuffix", "xml"};
      extensions = winExtensions;
    }
    for (String extension : extensions) {
      Path createTempFile = DownloadUtil.createTempFile(null, extension);
      Files.copy(srcFile, createTempFile.toFile());
      Path result = DownloadUtil.fixFileExtension(createTempFile);
      assertTrue(result.toString().endsWith(extension));
      assertTrue("Can't delete file '" + result + "'!", result.toFile().delete());

    }
  }

  @Test
  public void testFixFileExtensionWrongFile() {
    System.out.println("testFixFileExtensionUnknown");
    File srcFile = new File("/tmp");
    Path result = DownloadUtil.fixFileExtension(srcFile.toPath());
    assertEquals(result, srcFile.toPath());
    srcFile = new File("/invalid/path/for/file");
    result = DownloadUtil.fixFileExtension(srcFile.toPath());
    assertEquals(result, srcFile.toPath());
    result = DownloadUtil.fixFileExtension(null);
    assertNull(result);
  }

}
