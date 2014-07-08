(ns server
  (:require [compojure.route :as route]
            [org.httpkit.server :as hk])
  (:use compojure.core)
  (:import [java.io File])
  )

;; the server instance
(defonce server (atom nil))

(defroutes static
  (GET "/" [] (File. "resources/public/index.html"))
  (route/resources "/")
  (route/not-found "<h1>Page not found</h1>"))

(defn app [request] 
  (static request))

(defn stop-server []
  (when-not (nil? @server)
    ;; graceful shutdown: wait 100ms for existing requests to be finished
    ;; :timeout is optional, when no timeout, stop immediately
    (@server :timeout 100)
    (reset! server nil)))

(defn -main [ & args]
  ;; The #' is useful, when you want to hot-reload code
  ;; You may want to take a look: https://github.com/clojure/tools.namespace
  ;; and http://http-kit.org/migration.html#reload
  (println "Running")
  (reset! server (hk/run-server #'app {:port 8080})))
