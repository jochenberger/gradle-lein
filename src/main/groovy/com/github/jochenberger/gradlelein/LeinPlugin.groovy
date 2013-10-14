package com.github.jochenberger.gradlelein

import org.gradle.api.Plugin
import org.gradle.api.Project

import clojure.lang.Keyword
import clojure.lang.RT
import clojure.lang.Symbol
import clojure.lang.Var

class LeinPlugin implements Plugin<Project>{

  @Override
  public void apply(Project p) {

    p.extensions.create('lein', LeinPluginExtension)

    Var require = RT.var("clojure.core", "require");
    require.invoke(Symbol.create("leiningen.core.main"));
    require.invoke(Symbol.create("leiningen.core.user"));
    require.invoke(Symbol.create("leiningen.core.project"));
    require.invoke(Symbol.create("leiningen.core.classpath"));
    Var userInit = Var.find(Symbol.create("leiningen.core.user/init"));
    userInit.invoke();

    Var readProject = Var.find(Symbol.create("leiningen.core.project/read"));

    Map cljProject = (Map) readProject.invoke(p.file('project.clj').absolutePath);

    p.logger.info("Project = {}", cljProject);

    List deps = cljProject.get(Keyword.find("dependencies"));
    def addedConfigurations = [];

    deps.each { dep->
      def name = dep.get(0);
      def groupId = name.getNamespace();
      def artifactId = name.getName();
      def version = dep.get(1);
      p.logger.info("dep = {} ", dep);

      int indexOfScope = dep.indexOf(Keyword.find("scope"));
      String scope = (String) (indexOfScope >= 0 ? dep.get(indexOfScope + 1) : null);

      if (scope == null) {
        scope = "compile";
      }
      p.logger.debug("scope =" + scope);

      List exclusions = null
      int indexOfExclusions = dep.indexOf(Keyword.find("exclusions"));
      if (indexOfExclusions >= 0) {
        exclusions = (List) dep.get(indexOfExclusions + 1);
        p.logger.debug("Exclusions = {}" , exclusions);
      }

      String gradleDepString = "$groupId:$artifactId:$version";
      if (!addedConfigurations.contains(scope)){
        p.logger.debug("Adding configuration {}" , scope);
        addedConfigurations.add(scope)
        p.configurations.create(scope)
      }
      def dependency = p.dependencies.add(scope,gradleDepString)
      if (exclusions !=null){
        exclusions.each {exDep->
          def exName = exDep.get(0);
          def exGroupId = exName.getNamespace();
          def exArtifactId = exName.getName();
          p.logger.debug("EXCLUDE $exGroupId:$exArtifactId")
          dependency.exclude group: exGroupId, module: exArtifactId
        }
      }
    }
  }
}
