import com.jsuereth.sbtpgp.PgpKeys.publishSigned

// Used to publish snapshots to Maven Central.
val mavenCentralSnapshots = "Maven Central Snapshots" at "https://central.sonatype.com/repository/maven-snapshots"

// Versions:

val scala2_13 = "2.13.16"
val scala3 = "3.3.7"
val allScalaVersions = Seq(scala2_13, scala3)

val hearthVersion = "0.3.0-14-gf117e17-SNAPSHOT"
val refinedVersion = "0.11.3"
val munitVersion = "1.2.4"

// Common settings:

val publishSettings = Seq(
  organization := "com.kubuszok",
  homepage := Some(url("https://github.com/kubuszok/refined-compat")),
  organizationHomepage := Some(url("https://kubuszok.com")),
  licenses := Seq("Apache-2.0" -> url("https://www.apache.org/licenses/LICENSE-2.0")),
  scmInfo := Some(
    ScmInfo(
      url("https://github.com/kubuszok/refined-compat/"),
      "scm:git:git@github.com:kubuszok/refined-compat.git"
    )
  ),
  startYear := Some(2026),
  developers := List(
    Developer("MateuszKubuszok", "Mateusz Kubuszok", "", url("https://github.com/MateuszKubuszok"))
  ),
  pomExtra := (
    <issueManagement>
      <system>GitHub issues</system>
      <url>https://github.com/kubuszok/refined-compat/issues</url>
    </issueManagement>
  ),
  publishTo := {
    if (isSnapshot.value) Some(mavenCentralSnapshots)
    else localStaging.value
  },
  publishMavenStyle := true,
  Test / publishArtifact := false,
  pomIncludeRepository := { _ => false },
  versionScheme := Some("early-semver"),
  git.useGitDescribe := true,
  git.uncommittedSignifier := None,
  // Sonatype ignores isSnapshot setting and only looks at -SNAPSHOT suffix in version:
  //   https://central.sonatype.org/publish/publish-maven/#performing-a-snapshot-deployment
  // meanwhile sbt-git used to set up SNAPSHOT if there were uncommitted changes:
  //   https://github.com/sbt/sbt-git/issues/164
  // (now this suffix is empty by default) so we need to fix it manually.
  git.gitUncommittedChanges := git.gitCurrentTags.value.isEmpty,
  git.uncommittedSignifier := Some("SNAPSHOT")
)

val noPublishSettings =
  Seq(publish / skip := true, publishArtifact := false)

// Modules:

lazy val root = project
  .in(file("."))
  .enablePlugins(GitVersioning, GitBranchPrompt)
  .settings(publishSettings)
  .settings(noPublishSettings)
  .settings(
    name := "refined-compat-root",
    crossScalaVersions := Nil,
    commands += Command.command("ci-release") { state =>
      val extracted = Project.extract(state)
      val tags = extracted.get(git.gitCurrentTags)
      val cmd = if (tags.nonEmpty) "+publishSigned ; sonaRelease" else "+publishSigned"
      cmd :: state
    }
  )
  .aggregate(compat, tests)

lazy val compat = project
  .enablePlugins(GitVersioning, GitBranchPrompt)
  .settings(publishSettings)
  .settings(
    name := "refined-compat",
    crossScalaVersions := allScalaVersions,
    scalaVersion := scala3,
    scalacOptions ++= (CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, _)) => Seq("-Xsource:3", "-language:implicitConversions")
      case _            => Seq("-language:implicitConversions")
    }),
    resolvers += mavenCentralSnapshots,
    resolvers += Resolver.mavenLocal,
    libraryDependencies ++= Seq(
      "com.kubuszok" %% "hearth" % hearthVersion,
      "eu.timepit" %% "refined" % refinedVersion
    ),
    libraryDependencies ++= (CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, _)) =>
        Seq("com.kubuszok" %% "hearth-cross-quotes" % hearthVersion)
      case _ =>
        Seq("com.kubuszok" %% "hearth-cross-quotes" % hearthVersion % Provided)
    }),
    scalacOptions ++= (CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, _)) => Nil
      case _ =>
        val pluginJar = (Compile / dependencyClasspath).value
          .find(_.data.getName.contains("hearth-cross-quotes"))
          .map(_.data.getAbsolutePath)
          .getOrElse(sys.error("hearth-cross-quotes jar not found on classpath"))
        Seq(s"-Xplugin:$pluginJar")
    })
  )

lazy val tests = project
  .dependsOn(compat)
  .enablePlugins(GitVersioning, GitBranchPrompt)
  .settings(publishSettings)
  .settings(noPublishSettings)
  .settings(
    name := "refined-compat-tests",
    crossScalaVersions := allScalaVersions,
    scalaVersion := scala3,
    scalacOptions ++= (CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, _)) => Seq("-Xsource:3", "-language:implicitConversions")
      case _            => Seq("-language:implicitConversions")
    }),
    resolvers += mavenCentralSnapshots,
    resolvers += Resolver.mavenLocal,
    libraryDependencies ++= Seq(
      "org.scalameta" %% "munit" % munitVersion % Test
    )
  )
