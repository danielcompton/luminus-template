(ns <<project-ns>>.core
  (:require [<<project-ns>>.handler :refer [app init destroy]]
            [luminus.repl-server :as repl]
            [luminus.http-server :as http]<% if relational-db %>
            [<<project-ns>>.db.migrations :as migrations]<% endif %>
            [environ.core :refer [env]])
  (:gen-class))

(defn parse-port [port]
  (when port
    (cond
      (string? port) (Integer/parseInt port)
      (number? port) port
      :else          (throw (Exception. (str "invalid port value: " port))))))

(defn http-port [port]
  (parse-port (or port (env :port) 3000)))

(defn stop-app []
  (repl/stop)
  (http/stop destroy)
  (shutdown-agents))

(defn start-app
  "e.g. lein run 3000"
  [[port]]
  (let [port (http-port port)]
    (.addShutdownHook (Runtime/getRuntime) (Thread. stop-app))
    (when-let [repl-port (env :nrepl-port)]
      (repl/start (parse-port repl-port)))
    (http/start {:handler app
                 :init    init
                 :port    port})))

(defn -main [& args]
  <% if relational-db %>(cond
    (some #{"migrate" "rollback"} args)
    (do (migrations/migrate args) (System/exit 0))
    :else
    (start-app args)))
  <% else %>(start-app args))<% endif %>
