/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.kit.datamanager.metastore2.domain.acl;

import edu.kit.datamanager.entities.PERMISSION;
import edu.kit.datamanager.repo.domain.acl.AclEntry;
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
public class AclEntryTest {
    
    public AclEntryTest() {
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
     * Test of hashCode method, of class AclEntry.
     */
    @Test
    public void testHashCode() {
        System.out.println("hashCode");
        AclEntry instance = new AclEntry();
        int expResult = 4934783;
        int result = instance.hashCode();
        assertEquals(expResult, result);
    }

    /**
     * Test of equals method, of class AclEntry.
     */
    @Test
    public void testEquals() {
        System.out.println("equals");
        Object obj = null;
        AclEntry instance = new AclEntry();
        boolean expResult = false;
        boolean result = instance.equals(obj);
        assertEquals(expResult, result);
        result = instance.equals("anyOtherObject");
        assertEquals(expResult, result);
        obj = new AclEntry("admin", null);
        result = instance.equals(obj);
        assertEquals(expResult, result);
        obj = new AclEntry(null, PERMISSION.ADMINISTRATE);
        result = instance.equals(obj);
        assertEquals(expResult, result);
        expResult = true;
        result = instance.equals(instance);
        assertEquals(expResult, result);
        obj = new AclEntry();
        result = instance.equals(obj);
        assertEquals(expResult, result);
        obj = new AclEntry(null, null);
        result = instance.equals(obj);
        assertEquals(expResult, result);
    }
    
}
