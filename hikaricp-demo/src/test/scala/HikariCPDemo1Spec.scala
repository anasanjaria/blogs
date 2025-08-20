import com.google.common.util.concurrent.ThreadFactoryBuilder
import com.zaxxer.hikari.HikariDataSource
import com.zaxxer.hikari.HikariConfig
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AsyncWordSpec

import java.sql.SQLTransientConnectionException
import java.util.concurrent.Executors
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class HikariCPDemo1Spec extends AsyncWordSpec with Matchers with BeforeAndAfterAll {

  private val databaseUrl = "postgresql://localhost:5432/demo"
  private val driver = "org.postgresql.Driver"
  private val connectionTimeoutInSeconds = 3
  private val maxConnections: Int = 1

  private val config = new HikariConfig()
  config.setJdbcUrl(s"jdbc:$databaseUrl")
  config.setUsername("postgres")
  config.setPassword("postgres")
  config.setDriverClassName(driver)
  config.setMaximumPoolSize(maxConnections)
  config.setConnectionTimeout(connectionTimeoutInSeconds * 1000)
  config.setPoolName("test-pool")

  private val ds = new HikariDataSource(config)

  private val executorServiceWithMoreThreadsThanConnections = Executors.newFixedThreadPool(maxConnections + 1, new ThreadFactoryBuilder().setNameFormat(s"thread-pool-a-%d").build())
  private val executionContextWithMoreThreadsThanConnections: ExecutionContext = ExecutionContext.fromExecutor(executorServiceWithMoreThreadsThanConnections)

  private val executorServiceWithSameThreadsAsConnections = Executors.newFixedThreadPool(maxConnections, new ThreadFactoryBuilder().setNameFormat(s"thread-pool-b-%d").build())
  private val executionContextWithSameThreadsAsConnections: ExecutionContext = ExecutionContext.fromExecutor(executorServiceWithSameThreadsAsConnections)

  override def afterAll(): Unit = {
    ds.close()
    executorServiceWithMoreThreadsThanConnections.shutdown()
    executorServiceWithSameThreadsAsConnections.shutdown()
  }

  private def executeQuery(sleepSeconds: Int)(executionContext: ExecutionContext): Future[String] = Future {
    val connection = ds.getConnection
    try {
      val stmt = connection.prepareStatement(s"SELECT pg_sleep($sleepSeconds)")
      stmt.execute()
      "success"
    } finally {
      connection.close()
    }
  }(executionContext)

  "HikariTimeoutDemo" should {
    "throw an exception when there are more work ready for processing but unavailable connection blocks them" in {
      recoverToSucceededIf[SQLTransientConnectionException] {
        val computation1 = executeQuery(connectionTimeoutInSeconds)(executionContextWithMoreThreadsThanConnections)
        val computation2 = executeQuery(connectionTimeoutInSeconds)(executionContextWithMoreThreadsThanConnections)

        for {
          _ <- computation1
          _ <- computation2
        } yield ()
      }
    }
    "process computations successfully when there are more work but not ready for processing hence not waiting for connection to be available" in {
      val computation1 = executeQuery(connectionTimeoutInSeconds)(executionContextWithSameThreadsAsConnections)
      val computation2 = executeQuery(connectionTimeoutInSeconds)(executionContextWithSameThreadsAsConnections)

      for {
        r1 <- computation1
        r2 <- computation2
      } yield {
        r1 mustBe "success"
        r2 mustBe "success"
      }
    }
  }
}
