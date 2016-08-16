

package com.github.jochenberger.gradlelein

import groovy.transform.TypeChecked

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ModuleDependency

import clojure.lang.Keyword
import clojure.lang.PersistentVector
import clojure.lang.RT
import clojure.lang.Symbol
import clojure.lang.Var

class LeinPlugin implements Plugin<Project>{

  @Override
  @TypeChecked
  public void apply(Project p) {
    p.logger.debug("Applying plugin")
    p.extensions.create('lein', LeinPluginExtension)
    def previousClassLoader = Thread.currentThread().contextClassLoader
    try {
      Thread.currentThread().contextClassLoader = RT.class.classLoader
      Var require = RT.var("clojure.core", "require");
      require.invoke(Symbol.create("leiningen.core.main"));
      require.invoke(Symbol.create("leiningen.core.user"));
      require.invoke(Symbol.create("leiningen.core.project"));
      require.invoke(Symbol.create("leiningen.core.classpath"));
      Var userInit = Var.find(Symbol.create("leiningen.core.user/init"));
      p.logger.debug("Initializing Leiningen")

      userInit.invoke();
      p.logger.debug("Reading project")
      Var readProject = Var.find(Symbol.create("leiningen.core.project/read-raw"));

      Map cljProject = (Map) readProject.invoke(p.file('project.clj').absolutePath);

      p.logger.info("Project = {}", cljProject);

      List deps = cljProject.get(Keyword.find("dependencies"));
      def addedConfigurations = [];

      deps.each { Object o->
        PersistentVector dep = (PersistentVector) o;
        Symbol name = (Symbol) dep.get(0);

        String groupId = name.getNamespace();
        String artifactId = name.getName();
        String version = dep.get(1);
        p.logger.info("dep = {} ", dep);

        int indexOfScope = dep.indexOf(Keyword.find("scope"));
        String scope = (String) (indexOfScope >= 0 ? dep.get(indexOfScope + 1) : null);

        if (scope == null) {
          scope = "compile";
        }
        p.logger.debug("scope = {}", scope);

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
        ModuleDependency dependency = (ModuleDependency) p.dependencies.add(scope, gradleDepString)
        if (exclusions != null){
          exclusions.each { Object eo->
            PersistentVector exDep = (PersistentVector) eo;
            Symbol exName = (Symbol) exDep.get(0);
            String exGroupId = exName.getNamespace();
            String exArtifactId = exName.getName();
            p.logger.debug("EXCLUDE $exGroupId:$exArtifactId")
            dependency.exclude group: exGroupId, module: exArtifactId
          }
        }
      }
    } finally{
      Thread.currentThread().contextClassLoader = previousClassLoader
    }
  }
}
