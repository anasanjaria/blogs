import user.User

import java.nio.file.{Files, Paths}

object ConsumerApp extends App {
  println("ConsumerApp running...")

  private val bytes =
    Files.readAllBytes(
      Paths.get("target/user.bin")
    )

  private val user = User.parseFrom(bytes)
  println("Deserialized user: " + user.userId + ", " + user.name)
}

