package org.basex.util.options;

import java.util.*;

/**
 * Numbers option.
 *
 * @author BaseX Team 2005-13, BSD License
 * @author Christian Gruen
 */
public final class NumbersOption extends Option {
  /** Default value. */
  public final int[] value;

  /**
   * Default constructor.
   * @param n name
   * @param v value
   */
  public NumbersOption(final String n, final int... v) {
    super(n);
    value = v;
  }

  @Override
  public int[] value() {
    return value;
  }

  @Override
  public String toString() {
    return new StringBuilder(name()).append(Arrays.asList(value)).toString();
  }
}