import com.google.common.util.concurrent.ThreadFactoryBuilder
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

import java.time.LocalDateTime
import java.util.concurrent.Executors
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}

class ExecutionContextExamplesSpec extends AnyFlatSpec with Matchers {

  private def fetchDataFromDB(implicit executionContext: ExecutionContext): Future[Int] = Future {
      printWithThreadName("Fetching data from DB")
      // mimic retrieval time
      Thread.sleep(500)
      1
  }

  private def printWithThreadName(message: String): Unit = {
    val threadName = Thread.currentThread.getName
    println(s"${LocalDateTime.now} - [$threadName] - $message")
  }

  private def applyBusinessLogic(n: Int)(implicit executionContext: ExecutionContext): Future[Int] = Future {
    printWithThreadName("Applying business logic")
    // mimic time it takes to perform business logic
    Thread.sleep(500)
    n * 2
  }



  "ExecutionContextExamples" should "execute all operations sequentially when pool size is 1" ignore {

    val start = System.currentTimeMillis()
    val executorService = Executors.newFixedThreadPool(1, new ThreadFactoryBuilder().setNameFormat(s"app-thread-pool-%d").build())
    implicit val executionContext: ExecutionContext = ExecutionContext.fromExecutor(executorService)

    val op1 = fetchDataFromDB.flatMap(applyBusinessLogic)
    val op2 = fetchDataFromDB.flatMap(applyBusinessLogic)

    val f = (for {
      r1 <- op1
      r2 <- op2
    } yield {
      r1 + r2
    })

    val r = Await.result(f, Duration.Inf)
    val end = System.currentTimeMillis()
    println(s"Time taken: ${(end - start) / 1000.0} seconds")
    r must be(4)

  }
  it should "execute all operation concurrently when the pool size is 2" ignore {

    val start = System.currentTimeMillis()
    val executorService = Executors.newFixedThreadPool(2, new ThreadFactoryBuilder().setNameFormat(s"app-thread-pool-%d").build())
    implicit val executionContext: ExecutionContext = ExecutionContext.fromExecutor(executorService)

    val op1 = fetchDataFromDB.flatMap(applyBusinessLogic)
    val op2 = fetchDataFromDB.flatMap(applyBusinessLogic)

    val f = (for {
      r1 <- op1
      r2 <- op2
    } yield {
      r1 + r2
    })

    val r = Await.result(f, Duration.Inf)
    val end = System.currentTimeMillis()
    println(s"Time taken: ${(end - start) / 1000.0} seconds")
    r must be(4)

  }
  it should "use dedicated execution context for DB and business logic" ignore {

    val start = System.currentTimeMillis()
    val dbExecutorService = Executors.newFixedThreadPool(2, new ThreadFactoryBuilder().setNameFormat(s"db-thread-pool-%d").build())
    val dbExecutionContext: ExecutionContext = ExecutionContext.fromExecutor(dbExecutorService)

    val executorService = Executors.newFixedThreadPool(2, new ThreadFactoryBuilder().setNameFormat(s"app-thread-pool-%d").build())
    implicit val executionContext: ExecutionContext = ExecutionContext.fromExecutor(executorService)

    val op1 = fetchDataFromDB(dbExecutionContext).flatMap(applyBusinessLogic)
    val op2 = fetchDataFromDB(dbExecutionContext).flatMap(applyBusinessLogic)

    val f = (for {
      r1 <- op1
      r2 <- op2
    } yield {
      r1 + r2
    })

    val r = Await.result(f, Duration.Inf)
    val end = System.currentTimeMillis()
    println(s"Time taken: ${(end - start) / 1000.0} seconds")
    r must be(4)

  }
  it should "increase number of concurrent operation but using dedicated execution context" ignore {

    val start = System.currentTimeMillis()
    val dbExecutorService = Executors.newFixedThreadPool(2, new ThreadFactoryBuilder().setNameFormat(s"db-thread-pool-%d").build())
    val dbExecutionContext: ExecutionContext = ExecutionContext.fromExecutor(dbExecutorService)

    val executorService = Executors.newFixedThreadPool(2, new ThreadFactoryBuilder().setNameFormat(s"app-thread-pool-%d").build())
    implicit val executionContext: ExecutionContext = ExecutionContext.fromExecutor(executorService)

    val op1 = fetchDataFromDB(dbExecutionContext).flatMap(applyBusinessLogic)
    val op2 = fetchDataFromDB(dbExecutionContext).flatMap(applyBusinessLogic)
    val op3 = fetchDataFromDB(dbExecutionContext).flatMap(applyBusinessLogic)

    val f = (for {
      r1 <- op1
      r2 <- op2
      r3 <- op3
    } yield {
      r1 + r2 + r3
    })

    val r = Await.result(f, Duration.Inf)
    val end = System.currentTimeMillis()
    println(s"Time taken: ${(end - start) / 1000.0} seconds")
    r must be(6)

  }

  it should "increase number of concurrent operation but using only single execution context" ignore {

    val start = System.currentTimeMillis()
    val executorService = Executors.newFixedThreadPool(2, new ThreadFactoryBuilder().setNameFormat(s"app-thread-pool-%d").build())
    implicit val executionContext: ExecutionContext = ExecutionContext.fromExecutor(executorService)

    val op1 = fetchDataFromDB.flatMap(applyBusinessLogic)
    val op2 = fetchDataFromDB.flatMap(applyBusinessLogic)
    val op3 = fetchDataFromDB.flatMap(applyBusinessLogic)

    val f = (for {
      r1 <- op1
      r2 <- op2
      r3 <- op3
    } yield {
      r1 + r2 + r3
    })

    val r = Await.result(f, Duration.Inf)
    val end = System.currentTimeMillis()
    println(s"Time taken: ${(end - start) / 1000.0} seconds")
    r must be(6)

  }
}