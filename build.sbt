import scala.sys.process._
// *****************************************************************************
// Projects
// *****************************************************************************

lazy val `akka-cqrs-activator` =
  project
    .in(file("."))
    .enablePlugins(AutomateHeaderPlugin, GitVersioning, GitBranchPrompt)
    .dependsOn(`ui`)
    .settings(settings)
    .settings(
      assembly / assemblyMergeStrategy := {
        case x if x.contains("io.netty.versions.properties") => MergeStrategy.first
        case x =>
          val oldStrategy = (assembly / assemblyMergeStrategy).value
          oldStrategy(x)
      }
    )
    .settings(
      libraryDependencies ++= Seq(
          library.akkaHttp,
          library.akkaHttpCirce,
          library.akkaPersistence,
          library.akkaPersistenceCassandra,
          library.akkaRemote,
          library.akkaStream,
          library.avro4s,
          library.circeGeneric,
          library.akkaTestkit             % Test,
          library.akkaHttpTestkit         % Test,
          library.akkaPersistenceInMemory % Test,
          library.mockito                 % Test,
          library.scalaCheck              % Test,
          library.scalaTest               % Test
        )
    )
lazy val frontendTask           = taskKey[Unit]("execute frontend task")
lazy val frontendNodeModulesDir = settingKey[File]("node_modules directory")
lazy val frontendOutputDir      = settingKey[File]("output directory for target files")
lazy val frontendBuildDir       = settingKey[File]("output directory for build files")

Global / frontendNodeModulesDir := baseDirectory.value / "ui" / "node_modules"
Global / frontendBuildDir := baseDirectory.value / "ui" / "build"
Global / frontendOutputDir := baseDirectory.value / "ui" / "target" / s"scala-${scalaBinaryVersion.value}" / "classes"

Global / commands += Command.command("yarn") { state =>
  if (!frontendNodeModulesDir.value.exists()) {
    Keys.sLog.value.info("Node modules not installed. Installing ...")
    Process("yarn", new File("./ui")).!!
  }
  Keys.sLog.value.info("Building frontend app ...")
  Process("yarn build", new File("./ui")).!!
  IO.copyDirectory(frontendBuildDir.value, frontendOutputDir.value)
  state
}

lazy val `ui` =
  project
    .in(file("./ui"))
    .settings(settings)
    .settings(
      Compile / resourceDirectory := baseDirectory.value / "build",
      Compile / sourceDirectory := baseDirectory.value / "src"
    )

// *****************************************************************************
// Library dependencies
// *****************************************************************************

lazy val library =
  new {
    object Version {
      val akka                     = "2.5.32"
      val akkaHttp                 = "10.1.9"
      val akkaPersistenceCassandra = "0.107"
      val akkaPersistenceInMemory  = "2.5.15.2"
      val akkaHttpCirce            = "1.39.2"
      val circeGeneric             = "0.14.2"
      val mockito                  = "1.9.0"
      val scalaCheck               = "1.16.0"
      val scalaTest                = "3.2.12"
    }
    val akkaHttp                 = "com.typesafe.akka"   %% "akka-http"                  % Version.akkaHttp
    val akkaTestkit              = "com.typesafe.akka"   %% "akka-testkit"               % Version.akka
    val akkaStream               = "com.typesafe.akka"   %% "akka-stream"                % Version.akka
    val akkaRemote               = "com.typesafe.akka"   %% "akka-remote"                % Version.akka
    val akkaHttpTestkit          = "com.typesafe.akka"   %% "akka-http-testkit"          % Version.akkaHttp
    val akkaPersistence          = "com.typesafe.akka"   %% "akka-persistence"           % Version.akka
    val akkaPersistenceCassandra = "com.typesafe.akka"   %% "akka-persistence-cassandra" % Version.akkaPersistenceCassandra
    val akkaPersistenceInMemory  = "com.github.dnvriend" %% "akka-persistence-inmemory"  % Version.akkaPersistenceInMemory
    val akkaHttpCirce            = "de.heikoseeberger"   %% "akka-http-circe"            % Version.akkaHttpCirce
    val avro4s                   = "com.sksamuel.avro4s" %% "avro4s-core"                % "4.0.13"
    val circeGeneric             = "io.circe"            %% "circe-generic"              % Version.circeGeneric
    val mockito                  = "org.mockito"         %% "mockito-scala"              % Version.mockito
    val scalaCheck               = "org.scalacheck"      %% "scalacheck"                 % Version.scalaCheck
    val scalaTest                = "org.scalatest"       %% "scalatest"                  % Version.scalaTest
  }

// *****************************************************************************
// Settings
// *****************************************************************************

lazy val settings =
  commonSettings ++
  gitSettings ++
  scalafmtSettings

lazy val commonSettings =
  Seq(
    scalaVersion := "2.13.8",
    organization := "org.akkacqrs",
    organizationName := "Branislav Lazic",
    startYear := Some(2018),
    licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0")),
    scalacOptions ++= Seq(
        "-unchecked",
        "-deprecation",
        "-language:_",
        "-encoding",
        "UTF-8"
      ),
    Compile / unmanagedSourceDirectories := Seq((Compile / scalaSource).value),
    Test / unmanagedSourceDirectories := Seq((Test / scalaSource).value)
  )

lazy val gitSettings =
  Seq(
    git.useGitDescribe := true
  )

lazy val scalafmtSettings =
  Seq(
    scalafmtOnCompile := true
  )

addCommandAlias("c", "compile")
addCommandAlias("t", "test")
addCommandAlias("r", "reload")
// Create executable JAR
addCommandAlias("dist", ";yarn;assembly")
