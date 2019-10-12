package respy.core;

/**
 * Mark the Timeout exception.
 */
public class TimeoutException extends RuntimeException {
  public TimeoutException(String msg) {
    super(msg);
  }
}
