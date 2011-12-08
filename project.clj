(defproject nibblonian "0.0.5-SNAPSHOT"
  :description "RESTful interface into iRODS."
  :dependencies [[org.clojure/clojure "1.2.0"]
                 [org.clojure/clojure-contrib "1.2.0"]
                 [org.clojure/java.jdbc "0.1.0"]
                 [postgresql/postgresql "9.0-801.jdbc4"]
                 [compojure "0.6.5"]
                 [org.irods.jargon/jargon-core "3.0.1-SNAPSHOT"]
                 [org.irods.jargon/jargon-test "3.0.1-SNAPSHOT"]
                 [org.irods.jargon/jargon-data-utils "3.0.1-SNAPSHOT"]
                 [org.irods.jargon.transfer/jargon-transfer-engine "3.0.1-SNAPSHOT"]
                 [org.irods.jargon/jargon-security "3.0.1-SNAPSHOT"]
                 [log4j/log4j "1.2.16"]
                 [swank-clojure "1.3.1"]]
  :dev-dependencies [[lein-ring "0.4.5"]]
  :ring {:handler nibblonian.core/app}
  :repositories {"iplantCollaborative"
                 "http://projects.iplantcollaborative.org/archiva/repository/internal/"
                 
                 "renci.repository"
                 "http://ci-dev.renci.org/nexus/content/repositories/snapshots/"})
