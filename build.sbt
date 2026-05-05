import com.jsuereth.sbtpgp.PgpKeys.publishSigned
import commandmatrix.extra.*

// Used to publish snapshots to Maven Central.
val mavenCentralSnapshots = "Maven Central Snapshots" at "https://central.sonatype.com/repository/maven-snapshots"

// Versions:

val versions = new {
  val scala213 = "2.13.16"
  val scala3 = "3.3.7"

  val scalas = List(scala213, scala3)
  val platforms = List(VirtualAxis.jvm, VirtualAxis.js, VirtualAxis.native)

  val hearth = "0.3.0-8-gc90a7fe-SNAPSHOT"
  val refined = "0.11.3"
  val munit = "1.2.4"

  def fold[A](scalaVersion: String)(for2_13: => Seq[A], for3: => Seq[A]): Seq[A] =
    CrossVersion.partialVersion(scalaVersion) match {
      case Some((2, 13)) => for2_13
      case Some((3, _))  => for3
      case _             => Seq.empty
    }
}

// Common settings:

val settings = Seq(
  git.useGitDescribe := true,
  git.uncommittedSignifier := None,
  scalacOptions ++= versions.fold(scalaVersion.value)(
    for2_13 = Seq("-Xsource:3", "-language:implicitConversions"),
    for3 = Seq("-language:implicitConversions")
  )
)

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
  git.gitUncommittedChanges := git.gitCurrentTags.value.isEmpty,
  git.uncommittedSignifier := Some("SNAPSHOT")
)

val noPublishSettings =
  Seq(publish / skip := true, publishArtifact := false)

val resolverSettings = Seq(
  resolvers += mavenCentralSnapshots,
  resolvers += Resolver.mavenLocal
)

// Cross-quotes plugin for Scala 3:

val useCrossQuotes = versions.scalas.flatMap { scalaVersion =>
  versions.fold(scalaVersion)(
    for2_13 = List(
      MatrixAction {
        case (version, List(VirtualAxis.jvm)) => version.isScala2
        case _                                => false
      }.Configure(_.settings(libraryDependencies += "com.kubuszok" %% "hearth-cross-quotes" % versions.hearth)),
      MatrixAction {
        case (version, List(VirtualAxis.js)) => version.isScala2
        case _                               => false
      }.Configure(_.settings(libraryDependencies += "com.kubuszok" %% "hearth-cross-quotes" % versions.hearth)),
      MatrixAction {
        case (version, List(VirtualAxis.native)) => version.isScala2
        case _                                   => false
      }.Configure(_.settings(libraryDependencies += "com.kubuszok" %% "hearth-cross-quotes" % versions.hearth))
    ),
    for3 = List(
      MatrixAction
        .ForScala(_.isScala3)
        .Configure(
          _.settings(
            libraryDependencies += "com.kubuszok" %% "hearth-cross-quotes" % versions.hearth % Provided,
            scalacOptions ++= {
              val pluginJar = (Compile / dependencyClasspath).value
                .find(_.data.getName.contains("hearth-cross-quotes"))
                .map(_.data.getAbsolutePath)
                .getOrElse(sys.error("hearth-cross-quotes jar not found on classpath"))
              Seq(s"-Xplugin:$pluginJar")
            }
          )
        )
    )
  )
}

// Modules:

lazy val root = project
  .in(file("."))
  .enablePlugins(GitVersioning, GitBranchPrompt)
  .settings(publishSettings)
  .settings(noPublishSettings)
  .settings(
    name := "refined-compat-root",
    commands += Command.command("ci-release") { state =>
      val extracted = Project.extract(state)
      val tags = extracted.get(git.gitCurrentTags)
      val cmd = if (tags.nonEmpty) "publishSigned ; sonaRelease" else "publishSigned"
      cmd :: state
    }
  )
  .aggregate(compat.projectRefs *)
  .aggregate(tests.projectRefs *)

lazy val compat = projectMatrix
  .in(file("compat"))
  .someVariations(versions.scalas, versions.platforms)(useCrossQuotes *)
  .enablePlugins(GitVersioning, GitBranchPrompt)
  .settings(
    moduleName := "refined-compat",
    name := "refined-compat",
    description := "Compile-time refinement validation for refined types on Scala 3, powered by Hearth's semiEval"
  )
  .settings(settings *)
  .settings(publishSettings *)
  .settings(resolverSettings *)
  .settings(
    libraryDependencies ++= Seq(
      "com.kubuszok" %%% "hearth" % versions.hearth,
      "eu.timepit" %%% "refined" % versions.refined
    )
  )

lazy val tests = projectMatrix
  .in(file("tests"))
  .someVariations(versions.scalas, List(VirtualAxis.jvm))()
  .enablePlugins(GitVersioning, GitBranchPrompt)
  .settings(
    moduleName := "refined-compat-tests",
    name := "refined-compat-tests"
  )
  .settings(settings *)
  .settings(publishSettings *)
  .settings(noPublishSettings *)
  .settings(resolverSettings *)
  .settings(
    libraryDependencies ++= Seq(
      "org.scalameta" %%% "munit" % versions.munit % Test
    )
  )
  .dependsOn(compat)
