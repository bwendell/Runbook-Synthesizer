package com.oracle.runbook;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Tests for RunbookSynthesizerApp entry point. */
class RunbookSynthesizerAppTest {

  @Test
  @DisplayName("RunbookSynthesizerApp class exists")
  void classExists() {
    assertThatCode(() -> Class.forName("com.oracle.runbook.RunbookSynthesizerApp"))
        .as("RunbookSynthesizerApp class should exist")
        .doesNotThrowAnyException();
  }

  @Test
  @DisplayName("main method exists with correct signature")
  void mainMethodExists() throws Exception {
    Class<?> appClass = Class.forName("com.oracle.runbook.RunbookSynthesizerApp");
    Method mainMethod = appClass.getMethod("main", String[].class);

    assertThat(mainMethod).as("main method should exist").isNotNull();
    assertThat(Modifier.isPublic(mainMethod.getModifiers())).as("main should be public").isTrue();
    assertThat(Modifier.isStatic(mainMethod.getModifiers())).as("main should be static").isTrue();
    assertThat(mainMethod.getReturnType()).as("main should return void").isEqualTo(void.class);
  }
}
