/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/UnitTests/JUnit4TestClass.java to edit this template
 */
package edu.kit.datamanager.metastore2.domain;

import edu.kit.datamanager.entities.PERMISSION;
import edu.kit.datamanager.repo.domain.acl.AclEntry;
import java.util.HashSet;
import java.util.Set;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 */
public class AclRecordTest {

  public AclRecordTest() {
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
   * Test of setAcl method, of class AclRecord.
   */
  @Test
  public void testSetAclWithNull() {
    System.out.println("setAcl");
    Set<AclEntry> newAclList = null;
    AclRecord instance = new AclRecord();
    instance.setAcl(newAclList);
    assertTrue(instance.getReadSids().isEmpty());
    assertNull(instance.getMetadataDocument());
    assertNull(instance.getMetadataRecord());
  }

  /**
   * Test of getReadSids method, of class AclRecord.
   */
  @Test
  public void testGetReadSidsWithoutPermission() {
    System.out.println("getReadSids");
    Set<AclEntry> newAclList = new HashSet<>();
    newAclList.add(new AclEntry("undesired", PERMISSION.NONE));
    AclRecord instance = new AclRecord();
    instance.setAcl(newAclList);
    assertTrue(instance.getReadSids().isEmpty());
    assertNull(instance.getMetadataDocument());
    assertNull(instance.getMetadataRecord());
  }

  /**
   * Test of getReadSids method, of class AclRecord.
   */
  @Test
  public void testGetReadSidsWithAdminPermission() {
    System.out.println("getReadSids");
    String sid = "admin";
    Set<AclEntry> newAclList = new HashSet<>();
    newAclList.add(new AclEntry(sid, PERMISSION.ADMINISTRATE));
    AclRecord instance = new AclRecord();
    instance.setAcl(newAclList);
    assertTrue(instance.getReadSids().size() == 1);
    assertTrue(instance.getReadSids().contains(sid));
    assertNull(instance.getMetadataDocument());
    assertNull(instance.getMetadataRecord());
  }

  /**
   * Test of getReadSids method, of class AclRecord.
   */
  @Test
  public void testGetReadSidsWithWritePermission() {
    System.out.println("getReadSids");
    String sid = "write";
    Set<AclEntry> newAclList = new HashSet<>();
    newAclList.add(new AclEntry(sid, PERMISSION.WRITE));
    AclRecord instance = new AclRecord();
    instance.setAcl(newAclList);
    assertTrue(instance.getReadSids().size() == 1);
    assertTrue(instance.getReadSids().contains(sid));
    assertNull(instance.getMetadataDocument());
    assertNull(instance.getMetadataRecord());
  }

  /**
   * Test of getReadSids method, of class AclRecord.
   */
  @Test
  public void testGetReadSidsWithReadPermission() {
    System.out.println("getReadSids");
    String sid = "read";
    Set<AclEntry> newAclList = new HashSet<>();
    newAclList.add(new AclEntry(sid, PERMISSION.READ));
    AclRecord instance = new AclRecord();
    instance.setAcl(newAclList);
    assertTrue(instance.getReadSids().size() == 1);
    assertTrue(instance.getReadSids().contains(sid));
    assertNull(instance.getMetadataDocument());
    assertNull(instance.getMetadataRecord());
  }

}
