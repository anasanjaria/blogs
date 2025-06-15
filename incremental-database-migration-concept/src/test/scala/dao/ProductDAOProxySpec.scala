package dao

import org.scalamock.scalatest.AsyncMockFactory
import org.scalatest.FutureOutcome
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.FixtureAsyncWordSpec
import utils.IncrementalRollout

import scala.concurrent.Future

class ProductDAOProxySpec extends FixtureAsyncWordSpec with Matchers with AsyncMockFactory {

  private val productName: String = "product name"
  private val productId: Int = 1
  private val userId: Int = 10

  override def withFixture(test: OneArgAsyncTest): FutureOutcome = {
    withFixture(
      test.toNoArgAsyncTest(
        new ProductDAOProxy(mock[MongodbProductDAO], mock[PsqlProductDAO], mock[IncrementalRollout], executionContext)
      )
    )
  }

  override type FixtureParam = ProductDAOProxy

  "ProductDAOProxy" should {
    "fail whole product saving operation while rolling out dual-writes" when {
      "it fails to store product in a mongodb" in { proxy =>
        (proxy.psqlProductDAO.storeProduct _).expects(userId, productName).returns(Future.successful(productId))
        (proxy.mongodbProductDAO.storeProduct _).expects(userId, productName).returns(Future.failed(new RuntimeException("Database failed to store a product")))
        (proxy.rollout.isAllowed _).expects(userId).returns(true)

        recoverToSucceededIf[RuntimeException] {
          proxy.storeProduct(userId, productName)
        }
      }
    }
    "not fail whole product saving operation while rolling out dual-writes" when {
      "it fails to store product in a postgres" in { proxy =>
        (proxy.psqlProductDAO.storeProduct _).expects(userId, productName).returns(Future.failed(new RuntimeException("Database failed to store a product")))
        (proxy.mongodbProductDAO.storeProduct _).expects(userId, productName).returns(Future.successful(productId))
        (proxy.rollout.isAllowed _).expects(userId).returns(true)

        for {
          result <- proxy.storeProduct(userId, productName)
        } yield {
          result must be(productId)
        }
      }
    }
    "only store products in mongodb when user is not allowed to use postgres" in { proxy =>
      (proxy.mongodbProductDAO.storeProduct _).expects(userId, productName).returns(Future.successful(productId))
      (proxy.rollout.isAllowed _).expects(userId).returns(false)

      for {
        result <- proxy.storeProduct(userId, productName)
      } yield {
        result must be(productId)
      }
    }
  }
}
