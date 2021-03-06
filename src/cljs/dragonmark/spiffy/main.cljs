(ns dragonmark.spiffyf.main
  (:require-macros [schema.macros :as sc]
                   [dragonmark.circulate.core :as circ]
                   [cljs.core.async.macros :as async :refer [go]])
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [dragonmark.util.core :as dc]
            [dragonmark.circulate.core :as circ]
            [schema.core :as sc]
            [cljs.core.async :as async]
            [cljs.core.async.impl.protocols :as asyncp]

            [clojure.browser.repl :as repl]))

(enable-console-print!)

;; is the connection secure?
(def secure? (boolean (= "https:" (.-protocol js/location))))

(def hostname (.-hostname js/location))

(def protocol (.-protocol js/location))


(def port (.-port js/location))

(defonce page-id (dc/next-guid))

(defonce source-chan (async/chan))

(defonce dest-chan (async/chan))
(defonce root (circ/build-root-channel
               {}))

(defonce
  transport (circ/build-transport
             root
             source-chan dest-chan))

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
      (dom/h3 nil (:text data))
      )))

(def chat-server (atom nil))

(defn- send-chat
  [data owner]
  (let [the-node (om/get-node owner "chat-in")
        chat-string (.-value the-node)
        chat-server @chat-server]
    (when (and
           chat-server
           (not (asyncp/closed? chat-server))
           chat-string)
      ;; send the message to the chat server
      (async/put! chat-server
                  {:msg chat-string
                   :_cmd "send-message"})

      ;; clear the input
      (aset the-node "value" "")
      ))
  )

(sc/defn ^{:service true} chat-out :- sc/Any
  "Hi dude"
  [data :- sc/Str
   owner :- {}]
  (reify
    om/IRender
    (render [this]
      (dom/div
       nil
       (dom/ul nil
               (clj->js
                (map-indexed #(dom/li #js
                                      {:key (str "i-" %1)} %2)
                             (:chat data))))
       (dom/input #js {:type "text" :ref "chat-in"})
       (dom/button #js {:onClick (fn [] (send-chat data owner))}
                   "Chat"))
      )))

(def app-state
  (atom
   {
    :text "Hello world!"
    :chat []
    }))

(if-let [target (. js/document (getElementById "my-app"))]
  (om/root widget app-state
           {:target target}))

(if-let [target (. js/document (getElementById "chat-area"))]
  (om/root chat-out app-state
           {:target target}))


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
                                     (async/put! source-chan (.-data  message))
                                     ;; (js/console.log "Message " (.-data message))
                                     ))
             (set! (.-onopen w) (fn [me]
                                  (async/go
                                    (loop []
                                      (let [info (async/<! dest-chan)]
                                        (when (string? info)
                                          (.send w info)
                                          (recur)
                                          ))))
                                  (reset! server-socket w)))
             (set! (.-onerror w) (fn [error]
                                   (async/put! dest-chan :closed)
                                   (reset! server-socket nil)
                                   (js/setTimeout setup-server-socket 100)
                                   ;; (log "Web Socket Error: " error)
                                   ))
             (set! (.-onclose w) (fn [me]
                                   (async/put! dest-chan :closed)
                                   (reset! server-socket nil)
                                   (js/setTimeout setup-server-socket 100)
                                   ;; (log "closed " me)
                                   ))
             w)

           ))
  )

(setup-server-socket)

(defn make-remote-calls
  []

  (circ/gofor
   [answer (inc (circ/remote-root transport))]
   (do
     (swap! app-state assoc :text (str "Remote count: " answer))
     (js/setTimeout make-remote-calls 1000)
     )
   ))

(make-remote-calls)

(def chat-listener (async/chan))

(go
  (loop []
    (let [info (async/<! chat-listener)]
      (if (nil? info)
        nil
        (do
          (if (sequential? info)
            (swap! app-state assoc :chat info)
            (swap! app-state #(update-in % [:chat]
                                         (fn [x]
                                           (->> (conj x info)
                                                (take-last 30)
                                                vec)))))
          (recur))))))


(circ/gofor
 :let [other-root (circ/remote-root transport)]
 [the-chat-server (locate-service other-root {:service "chat"})]
 [_ (add-listener the-chat-server {:the-chan chat-listener})]
 (reset! chat-server the-chat-server)
 :error (.log js/console "Got error " &err " var " &var)
 )
