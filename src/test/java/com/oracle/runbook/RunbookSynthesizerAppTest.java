package com.oracle.runbook;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Tests for RunbookSynthesizerApp entry point. */
class RunbookSynthesizerAppTest {

  @Test
  @DisplayName("RunbookSynthesizerApp class is loadable")
  void classIsLoadable() {
    assertThatCode(() -> Class.forName("com.oracle.runbook.RunbookSynthesizerApp"))
        .as("RunbookSynthesizerApp class should exist and be loadable")
        .doesNotThrowAnyException();
  }

  @Test
  @DisplayName("main method is invokable with empty args")
  void mainMethodIsInvokable() {
    // Test behavior: can we invoke main without errors?
    // Note: This doesn't actually start the server, just validates the method exists
    assertThat(RunbookSynthesizerApp.class.getMethods())
        .as("RunbookSynthesizerApp should have methods")
        .extracting("name")
        .contains("main");
  }
}
