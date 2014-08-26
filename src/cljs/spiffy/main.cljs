(ns spiffy.main
  (:require-macros [schema.macros :as sc])
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [dragonmark.util.core :as dc]
            [schema.core :as sc]
            [clojure.browser.repl :as repl]))

(enable-console-print!)

;; is the connection secure?
(def secure? (boolean (= "https:" (.-protocol js/location))))

(def hostname (.-hostname js/location))

(def protocol (.-protocol js/location))

(def port (.-port js/location))

(defonce page-id (dc/next-guid))

(def webservice-url (str (if secure? "wss" "ws")
                         "://"
                         hostname
                         ":"
                         port
                         "/core?pageid="
                         page-id))


(def repl-url (str protocol "//" hostname ":9000/repl" ))

(def setup-url (str protocol "//" hostname ":" port  "/setup?pageid="
                    page-id))

(sc/defn ^{:service true} meow :- sc/Str "I say meow" [] "meow")

(sc/defn ^{:service true} widget :- sc/Any
  "Hi dude"
  [data :- sc/Str
   owner :- {}]
  (reify
    om/IRender
    (render [this]
      (dom/h1 nil (:text data)))))

(def app-state (atom {:text "Hello world, mr yak!"}))

(if-let [target (. js/document (getElementById "my-app"))]
  (om/root widget app-state
           {:target target}))

(swap! app-state assoc :text "It's aliv3")

(repl/connect repl-url)

;; the socket to the server
(def server-socket (atom nil))

(defn setup-server-socket
  "Sets up the web socket to the server... with nice retries and all"
  []
  (.send goog.net.XhrIo setup-url
         (fn [x]
           (js/console.log "got "  (-> x .-target .getResponse))

           (let [w (new js/WebSocket webservice-url)]
             (set! (.-onmessage w) (fn [message]
                                     (js/console.log "Message " (.-data message))))
             (set! (.-onopen w) (fn [me] (reset! server-socket w)))
             (set! (.-onerror w) (fn [error]
                                   (reset! server-socket nil)
                                   (js/setTimeout setup-server-socket 100)
                                   ;; (log "Web Socket Error: " error)
                                   ))
             (set! (.-onclose w) (fn [me]
                                   (reset! server-socket nil)
                                   (js/setTimeout setup-server-socket 100)
                                   ;; (log "closed " me)
                                   ))
             w)

           ))
  )

(setup-server-socket)

;; (ss/register 'foo/bar)
