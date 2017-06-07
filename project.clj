(defproject miniredis "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :plugins [[lein-ring "0.10.0"]]
  :ring {:handler miniredis.core/app}
  :dependencies [[org.clojure/clojure "1.8.0"] 
  				 [ring "1.6.1"]
  				 [ring/ring-json "0.4.0"]
  				 [compojure "1.6.0"]]
  :main miniredis.core)