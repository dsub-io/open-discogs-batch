package io.dsub.discogs.batch.argument.formatter;

/**
 * Argument formatter interface to present formatter of specific argument value.
 */
public interface ArgumentFormatter {

  /**
   * Single method that performs formatting.
   *
   * @param args arguments to be evaluated.
   * @return result that is either being formatted, or ignored to be formatted.
   */
  String[] format(String[] args);
}
