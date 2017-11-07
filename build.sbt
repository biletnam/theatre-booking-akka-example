name := "theatre-example"

version := "0.1"

scalaVersion := "2.12.4"

resolvers += Resolver.bintrayRepo("tanukkii007", "maven")

libraryDependencies ++= {
  val akka = "com.typesafe.akka"
  val akkaV = "2.5.6"
  val akkaHttpV = "10.0.10"

  Seq(
    akka                       %% "akka-actor"                        % akkaV,
    akka                       %% "akka-cluster-sharding"             % akkaV,
    akka                       %% "akka-slf4j"                        % akkaV,
    akka                       %% "akka-stream"                       % akkaV,
    akka                       %% "akka-testkit"                      % akkaV % Test,
    akka                       %% "akka-http"                         % akkaHttpV,
    akka                       %% "akka-http-spray-json"              % akkaHttpV,
    akka                       %% "akka-http-testkit"                 % akkaHttpV % Test,
    "com.github.TanUkkii007"   %% "akka-cluster-custom-downing"       % "0.0.8",
    "de.heikoseeberger"        %% "constructr"                        % "0.18.0",
    "com.lightbend.constructr" %% "constructr-coordination-zookeeper" % "0.4.0",
    "org.scalatest"            %% "scalatest"                         % "3.0.4" % Test,
    "ch.qos.logback"           % "logback-classic"                    % "1.2.3",
    "org.codehaus.groovy"      % "groovy"                             % "2.4.12"
  )
}

scalafmtOnCompile in ThisBuild := true // all projects
scalafmtOnCompile := true // current project
scalafmtOnCompile in Compile := true // current project, specific configuration
