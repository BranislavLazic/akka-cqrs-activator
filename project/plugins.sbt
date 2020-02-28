addSbtPlugin("com.dwijnand"      % "sbt-travisci" % "1.1.1")
addSbtPlugin("org.scalameta"     % "sbt-scalafmt" % "2.3.0")
addSbtPlugin("com.typesafe.sbt"  % "sbt-git"      % "0.9.3")
addSbtPlugin("de.heikoseeberger" % "sbt-header"   % "4.0.0")
addSbtPlugin("io.spray"          % "sbt-revolver" % "0.9.1")
addSbtPlugin("io.get-coursier"   % "sbt-coursier" % "1.0.3")

libraryDependencies += "org.slf4j" % "slf4j-nop" % "1.7.25" // Needed by sbt-git
