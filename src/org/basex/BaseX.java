package org.basex;

import static org.basex.Text.*;
import org.basex.core.AbstractProcess;
import org.basex.core.Command;
import org.basex.core.Context;
import org.basex.core.Prop;
import org.basex.util.Token;
import org.basex.util.TokenBuilder;

/**
 * This is the starter class for the stand-alone console mode.
 * It overwrites the {@link BaseXClient} to allow local database
 * operations.
 * Add the '-h' option to get a list on all available command-line arguments.
 *
 * @author Workgroup DBIS, University of Konstanz 2005-08, ISC License
 * @author Christian Gruen
 */
public final class BaseX extends BaseXClient {
  /** Database Context. */
  private final Context context = new Context();

  /**
   * Main method, launching the stand-alone console mode.
   * Use <code>-h</code> to get a list of all available command-line
   * arguments.
   * @param args command-line arguments
   */
  public static void main(final String[] args) {
    new BaseX().init(args);
  }

  @Override
  void init(final String[] args) {
    standalone = true;
    super.init(args);
  }

  @Override
  synchronized void quit(final boolean force) {
    super.quit(force);
    if(!force) Prop.write();
    context.close();
  }

  @Override
  protected AbstractProcess getProcess(final Command cmd) {
    return cmd.proc(context);
  }

  /**
   * Global method, replacing all % characters
   * (see {@link TokenBuilder#add(String, Object...)} for details.
   * @param str string to be extended
   * @param ext text text extensions
   * @return token
   */
  public static byte[] inf(final Object str, final Object... ext) {
    final TokenBuilder info = new TokenBuilder();
    info.add(str, ext);
    return info.finish();
  }

  /**
   * Global method, replacing all % characters
   * (see {@link TokenBuilder#add(String, Object...)} for details.
   * @param str string to be extended
   * @param ext text text extensions
   * @return extended string
   */
  public static String info(final Object str, final Object... ext) {
    return Token.string(inf(str, ext));
  }

  /**
   * Global method for printing debug information if the
   * {@link Prop#debug} flag is set.
   * @param str debug string
   * @param ext text optional extensions
   */
  public static void debug(final Object str, final Object... ext) {
    if(Prop.debug) errln(str, ext);
  }

  /**
   * Global method for printing the exception stack trace if the
   * {@link Prop#debug} flag is set.
   * @param ex exception
   * @return always false
   */
  public static boolean debug(final Exception ex) {
    if(Prop.debug && ex != null) ex.printStackTrace();
    return false;
  }

  /**
   * Global method for printing information to the standard output.
   * @param string debug string
   * @param ext text optional extensions
   */
  public static void err(final String string, final Object... ext) {
    System.err.print(info(string, ext));
  }

  /**
   * Global method for printing information to the standard output.
   * @param obj error string
   * @param ext text optional extensions
   */
  public static void errln(final Object obj, final Object... ext) {
    err(obj + NL, ext);
  }

  /**
   * Global method for printing information to the standard output.
   * @param str output string
   * @param ext text optional extensions
   */
  public static void out(final Object str, final Object... ext) {
    System.out.print(info(str, ext));
  }

  /**
   * Global method for printing information to the standard output.
   * @param str output string
   * @param ext text optional extensions
   */
  public static void outln(final Object str, final Object... ext) {
    out(str + NL, ext);
  }
  
  /**
   * Returns the length of the longest string.
   * @param str strings
   * @return maximum length
   */
  public static int max(final String[] str) {
    int max = 0;
    for(final String s : str) if(max < s.length()) max = s.length();
    return max;
  }

  /**
   * Returns an info message for the specified flag.
   * @param flag current flag status
   * @return ON/OFF message
   */
  public static String flag(final boolean flag) {
    return flag ? INFOON : INFOOFF;
  }

  /**
   * Throws a runtime exception for unimplemented methods.
   * @return dummy object
   */
  public static Object notimplemented() {
    throw new RuntimeException("Not implemented.");
  }

  /**
   * Throws a runtime exception for unexpected exceptions.
   * @return dummy object
   */
  public static Object notexpected() {
    throw new RuntimeException("Not expected.");
  }
}
