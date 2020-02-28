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
          library.pbDirect,
          library.akkaTestkit             % Test,
          library.akkaHttpTestkit         % Test,
          library.akkaPersistenceInMemory % Test,
          library.mockito                 % Test,
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
      val akka                     = "2.5.23"
      val akkaHttp                 = "10.1.9"
      val akkaPersistenceCassandra = "0.85"
      val akkaPersistenceInMemory  = "2.5.1.1"
      val akkaHttpCirce            = "1.27.0"
      val circeGeneric             = "0.11.1"
      val mockito                  = "1.0.0"
      val pbDirect                 = "0.1.0"
      val scalaCheck               = "1.13.5"
      val scalaTest                = "3.0.4"
    }
    val akkaHttp                 = "com.typesafe.akka"   %% "akka-http"                  % Version.akkaHttp
    val akkaTestkit              = "com.typesafe.akka"   %% "akka-testkit"               % Version.akka
    val akkaHttpTestkit          = "com.typesafe.akka"   %% "akka-http-testkit"          % Version.akkaHttp
    val akkaPersistence          = "com.typesafe.akka"   %% "akka-persistence"           % Version.akka
    val akkaPersistenceCassandra = "com.typesafe.akka"   %% "akka-persistence-cassandra" % Version.akkaPersistenceCassandra
    val akkaPersistenceInMemory  = "com.github.dnvriend" %% "akka-persistence-inmemory"  % Version.akkaPersistenceInMemory
    val akkaHttpCirce            = "de.heikoseeberger"   %% "akka-http-circe"            % Version.akkaHttpCirce
    val circeGeneric             = "io.circe"            %% "circe-generic"              % Version.circeGeneric
    val mockito                  = "org.mockito"         %% "mockito-scala"              % Version.mockito
    val pbDirect                 = "beyondthelines"      %% "pbdirect"                   % Version.pbDirect
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
    // scalaVersion from .travis.yml via sbt-travisci
    // scalaVersion := "2.12.4",
    organization := "org.akkacqrs",
    organizationName := "Branislav Lazic",
    startYear := Some(2018),
    licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0")),
    scalacOptions ++= Seq(
        "-unchecked",
        "-deprecation",
        "-language:_",
        "-target:jvm-1.8",
        "-encoding",
        "UTF-8",
        "-Ypartial-unification"
      ),
    unmanagedSourceDirectories.in(Compile) := Seq(scalaSource.in(Compile).value),
    unmanagedSourceDirectories.in(Test) := Seq(scalaSource.in(Test).value),
    resolvers += Resolver.bintrayRepo("beyondthelines", "maven")
  )

lazy val gitSettings =
  Seq(
    git.useGitDescribe := true
  )

lazy val scalafmtSettings =
  Seq(
    scalafmtOnCompile := true
  )
