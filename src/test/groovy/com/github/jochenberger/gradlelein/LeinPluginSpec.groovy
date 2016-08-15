package com.github.jochenberger.gradlelein

import static org.gradle.testkit.runner.TaskOutcome.*

import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.rules.TemporaryFolder

import spock.lang.Specification
import spock.lang.Unroll

class LeinPluginSpec extends Specification {
  @Rule final TemporaryFolder testProjectDir = new TemporaryFolder()
  File buildFile
  File cljBuildFile

  def setup() {
    buildFile = testProjectDir.newFile('build.gradle')
    cljBuildFile = testProjectDir.newFile('project.clj')
  }

  @Unroll
  def "Parse a simple Clojure project with Gradle #version"() {
    given:
    buildFile << """
    plugins {
        id 'com.github.jochenberger.lein'
    }
    """
    cljBuildFile << """
    (defproject leiningen.org "1.0.0"
      :description "Generate static HTML for http://leiningen.org"
      :dependencies [[enlive "1.0.1"]
                     [cheshire "4.0.0"]
                     [org.markdownj/markdownj "0.3.0-1.0.2b4"]]
      :main leiningen.web)
    """
    when:
    def result = GradleRunner.create()
        .withProjectDir(testProjectDir.root)
        .withArguments('dependencies')
        .withPluginClasspath()
        .withDebug(true)
        .withGradleVersion(version)
        .build()

    then:
    result.output.contains('enlive:enlive:1.0.1')
    result.task(":dependencies").outcome == SUCCESS
    where:
    version << [
      "2.13",
      "2.14.1",
      "3.0-milestone-1",
      "3.0-milestone-2",
      "3.0-rc-1",
      "3.0-rc-2"
    ]
  }
}