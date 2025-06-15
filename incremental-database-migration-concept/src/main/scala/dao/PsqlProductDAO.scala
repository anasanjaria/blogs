package dao

import scala.concurrent.Future

/**
 * Postgres based implementation for managing products.
 * */
class PsqlProductDAO extends ProductDAO {

  override def storeProduct(userId: Int, productName: String): Future[Int] = ???
}
