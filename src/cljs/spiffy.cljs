(ns spiffy
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [clojure.browser.repl :as repl]))

(js/console.log "Hi")

(js/console.log (. js/document (getElementById "my-app")))


(defn widget [data owner]
  (reify
    om/IRender
    (render [this]
      (dom/h1 nil (:text data)))))

(def app-state (atom {:text "Hello world, mr yak!"}))

(om/root widget app-state
         {:target (. js/document (getElementById "my-app"))})

(swap! app-state assoc :text "It's aliv,ceee")

(repl/connect "http://localhost:9000/repl")

(def ws 
  (let [w (new js/WebSocket "ws://localhost:8081/core")]
    (set! (.-onmessage w) (fn [message] (println "Message " (.-data message))))
    (set! (.-onopen w) (fn [me] (println "open " me)))
    (set! (.-onclose w) (fn [me] (println "closed " me)))
    w))

;; (println "foo")

;; (.-readyState ws)

;; (.send ws "he;")

;; (js/alert "jjj")
