package dao

import scala.concurrent.Future

trait ProductDAO {

  /**
   * Store a product in our system.
   * For the sake of simplicity, I have only considered product name.
   *
   * @param userId User id
   * @param productName Name of the product.
   * @return a `Future` holding an id of the inserted username.
   * */
  def storeProduct(userId: Int, productName: String): Future[Int]
}
