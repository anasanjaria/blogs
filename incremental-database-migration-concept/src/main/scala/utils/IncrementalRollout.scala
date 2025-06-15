package utils

/**
 * Rolling out a feature incrementally.
 *
 * @param percentage Percentage used to determine whether a given user is allowed
 *                   to use a feature. Allowed values: 0 - 1.
 *                   For instance:
 *                   - 0 means 0%.
 *                   - 0.95 means 95%.
 *                   - 1 means 100%.
 * */
class IncrementalRollout(percentage: Float) {

  /**
   * Whether a given user is allowed to use a feature.
   * @param userId User id.
   * @return Boolean true if a user is allowed false otherwise.
   * */
  def isAllowed(userId: Int): Boolean = {
    require(percentage >= 0.0 && percentage <= 1.0, "The percentage should be between 0.0 and 1.0")
    userId % 100 < (percentage * 100)
  }
}
