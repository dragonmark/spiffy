(ns spiffy.server
  (:require [compojure.route :as route]
            [ring.middleware.cookies :as ring-cookies]
            [ring.middleware.params :as ring-params]
            [org.httpkit.server :as hk]
            [dragonmark.util.props :as dup]
            [dragonmark.util.core :as dc]
            [schema.core :as sc]
            [clojure.core.async :as async]
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

(defonce ^:private sessions
;;   "The session keeper.

;; Sessions:

;; GUID -> SessionInfo"
  (atom {}))

(def SessionInfo
  {:last sc/Num
   :hits sc/Num
   :guid sc/Str
   :server (sc/maybe sc/Any)
   :client (sc/maybe sc/Any)
   :user-guid (sc/maybe sc/Str)
   (sc/optional-key  :user-data) sc/Any
   })

(def cookie-name "dragonmark-session")

(sc/defn ^:private find-or-build :- SessionInfo
  "Find a session or build a session"
  [guid]
  (let [session
        (or
         (get @sessions guid)
         (let [new-guid (dc/next-guid)
               init {:guid new-guid
                     :last (System/currentTimeMillis)
                     :hits 0
                     :server {}
                     :client {}}]
           (swap! sessions assoc new-guid init)
           init)
         )
        guid (:guid session)]
    (swap! sessions assoc-in [guid :last] (System/currentTimeMillis))
    (swap! sessions update-in [guid :hits] inc)
    session
    ))

(defonce ^:private harvest
  ;; harvest the expired sessions
  (async/go
    (loop []
      (let [timeout (async/timeout 500)
            _ (async/<! timeout) ;; wait half a second
            then (-  (System/currentTimeMillis)
                     (* 15 1000 60) ;; 15 minutes without activity
                     )
            to-remove
            (as-> @sessions
                  x
                  vals
                  (filter #(< (-> % :last) then) x)
                  :guid)
            ]
        (dorun
         (map #(swap! sessions dissoc %) to-remove))
        )
      (recur))))

(defn app [request]
  (cond
   (and
    (:websocket? request)
    (= "/core" (:uri request)))

   (hk/with-channel request channel    ; get the channel
     (if (hk/websocket? channel)            ; if you want to distinguish them
       (do
         (println "Cookies " (some-> request :cookies))
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
   (let [session (find-or-build (-> request :cookies (get cookie-name)))]
     (println request)
     {:body "got it"
      :cookies {cookie-name (:guid session)}
      :headers {"Content-Type" "text/plain"}
      })


   :else (static request)

   ))

(defn run-cljs-repl
  "Make the right piggieback calls to start a ClojureScript repl.
This should be part of a workflow where the REPL is started in one
session (e.g. Emacs browser instance) and used for ClojureScript stuff"
  []
  (cemerick.piggieback/cljs-repl
   :repl-env
   (cljs.repl.browser/repl-env
    :port (or
           (some-> @dup/info :http :port :repl)
           9000))))

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
  (reset! server
          (hk/run-server
           (-> #'app ring-cookies/wrap-cookies
               ring-params/wrap-params)
           {:port
            (or
             (some-> @dup/info :http :port :main)
             8080)})))

(defn -main [ & args]
  (start-server))
