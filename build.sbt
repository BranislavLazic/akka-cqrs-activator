lazy val `akka-cqrs-activator` = project
  .in(file("."))
  .enablePlugins(AutomateHeaderPlugin, GitVersioning)

libraryDependencies ++= Vector(
  Library.akkaHttp,
  Library.akkaHttpJson,
  Library.akkaPersistence,
  Library.akkaPersistenceCassandra,
  Library.akkaPersistenceInmemory,
  Library.circeGeneric,
  Library.serverSentEvents,
  Library.scalaTest % "test",
  Library.akkaHttpTestkit % "test"
)

initialCommands := """|import org.akkacqrs._
                      |""".stripMargin

PB.targets in Compile := Seq(
  scalapb.gen() -> (sourceManaged in Compile).value
)
