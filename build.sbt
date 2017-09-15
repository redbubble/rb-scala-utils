organization := "com.redbubble"

name := "rb-scala-utils"

enablePlugins(GitVersioning, GitBranchPrompt)

git.useGitDescribe := true

bintrayOrganization := Some("redbubble")

bintrayRepository := "open-source"

bintrayPackageLabels := Seq("scala", "utilities", "util", "circe", "cats", "finagle", "finch")

licenses += ("BSD New", url("https://opensource.org/licenses/BSD-3-Clause"))

scalacOptions ++= Seq(
  "-unchecked",
  "-deprecation",
  "-Xlint",
  "-encoding", "UTF-8",
  "-feature",
  "-language:existentials",
  "-language:higherKinds",
  "-language:implicitConversions",
  "-language:reflectiveCalls",
  "-unchecked",
  "-Yno-adapted-args",
  "-Ywarn-dead-code",
  "-Ywarn-numeric-widen",
  "-Xfuture",
  "-Xlint",
  //"-Yno-predef",
  //"-Ywarn-unused-import", // gives false positives
  "-Xfatal-warnings",
  "-Ywarn-value-discard",
  "-Ypartial-unification"

)

resolvers ++= Seq(
  Resolver.jcenterRepo,
  Resolver.sonatypeRepo("releases"),
  Resolver.sonatypeRepo("snapshots"),
  "Twitter" at "http://maven.twttr.com"
)

scalacOptions in Test ++= Seq("-Yrangepos")

lazy val catsVersion = "0.9.0"
lazy val mouseVersion = "0.9"
lazy val circeVersion = "0.8.0"
// The version numbers for Finagle, Twitter, Finch & Catbird *must* work together. See the Finch build.sbt for known good versions.
lazy val finchVersion = "0.15.1"
lazy val finagleVersion = "6.45.0"
lazy val finagleHttpAuthVersion = "0.1.0"
lazy val twitterServerVersion = "1.30.0"
lazy val catBirdVersion = "0.15.0"
lazy val sangriaVersion = "1.3.0"
lazy val sangriaCirceVersion = "1.1.0"
lazy val scalaJava8CompatVersion = "0.8.0"
lazy val featherbedVersion = "0.3.1"
lazy val jodaTimeVersion = "2.9.9"
lazy val jodaConvertVersion = "1.8.2"
lazy val scalaCacheVersion = "0.9.4"
lazy val scalaUriVersion = "0.4.16"
lazy val fetchVersion = "0.6.2"
lazy val slf4jVersion = "1.7.25"
lazy val logbackVersion = "1.2.3"
lazy val rollbarVersion = "0.5.4"
lazy val nrVersion = "3.40.0"
lazy val metricsVersion = "3.2.3"
lazy val metricsNewRelicVersion = "1.1.1"
lazy val specsVersion = "3.9.5"
lazy val chillVersion = "0.9.2"

libraryDependencies ++= Seq(
  "org.typelevel" %% "cats-core" % catsVersion,
  "io.circe" %% "circe-core" % circeVersion,
  "io.circe" %% "circe-generic" % circeVersion,
  "io.circe" %% "circe-parser" % circeVersion,
  "com.github.benhutchison" %% "mouse" % mouseVersion,
  "com.github.finagle" %% "finch-core" % finchVersion,
  "com.github.finagle" %% "finch-circe" % finchVersion,
  "org.sangria-graphql" %% "sangria" % sangriaVersion,
  "org.sangria-graphql" %% "sangria-relay" % sangriaVersion,
  "org.sangria-graphql" %% "sangria-circe" % sangriaCirceVersion,
  "org.scala-lang.modules" %% "scala-java8-compat" % scalaJava8CompatVersion,
  "joda-time" % "joda-time" % jodaTimeVersion,
  "org.joda" % "joda-convert" % jodaConvertVersion,
  "com.twitter" %% "finagle-http" % finagleVersion,
  "com.twitter" %% "finagle-stats" % finagleVersion,
  "com.github.finagle" %% "finagle-http-auth" % finagleHttpAuthVersion,
  "com.github.finagle" %% "finch-core" % finchVersion,
  "com.github.finagle" %% "finch-circe" % finchVersion,
  "io.catbird" %% "catbird-finagle" % catBirdVersion,
  "com.netaporter" %% "scala-uri" % scalaUriVersion,
  "com.47deg" %% "fetch" % fetchVersion,
  "io.github.finagle" %% "featherbed-core" % featherbedVersion,
  "io.github.finagle" %% "featherbed-circe" % featherbedVersion,
  "io.dropwizard.metrics" % "metrics-core" % metricsVersion,
  "com.palominolabs.metrics" % "metrics-new-relic" % metricsNewRelicVersion,
  "com.github.cb372" %% "scalacache-caffeine" % scalaCacheVersion,
  "com.github.cb372" %% "scalacache-redis" % scalaCacheVersion,
  "ch.qos.logback" % "logback-core" % logbackVersion,
  "ch.qos.logback" % "logback-classic" % logbackVersion,
  "org.slf4j" % "slf4j-api" % slf4jVersion,
  "org.slf4j" % "jul-to-slf4j" % slf4jVersion,
  "com.rollbar" % "rollbar" % rollbarVersion,
  "org.specs2" %% "specs2-core" % specsVersion % "test",
  "org.specs2" %% "specs2-scalacheck" % specsVersion % "test",
  "com.twitter" %% "chill" % chillVersion
)
