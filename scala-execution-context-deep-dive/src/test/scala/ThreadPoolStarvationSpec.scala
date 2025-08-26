import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import scala.concurrent.duration._
import java.util.concurrent.Executors
import scala.concurrent.{Await, ExecutionContext, Future}

class ThreadPoolStarvationSpec extends AnyWordSpec with Matchers with BeforeAndAfterAll {
  private val executorService = Executors.newFixedThreadPool(1)
  private implicit val executionContext: ExecutionContext = ExecutionContext.fromExecutor(executorService)

  override def afterAll(): Unit = {
    executorService.shutdown()
  }

  private def asyncOp(): Future[Int] = Future {
    Thread.sleep(100)
    42
  }

  "ThreadPoolStarvation" should {
    "throw timeout exception due to thread pool starvation" in {
      // task occupy the only thread in the pool and block, waiting for asyncOperation to complete.
      // But asyncOperation also needs a thread from the same pool to run!
      // Since all threads in a pool, which in our case is only 1, are blocked,
      // asyncOperation never runs, and the program deadlocks.
      val task = Future { Await.result(asyncOp(), 1.second) * 2 }
      intercept[Exception] {
        Await.result(task, 2.seconds)
      }
    }
    "process task successfully due to non-blocking composition with future chaining" in {
      val task = asyncOp().map(_ * 2)
      val result = Await.result(task, 2.seconds)
      result must be(84)
    }
  }
}
