package org.log4j2.spring.boot.ansi;

/**
 * An ANSI encodable element.
 */
public interface AnsiElement {

  /**
   * @return the ANSI escape code
   */
  @Override
  String toString();

}
