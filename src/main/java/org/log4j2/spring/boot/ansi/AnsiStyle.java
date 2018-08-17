package org.log4j2.spring.boot.ansi;

/**
 * {@link AnsiElement Ansi} styles.
 */
public enum AnsiStyle implements AnsiElement {

  NORMAL("0"),

  BOLD("1"),

  FAINT("2"),

  ITALIC("3"),

  UNDERLINE("4");

  private final String code;

  AnsiStyle(String code) {
    this.code = code;
  }

  @Override
  public String toString() {
    return this.code;
  }

}
