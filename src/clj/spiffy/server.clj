(ns spiffy.server
  (:require [compojure.route :as route]
            [org.httpkit.server :as hk]
            cljs.repl.browser
            cemerick.piggieback)
  (:use compojure.core)
  (:import [java.io File])
  )

;; the server instance
(defonce server (atom nil))

(defroutes static
  (GET "/" [] (File. "resources/public/index.html"))
  (route/resources "/")
  (route/not-found "<h1>Page not found</h1>"))

(def client-chan (atom nil))

(defn app [request] 
  (cond
   (and
    (:websocket? request)
    (= "/core" (:uri request)))
   
   (hk/with-channel request channel    ; get the channel
     (if (hk/websocket? channel)            ; if you want to distinguish them
       (do
         (println "Request keys " (some-> request :headers (get "cookie")))
         (reset! client-chan channel)
         (hk/on-close channel 
                      (fn [status] 
                        (reset! client-chan nil)
                        (println "Channel closed " status)))
         
         (hk/on-receive channel (fn [data]     ; two way communication
                                  (println "Got " data)
                                  (hk/send! channel (str "Client sez: " data))))
         )
       
       ))
   
   (= "/setup" (:uri request))
   (do
     (println "REquest for setup")
     {:body "got it"
      :headers {"Set-Cookie" '("frut=bat")}}
     )

   
   :else (static request)
    
    ))

(defn run-cljs-repl
  "Make the right piggieback calls to start a ClojureScript repl.
This should be part of a workflow where the REPL is started in one 
session (e.g. Emacs browser instance) and used for ClojureScript stuff"
  []
  (cemerick.piggieback/cljs-repl
   :repl-env (cljs.repl.browser/repl-env :port 9000)))

(defn stop-server []
  (when-not (nil? @server)
    ;; graceful shutdown: wait 100ms for existing requests to be finished
    ;; :timeout is optional, when no timeout, stop immediately
    (@server :timeout 100)
    (reset! server nil)))

(defn start-server
  "start an http-kit instance at port 8080"
  []
  ;; The #' is useful, when you want to hot-reload code
  ;; You may want to take a look: https://github.com/clojure/tools.namespace
  ;; and http://http-kit.org/migration.html#reload
  (println "Running")
  (reset! server (hk/run-server #'app {:port 8080})))

(defn -main [ & args]
  (start-server))
