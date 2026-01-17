package com.oracle.runbook;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Tests for RunbookSynthesizerApp entry point. */
class RunbookSynthesizerAppTest {

  @Test
  @DisplayName("RunbookSynthesizerApp class exists")
  void classExists() {
    assertDoesNotThrow(
        () -> Class.forName("com.oracle.runbook.RunbookSynthesizerApp"),
        "RunbookSynthesizerApp class should exist");
  }

  @Test
  @DisplayName("main method exists with correct signature")
  void mainMethodExists() throws Exception {
    Class<?> appClass = Class.forName("com.oracle.runbook.RunbookSynthesizerApp");
    Method mainMethod = appClass.getMethod("main", String[].class);

    assertNotNull(mainMethod, "main method should exist");
    assertTrue(Modifier.isPublic(mainMethod.getModifiers()), "main should be public");
    assertTrue(Modifier.isStatic(mainMethod.getModifiers()), "main should be static");
    assertEquals(void.class, mainMethod.getReturnType(), "main should return void");
  }
}
