/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.kit.datamanager.metastore2.domain;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author hartmann-v
 */
public class ResourceIdentifierTest {

  public final static String IDENTIFIER = "anyIdentifier";

  public ResourceIdentifierTest() {
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
   * Test of toString method, of class ResourceIdentifier.
   */
  @Test
  public void testToString() {
    System.out.println("toString");

    ResourceIdentifier instance = new ResourceIdentifier();
    String prefix = "edu.kit.datamanager.metastore2.domain.ResourceIdentifier@";
    String expResult = "[resourceIdentifier=<null>,resourceIdentifierType=<null>]";
    String result = instance.toString();
    System.out.println(result);
    assertTrue(result.startsWith(prefix));
    assertTrue(result.endsWith(expResult));
    instance.setIdentifier(IDENTIFIER);
    instance.setIdentifierType(ResourceIdentifier.IdentifierType.INTERNAL);
    expResult = "[resourceIdentifier=" + IDENTIFIER + ",resourceIdentifierType=" + ResourceIdentifier.IdentifierType.INTERNAL + "]";
    result = instance.toString();
    assertTrue(result.startsWith(prefix));
    assertTrue(result.endsWith(expResult));
  }

  /**
   * Test of hashCode method, of class ResourceIdentifier.
   */
  @Test
  public void testHashCode() {
    System.out.println("hashCode");
    int expResult = 961;
    ResourceIdentifier instance = new ResourceIdentifier();
    int result = instance.hashCode();
    assertEquals(expResult, result);
    instance.setIdentifier("x");
    instance.setIdentifierType(ResourceIdentifier.IdentifierType.INTERNAL);
    result = instance.hashCode();
    instance.setIdentifier("x");
    expResult = instance.hashCode();
    assertEquals(expResult, result);
  }

  /**
   * Test of equals method, of class ResourceIdentifier.
   */
  @Test
  public void testEquals() {
    System.out.println("equals");
    Object other = null;
    ResourceIdentifier instance1 = new ResourceIdentifier();
    ResourceIdentifier instance2 = new ResourceIdentifier();
    boolean expResult = false;
    boolean result = instance1.equals(other);
    assertEquals(expResult, result);
    expResult = true;
    result = instance1.equals(instance1);
    assertEquals(expResult, result);
    result = instance1.equals(instance2);
    assertEquals(expResult, result);
    instance1 = ResourceIdentifier.factoryInternalResourceIdentifier("hallo");
    instance2 = ResourceIdentifier.factoryInternalResourceIdentifier("hallo");
    result = instance1.equals(instance2);
    assertEquals(expResult, result);
  }

  /**
   * Test of setIdentifier method, of class ResourceIdentifier.
   */
  @Test
  public void testSetAndGetIdentifier() {
    System.out.println("setIdentifier");
    ResourceIdentifier instance = new ResourceIdentifier();
    String identifier = "identifier";
    instance.setIdentifier(identifier);
    String identifier2 = instance.getIdentifier();
    // TODO review the generated test code and remove the default call to fail.
    assertEquals(identifier, identifier2);
  }

  /**
   * Test of getIdentifierType method, of class ResourceIdentifier.
   */
  @Test
  public void testSetAndtGetIdentifierType() {
    System.out.println("getIdentifierType");
    ResourceIdentifier instance = new ResourceIdentifier();
    ResourceIdentifier.IdentifierType expResult = ResourceIdentifier.IdentifierType.BIBCODE;
    instance.setIdentifierType(ResourceIdentifier.IdentifierType.fromValue(expResult.value()));
    ResourceIdentifier.IdentifierType result = instance.getIdentifierType();
    assertEquals(expResult, result);
    try {
      ResourceIdentifier.IdentifierType.fromValue("wrongType");
      assertTrue(false);
    } catch (IllegalArgumentException iae) {
      assertTrue(true);
    }
  }

  /**
   * Test of factoryUrlResourceIdentifier method, of class ResourceIdentifier.
   */
  @Test
  public void testFactoryUrlResourceIdentifier() {
    System.out.println("factoryUrlResourceIdentifier");
    String identifier = "url";
    ResourceIdentifier expResult = new ResourceIdentifier();
    expResult.setIdentifier(identifier);
    expResult.setIdentifierType(ResourceIdentifier.IdentifierType.URL);
    ResourceIdentifier result = ResourceIdentifier.factoryUrlResourceIdentifier(identifier);
    assertEquals(expResult, result);
  }

  /**
   * Test of factoryInternalResourceIdentifier method, of class
   * ResourceIdentifier.
   */
  @Test
  public void testFactoryInternalResourceIdentifier() {
    System.out.println("factoryInternalResourceIdentifier");
    String identifier = "internal";
    ResourceIdentifier expResult = new ResourceIdentifier();
    expResult.setIdentifier(identifier);
    expResult.setIdentifierType(ResourceIdentifier.IdentifierType.INTERNAL);
    ResourceIdentifier result = ResourceIdentifier.factoryInternalResourceIdentifier(identifier);
    assertEquals(expResult, result);
  }

  /**
   * Test of factoryResourceIdentifier method, of class ResourceIdentifier.
   */
  @Test
  public void testFactoryResourceIdentifier() {
    System.out.println("factoryResourceIdentifier");
    String identifier = "bibcode";
    ResourceIdentifier.IdentifierType type = ResourceIdentifier.IdentifierType.BIBCODE;
    ResourceIdentifier expResult = new ResourceIdentifier();
    expResult.setIdentifier(identifier);
    expResult.setIdentifierType(ResourceIdentifier.IdentifierType.BIBCODE);
    ResourceIdentifier result = ResourceIdentifier.factoryResourceIdentifier(identifier, type);
    assertEquals(expResult, result);
  }

  /**
   * Test of getId method, of class ResourceIdentifier.
   */
  @Test
  public void testSetAndGetId() {
    System.out.println("getId");
    ResourceIdentifier instance = new ResourceIdentifier();
    Long expResult = 123l;
    instance.setId(expResult);
    Long result = instance.getId();
    assertEquals(expResult, result);
  }
}
