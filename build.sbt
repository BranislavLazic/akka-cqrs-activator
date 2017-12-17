// *****************************************************************************
// Projects
// *****************************************************************************

lazy val `akka-cqrs-activator` =
  project
    .in(file("."))
    .enablePlugins(AutomateHeaderPlugin, GitVersioning, GitBranchPrompt)
    .settings(settings)
    .settings(
      libraryDependencies ++= Seq(
        library.akkaHttp,
        library.akkaHttpCirce,
        library.akkaPersistence,
        library.akkaPersistenceCassandra,
        library.circeGeneric,
        library.serverSentEvents,
        library.akkaHttpTestkit         % Test,
        library.akkaPersistenceInMemory % Test,
        library.scalaCheck              % Test,
        library.scalaTest               % Test
      )
    )

// *****************************************************************************
// Library dependencies
// *****************************************************************************

lazy val library =
  new {
    object Version {
      val akka                     = "2.5.7"
      val akkaHttp                 = "10.0.11"
      val akkaPersistenceCassandra = "0.59"
      val akkaPersistenceInMemory  = "2.5.1.1"
      val akkaHttpCirce            = "1.17.0"
      val circeGeneric             = "0.8.0"
      val scalaCheck               = "1.13.5"
      val scalaTest                = "3.0.4"
      val scalaProtobuf            = com.trueaccord.scalapb.compiler.Version.scalapbVersion
      val sse                      = "3.0.0"
    }
    val akkaHttp                 = "com.typesafe.akka"      %% "akka-http"                  % Version.akkaHttp
    val akkaHttpTestkit          = "com.typesafe.akka"      %% "akka-http-testkit"          % Version.akkaHttp
    val akkaPersistence          = "com.typesafe.akka"      %% "akka-persistence"           % Version.akka
    val akkaPersistenceCassandra = "com.typesafe.akka"      %% "akka-persistence-cassandra" % Version.akkaPersistenceCassandra
    val akkaPersistenceInMemory  = "com.github.dnvriend"    %% "akka-persistence-inmemory"  % Version.akkaPersistenceInMemory
    val akkaHttpCirce            = "de.heikoseeberger"      %% "akka-http-circe"            % Version.akkaHttpCirce
    val serverSentEvents         = "de.heikoseeberger"      %% "akka-sse"                   % Version.sse
    val circeGeneric             = "io.circe"               %% "circe-generic"              % Version.circeGeneric
    val scalaCheck               = "org.scalacheck"         %% "scalacheck"                 % Version.scalaCheck
    val scalaTest                = "org.scalatest"          %% "scalatest"                  % Version.scalaTest
    val scalapbRuntime           = "com.trueaccord.scalapb" %% "scalapb-runtime"            % Version.scalaProtobuf
  }

// *****************************************************************************
// Settings
// *****************************************************************************

lazy val settings =
commonSettings ++
gitSettings ++
scalafmtSettings ++
protobufSettings

lazy val commonSettings =
  Seq(
    // scalaVersion from .travis.yml via sbt-travisci
    // scalaVersion := "2.12.4",
    organization := "org.akkacqrs",
    organizationName := "Branislav Lazic",
    startYear := Some(2017),
    licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0")),
    scalacOptions ++= Seq(
      "-unchecked",
      "-deprecation",
      "-language:_",
      "-target:jvm-1.8",
      "-encoding",
      "UTF-8"
    ),
    unmanagedSourceDirectories.in(Compile) := Seq(scalaSource.in(Compile).value),
    unmanagedSourceDirectories.in(Test) := Seq(scalaSource.in(Test).value)
  )

lazy val gitSettings =
  Seq(
    git.useGitDescribe := true
  )

lazy val scalafmtSettings =
  Seq(
    scalafmtOnCompile := true,
    scalafmtOnCompile.in(Sbt) := false,
    scalafmtVersion := "1.3.0"
  )

lazy val protobufSettings =
  Seq(
    PB.targets.in(Compile) := Seq(
      scalapb.gen(flatPackage = true) -> sourceManaged.in(Compile).value
    ),
    libraryDependencies ++= Seq(
      library.scalapbRuntime % "protobuf"
    )
  )
