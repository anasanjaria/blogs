package dao

import scala.concurrent.Future

/**
 * Mongodb based implementation for managing products.
 * */
class MongodbProductDAO extends ProductDAO {

  override def storeProduct(userId: Int, productName: String): Future[Int] = ???
}
