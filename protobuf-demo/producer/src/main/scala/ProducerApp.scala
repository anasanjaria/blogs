import user.User

import java.nio.file.{Files, Paths}

object ProducerApp extends App {
  private val v1 = User(
    userId = 42,
    name = "Foo Bar"
  )

  private val bytes = v1.toByteArray

  Files.write(Paths.get("target/user.bin"), bytes)
}

