(ns dragonmark.spiffy.server
  (:require [compojure.route :as route]
            [ring.middleware.cookies :as ring-cookies]
            [ring.middleware.params :as ring-params]
            [org.httpkit.server :as hk]
            [dragonmark.util.props :as dup]
            [dragonmark.util.core :as dc]
            [dragonmark.circulate.core :as circ]
            [schema.core :as sc]
            [clojure.core.async :as async]
            [dragonmark.spiffy.chat :as chat]
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

(def PageInfo
  {:open sc/Bool
   :last-closed-guid (sc/maybe sc/Str)
   :source-chan sc/Any ;; Really a channel
   :dest-chan sc/Any ;; Really a channel
   :transport sc/Any ;; the transport
   :root sc/Any ;; the root Channel
   :data sc/Any})

(def SessionInfo
  {:last sc/Num
   :hits sc/Num
   :guid sc/Str
   :pages {sc/Str PageInfo}
   :user-guid (sc/maybe sc/Str)
   (sc/optional-key  :user-data) sc/Any
   })


(defonce ^:private sessions
;;   "The session keeper.

;; Sessions:

;; GUID -> SessionInfo"
  (atom {}))

(sc/defn update-session! :- {}
  "update the session and the given path."
  [session-id :- sc/Str
   path :- [sc/Any]
   fn ;; :- Ifn
   & params]
  (get
   (swap! sessions #(apply update-in % (cons session-id path) fn params))
   session-id
   ))

(sc/defn assoc-session! :- {}
  "Assoc  the session and the given path"
  [session-id :- sc/Str
   path :- [sc/Any]
   value :- sc/Any]
  (get
   (swap! sessions #(apply  assoc-in % (cons session-id path) value))
   session-id))

(sc/defn get-session :- {}
  "Get a value out of the session"
  [guid :- sc/Str
   path :- [sc/Any]]
  (let [init  (get @sessions guid)]
    (loop [value init path path]
      (if (empty? path)
        value
        (recur
         (get value (first path))
         (rest path))))))

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
                     :root (circ/build-root-channel
                            {}
                            (str "Session: " new-guid)
                            @circ/env-root)
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

(defn ^:private shut-down-page
  "Shut down the page"
  [the-page]
  nil
  )
(sc/defn ^:private shut-down-session
  "Shut the session down"
  [session]
  (doall
   (map shut-down-page (-> session :pages vals))))

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
                  (vals x)
                  (filter #(< (-> % :last) then) x)
                  (map :guid x))]
        (dorun
         (map #(let [removed (get @sessions %)]
                 (shut-down-session removed)
                 (swap! sessions dissoc %)) to-remove))
        )
      (recur))))

(defn ^:private find-or-build-page-info
  "find or build a PageInfo object"
  [guid pageid]
  (let [ret (atom nil)]
    (update-session!
     guid [:page pageid]
     (fn [page-info]
       (let [page-info
             (or page-info
                 (let [count-atom (atom 0)
                       source-chan (async/chan)
                       dest-chan (async/chan)
                       root (circ/build-root-channel
                             {"get" (fn [msg env]
                                      @count-atom)
                              "inc" (fn [msg env]
                                      (swap! count-atom inc))}
                             (str "Page: " pageid)
                             (get-session guid [:root]))
                       transport (circ/build-transport
                                  root
                                  source-chan dest-chan)]
                   {:open true
                    :last-closed-guid nil
                    :source-chan source-chan
                    :dest-chan dest-chan
                    :root root
                    :transport transport
                    :data {}}
                   ))

             page-info (merge page-info
                              {:open true
                               :last-closed-guid nil})]
         (reset! ret page-info)
         page-info
         )))
    @ret))

(defn app [request]
  (cond
   (and
    (:websocket? request)
    (= "/core" (:uri request)))
   (let [pageid (-> request :params (get "pageid"))]
     (hk/with-channel request channel    ; get the channel
       (if (and
            (hk/websocket? channel) ; if you want to distinguish them
            pageid
            )
         (let [session (find-or-build (-> request :cookies (get cookie-name)))
               guid (:guid session)
               {:keys [source-chan
                       dest-chan]} (find-or-build-page-info guid pageid)]

           (async/go
             (loop []
               (let [info (async/<! dest-chan)]
                 (when (string? info)
                   (hk/send! channel info)
                   (recur)
                   ))))

           (hk/on-close
            channel
            (fn [status]
              (async/put! dest-chan :closed)
              (let [my-guid (dc/next-guid)]
                (update-session!
                 guid [:pages pageid]
                 (fn [j]
                   (as-> j x
                         (assoc x :open false)
                         (assoc x :last-closed-guid my-guid))))
                (async/go
                  (async/<! (async/timeout 60000))
                  (let [page-info (get-session guid
                                               [:pages pageid])]
                    (when (and (not (:open page-info))
                               (= my-guid
                                  (:last-closed-guid page-info)))
                      (shut-down-page page-info)
                      ))))))

           (hk/on-receive channel (fn [data]     ; two way communication
                                    (async/put! source-chan data))))

         )))

   (= "/setup" (:uri request))
   (let [session (find-or-build (-> request :cookies (get cookie-name)))]

     {:body "got it"
      :cookies {cookie-name (:guid session)}
      :headers {"Content-Type" "text/plain"}
      })

   :else (static request)

   ))

(defn build-server-root
  "Builds the root service for the application"
  []
  (let [base (circ/build-root-channel {} "base" nil)
        chat-service chat/chat-service]
    ;; register the chat service
    (circ/gofor
     [_ (add base {:channel chat-service
                   :public true
                   :service "chat"})]
     nil ;; (println "Added chat")
     :error (println "Add error " &err))

    base))

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
  (reset! circ/env-root (build-server-root))
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
