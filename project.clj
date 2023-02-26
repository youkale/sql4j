(defproject sql4j "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://github.com/youkale/sql4j"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url  "https://www.eclipse.org/legal/epl-2.0/"}

  :global-vars {*warn-on-reflection* false}

  :dependencies [[com.layerware/hugsql "0.5.3"]
                 [org.clojure/tools.logging "1.2.4"]]
  :source-paths ["src/clojure"]
  :java-source-paths ["src/java"]
  :test-paths ["test/clojure"]

  :aot :all

  :pom-plugins [[org.apache.maven.plugins/maven-compiler-plugin "3.8.1"
                 (:configuration
                   [:source "11"]
                   [:target "11"])]
                [com.theoryinpractise/clojure-maven-plugin "1.8.4"
                 (:configuration
                   [:sourceDirectories [:sourceDirectory "src/clojure"]])]]

  :profiles {
             :dev     {:dependencies [[hikari-cp "3.0.1"]
                                      [ch.qos.logback/logback-classic "1.4.5"]
                                      [com.h2database/h2 "2.1.214"]
                                      [org.clojure/clojure "1.11.1"]]}
             :1.8     {:dependencies [[org.clojure/clojure "1.8.0" :upgrade? false]]}
             :1.9     {:dependencies [[org.clojure/clojure "1.9.0" :upgrade? false]]}
             :1.10    {:dependencies [[org.clojure/clojure "1.10.0" :upgrade? false]]}
             :1.11    {:dependencies [[org.clojure/clojure "1.11.1" :upgrade? false]]}
             :jmh     {:jvm-opts ["-Dclojure.compiler.direct-linking=true"]}
             :perf    {:jvm-opts ^:replace ["-server"
                                            "-Xmx4096m"
                                            "-Dclojure.compiler.direct-linking=true"]}

             :uberjar {:omit-source  false
                       :aot          :all
                       :jvm-opts
                       ["-XX:+IgnoreUnrecognizedVMOptions"
                        "-Xverify:none"
                        "-Djava.awt.headless=true"
                        "-Dclojure.compiler.direct-linking=true"
                        "-XX:-OmitStackTraceInFastThrow"]
                       :uberjar-name "sql4j.jar"}}

  :aliases {"all"  ["with-profile" "default:dev:default:dev,1.8:dev,1.9,1.10,1.11"]
            "perf" ["with-profile" "default,dev,perf"]
            "repl" ["with-profile" "default,dev" "repl"]})
