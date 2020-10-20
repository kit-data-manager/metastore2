/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.kit.datamanager.metastore2.exception;

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
public class JsonValidationExceptionTest {
  
  public JsonValidationExceptionTest() {
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

  @Test
  public void testAllConstructors() {
    String messageOne = "any message";
    String messageTwo = "any other message";
    JsonValidationException exception = new JsonValidationException();
    assertNotNull(exception);
    assertNull(exception.getMessage());
    JsonValidationException exceptionWithMessage = new JsonValidationException(messageOne);
    assertNotNull(exceptionWithMessage);
    assertEquals(messageOne, exceptionWithMessage.getMessage());
    JsonValidationException exceptionWithCause = new JsonValidationException(exception);
    assertNotNull(exceptionWithCause);
    assertNull(exception.getMessage());
    assertEquals(exception, exceptionWithCause.getCause());
    JsonValidationException exceptionWithMessageAndCause = new JsonValidationException(messageTwo,exception);
    assertNotNull(exceptionWithMessageAndCause);
    assertNotNull(exceptionWithMessageAndCause.getMessage());
    assertEquals(exception, exceptionWithMessageAndCause.getCause());
    assertEquals(messageTwo, exceptionWithMessageAndCause.getMessage());
    
    assertTrue(true);
  }
  
}
