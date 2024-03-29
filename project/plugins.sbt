addSbtPlugin("org.scalameta"     % "sbt-scalafmt" % "2.3.0")
addSbtPlugin("com.typesafe.sbt"  % "sbt-git"      % "0.9.3")
addSbtPlugin("de.heikoseeberger" % "sbt-header"   % "4.0.0")
addSbtPlugin("io.spray"          % "sbt-revolver" % "0.9.1")
addSbtPlugin("io.get-coursier"   % "sbt-coursier" % "1.0.3")
addSbtPlugin("com.eed3si9n"      % "sbt-assembly" % "0.14.10")

libraryDependencies += "org.slf4j" % "slf4j-nop" % "1.7.36" // Needed by sbt-git
