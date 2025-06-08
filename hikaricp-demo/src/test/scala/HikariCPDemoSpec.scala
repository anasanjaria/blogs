import com.google.common.util.concurrent.ThreadFactoryBuilder
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.sql.SQLTransientConnectionException
import java.time.LocalDateTime
import java.util.concurrent.Executors
import scala.concurrent.duration.Duration
import scala.concurrent.Await
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class HikariCPDemoSpec extends AnyWordSpec with Matchers with BeforeAndAfterAll {

  private val databaseUrl = "postgresql://localhost:5432/demo"
  private val driver = "org.postgresql.Driver"
  private val connectionTimeoutInMillis = 3000

  private val config = new HikariConfig()
  config.setJdbcUrl(s"jdbc:$databaseUrl")
  config.setUsername("postgres")
  config.setPassword("postgres")
  config.setDriverClassName(driver)
  config.setMaximumPoolSize(1)
  config.setConnectionTimeout(connectionTimeoutInMillis)
  config.setPoolName("test-pool")

  private val ds = new HikariDataSource(config)

  private val executorService = Executors.newFixedThreadPool(2, new ThreadFactoryBuilder().setNameFormat(s"app-thread-pool-%d").build())
  private implicit val executionContext: ExecutionContext = ExecutionContext.fromExecutor(executorService)

  override def afterAll(): Unit = {
    ds.close()
    executorService.shutdown()
  }

  private def executeQuery(sleepSeconds: Int, callback: () => Unit): Future[Unit] = Future {
    printWithThreadName("Executing query with sleep: " + sleepSeconds)
    val connection = ds.getConnection
    try {
      val stmt = connection.prepareStatement(s"SELECT pg_sleep($sleepSeconds)")
      stmt.execute()
      callback()
    } finally {
      connection.close()
    }
  }

  private def simulateSlowNonDBWork(): Unit = {
    printWithThreadName("Simulating slow non-DB work")
    Thread.sleep(3000)
  }

  private def printWithThreadName(message: String): Unit = {
    val threadName = Thread.currentThread.getName
    println(s"${LocalDateTime.now} - [$threadName] - $message")
  }

  "Demo" should {
    "throw an exception when long running non-DB work block a connection" in {
      intercept[SQLTransientConnectionException] {

        val computation1 = executeQuery(1, () => simulateSlowNonDBWork())
        val computation2 = executeQuery(1, () => printWithThreadName("Quick Task."))

        val r = (for {
          _ <- computation1
          _ <- computation2
        } yield ())
        Await.result(r, Duration.Inf)
      }
    }
    "throw an exception when long running query block a connection" in {
      intercept[SQLTransientConnectionException] {
        // Both computations will start immediately in parallel, since we have two threads in the pool.
        // However, the long-running query will block the other query since we only have one connection in the pool.
        val longQuery = executeQuery(4, () => printWithThreadName("Quick Task."))
        val blockedQuery = executeQuery(1, () => printWithThreadName("Quick Task."))

        val r = (for {
          _ <- longQuery
          _ <- blockedQuery
        } yield ())
        Await.result(r, Duration.Inf)
      }
    }
  }
}
