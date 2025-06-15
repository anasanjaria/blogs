package utils

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

class IncrementalRolloutSpec extends AnyWordSpec with Matchers {
  "IncrementalRollout" should {
    "allow a user to use a feature" when {
      "a feature is enabled completely for all users" in {
        val rollout = new IncrementalRollout(percentage = 1)
        rollout.isAllowed(userId = 1) must be(true)
      }
      "a feature is enabled 50%" in {
        val rollout = new IncrementalRollout(percentage = 0.5f)
        rollout.isAllowed(userId = 1) must be(true)
      }
    }
    "not allow a user to use a feature" when {
      "a feature is disabled completely for all users" in {
        val rollout = new IncrementalRollout(percentage = 0)
        rollout.isAllowed(userId = 1) must be(false)
      }
      "a feature is enabled 50%" in {
        val rollout = new IncrementalRollout(percentage = 0.5f)
        rollout.isAllowed(userId = 80) must be(false)
      }
    }
  }
}
