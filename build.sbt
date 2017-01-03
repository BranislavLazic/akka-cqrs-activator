lazy val `akka-cqrs-activator` = project
  .in(file("."))
  .enablePlugins(AutomateHeaderPlugin, GitVersioning)

libraryDependencies ++= Vector(
  Library.scalaTest % "test"
)

initialCommands := """|import default.akka.cqrs.activator._
                      |""".stripMargin
