import sbtcrossproject.CrossPlugin.autoImport.{crossProject, CrossType}
val dottyVersion = "0.9.0-RC1"
val scala211Version = "2.11.12"
val scala212Version = "2.12.6"
val scala213Version = "2.13.0-M4"

lazy val commonSettings = Seq(

  organization := "org.emergentorder.onnx",
//  crossScalaVersions := Seq(dottyVersion, "2.10.7", "2.11.12",scala212Version, "2.13.0-M5"),
  version      := "1.2.2-0.1.0-SNAPSHOT",
  scalacOptions ++= Seq("-feature", "-unchecked", "-deprecation"),
  autoCompilerPlugins := true
//  wartremoverErrors ++= Warts.allBut(Wart.DefaultArguments, Wart.Nothing, Wart.ToString),
//  wartremoverExcluded += baseDirectory.value / "core" / "src" / "main" / "scala" / "Float16.scala"
)

lazy val common = (crossProject(JSPlatform, JVMPlatform, NativePlatform)
    .crossType(CrossType.Pure) in file("common"))
  .settings( commonSettings,
    name := "onnx-scala-common"
  )
  .jvmSettings(
    crossScalaVersions := Seq(dottyVersion, scala212Version, scala213Version, scala211Version)
  )
  .jsSettings(
    crossScalaVersions := Seq(scala212Version, scala211Version)
  )
  .nativeSettings(
    scalaVersion := scala211Version
  )

lazy val commonJS     = common.js.disablePlugins(dotty.tools.sbtplugin.DottyPlugin).disablePlugins(dotty.tools.sbtplugin.DottyIDEPlugin)

lazy val core = (crossProject(JSPlatform, JVMPlatform, NativePlatform)
    .crossType(CrossType.Pure) in file("core")).dependsOn(common)
  .settings(commonSettings,
    name := "onnx-scala",
    scalaVersion := scala212Version
    )
    .jvmSettings(
      crossScalaVersions := Seq(scala212Version, scala213Version, scala211Version),
      libraryDependencies ++= Seq(

      ),
      libraryDependencies ++= (CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, n)) if n == 13 => Seq("org.typelevel" % "spire_2.12" % "0.16.0",
                                            "org.typelevel" % "cats-free_2.12" % "1.3.1",
                                            "org.typelevel" % "cats-effect_2.12" % "1.0.0",
                                            "io.frees" % "frees-core_2.12" % "0.8.2",
                                            "eu.timepit" % "singleton-ops_2.12" % "0.3.0"
                                           )
        case _ => Seq("org.typelevel" %% "spire" % "0.16.0",
                      "org.typelevel" %% "cats-free" % "1.3.1",
                      "org.typelevel" %% "cats-effect" % "1.0.0",
                      "io.frees" %% "frees-core" % "0.8.2",
                      "eu.timepit" %% "singleton-ops" % "0.3.0"
                  )
      })
    )
    .jsSettings(
      crossScalaVersions := Seq(scala212Version, scala211Version),
      libraryDependencies ++= Seq("org.typelevel" %%% "spire" % "0.16.0",
        "org.typelevel" %%% "cats-free" % "1.3.1",
        "org.typelevel" %%% "cats-effect" % "1.0.0",
        "eu.timepit" %%% "singleton-ops" % "0.3.0",
        "io.frees" %%% "frees-core" % "0.8.2"
      )
    )
    .nativeSettings(
      scalaVersion := scala211Version,
      libraryDependencies ++= Seq("org.typelevel" %% "spire" % "0.16.0",
        "org.typelevel" %% "cats-free" % "1.3.1",
        "org.typelevel" %% "cats-effect" % "1.0.0",
        "eu.timepit" %% "singleton-ops" % "0.3.0",
        "io.frees" %% "frees-core" % "0.8.2",
      )
    )

lazy val coreDotty = (crossProject(JVMPlatform)
  .crossType(CrossType.Pure)).in(file("coreDotty")).dependsOn(common)
  .enablePlugins(dotty.tools.sbtplugin.DottyPlugin)
  .settings( commonSettings,
    name := "onnx-scala",
    scalaVersion := dottyVersion,
    scalacOptions ++= { if (isDotty.value) Seq("-language:Scala2") else Nil },
    libraryDependencies ++= Seq(
      ("org.typelevel" %% "spire" % "0.16.0").withDottyCompat(dottyVersion),
      ("eu.timepit" %% "singleton-ops" % "0.3.0").withDottyCompat(dottyVersion),
      ("org.typelevel" %% "cats-free" % "1.3.1").withDottyCompat(dottyVersion),
      ("org.typelevel" %% "cats-effect" % "1.0.0").withDottyCompat(dottyVersion)
    )
)

lazy val free = (crossProject(JSPlatform, JVMPlatform, NativePlatform)
    .crossType(CrossType.Pure) in file("free")).dependsOn(core)
  .disablePlugins(dotty.tools.sbtplugin.DottyPlugin)
  .settings( commonSettings,
    name := "onnx-scala-free", 
    scalaVersion := scala212Version,
    publishArtifact in (Compile, packageDoc) := false,
    addCompilerPlugin("org.spire-math" % "kind-projector" % "0.9.7" cross CrossVersion.binary),
    scalacOptions ++= (CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, n)) if n == 13 => Seq("-Ymacro-annotations"
                                           )
        case _ => Seq(
                  )
    }),
    libraryDependencies ++= (CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, n)) if n != 13 => Seq( compilerPlugin("org.scalameta" % "paradise" % "3.0.0-M11" cross CrossVersion.full)
                                           )
        case _ => Seq(
                  )
    }) 
  )
  .jvmSettings(
    crossScalaVersions := Seq(scala212Version, scala213Version, scala211Version)
  )
  .jsSettings(
    crossScalaVersions := Seq(scala212Version, scala211Version)
  )
  .nativeSettings(
    scalaVersion := scala211Version
  )

lazy val freeDotty = (crossProject(JVMPlatform)
    .crossType(CrossType.Pure) in file("freeDotty")).dependsOn(coreDotty)
  .enablePlugins(dotty.tools.sbtplugin.DottyPlugin)
  .settings( commonSettings,
    name := "onnx-scala-free",
    scalaVersion := dottyVersion,
    publishArtifact in (Compile, packageDoc) := false,
    libraryDependencies ++= Seq(
      ("io.frees" %% "frees-core" % "0.8.2").withDottyCompat(dottyVersion),
      (compilerPlugin("org.scalameta" % "paradise_2.12.6" % "3.0.0-M11")
    )
  )

)
