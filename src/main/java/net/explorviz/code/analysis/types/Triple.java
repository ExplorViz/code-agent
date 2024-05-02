package net.explorviz.code.analysis.types;

/**
 * Generic Triple Class to store three generic values.
 *
 * @param <L> left type
 * @param <M> middle type
 * @param <R> right type
 */
public class Triple<L, M, R> {

  private final L left;
  private final M middle;
  private final R right;

  /**
   * Creates a Triple of data entries.
   *
   * @param left   the left data entry
   * @param middle the middle data entry
   * @param right  the right data entry
   */
  public Triple(final L left, final M middle, final R right) {
    this.left = left;
    this.middle = middle;
    this.right = right;
  }

  public L getLeft() {
    return left;
  }

  public M getMiddle() {
    return middle;
  }

  public R getRight() {
    return right;
  }
}
