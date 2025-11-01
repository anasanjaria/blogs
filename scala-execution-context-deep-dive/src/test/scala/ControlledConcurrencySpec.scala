import com.google.common.util.concurrent.ThreadFactoryBuilder
import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

import java.util.concurrent.Executors
import scala.concurrent.{ExecutionContext, Future}

class ControlledConcurrencySpec extends AnyFlatSpec with Matchers with BeforeAndAfterAll {

  private val maxParallelRequests = 2
  private val executorService = Executors.newFixedThreadPool(maxParallelRequests, new ThreadFactoryBuilder().setNameFormat(s"app-thread-pool-%d").build())
  private implicit val executionContext: ExecutionContext = ExecutionContext.fromExecutor(executorService)

  override def afterAll(): Unit = {
    executorService.shutdown()
  }

  private def startProcessing(seconds: Int): Unit = {
    Thread.sleep(seconds * 1000)
  }

  "ControlledConcurrency" should "process batches but sequential across batches" in {
    val batch1 = Seq(4, 1)
    val batch2 = Seq(2, 3)
    val batches = Seq(
      batch1,
      batch2
    )
    val r = batches.foldLeft(Future.successful(0L)) { (acc, batch) =>
      acc.flatMap { results =>
        val start = System.currentTimeMillis()
        val futures = batch.map { seconds =>
          Future(startProcessing(seconds))
        }
        val fr = Future.sequence(futures).map { _ =>
          val duration = System.currentTimeMillis() - start
          println(s"Batch: ${batch} completed in duration: $duration  ms")
          results + duration
        }
        println("Starting next batch...")
        fr
      }
    }
    val results = scala.concurrent.Await.result(r, scala.concurrent.duration.Duration.Inf)
    println(s"Test # 1 - Whole batch completed in duration: $results ms")
    results must be >= 7000L
  }
  it should "process batches but uncontrolled concurrency" in {
    val batch1 = Seq(4, 1) //
    val batch2 = Seq(2, 3)
    val batches = Seq(
      batch1,
      batch2
    )

    val start = System.currentTimeMillis()
    val futures = batches.flatMap { batch =>
      batch.map{ seconds => Future(startProcessing(seconds)) }
    }
    val r = Future.sequence(futures).map { _ =>
      val end = System.currentTimeMillis()
      end - start
    }
    val results = scala.concurrent.Await.result(r, scala.concurrent.duration.Duration.Inf)
    println(s"Test # 2 - Whole batch completed in duration: $results ms")
    results must be >= 6000L
    results must be < 7000L
  }
}
