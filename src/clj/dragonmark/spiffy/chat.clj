(ns dragonmark.spiffy.chat
  (:require
   [clojure.core.async :as async]
   [dragonmark.circulate.core :as circ]))

(defonce ^:private listeners (atom []))

(defonce ^:private messages (atom []))

(defn ^:service add-listener
  "Adds a listener. Pass in a channel to
get messages"
  [the-chan]
  (swap! listeners conj the-chan)
  (into [] (take 50 @messages)))

(defn ^:service remove-listener
  "Remove the listener"
  [the-chan]
  (swap! listeners (fn [vec] (filterv #(not (= the-chan %)) vec)))
  true)

(defn ^:service send-message
  "Sends a message into the chat stream"
  [msg]
  (if (string? msg)
    (do
      (swap! messages conj msg)
      (swap! listeners (fn [x]
                         (filterv
                          (fn [c] (not (circ/chan-closed? c))) x)))
      (mapv (fn [c] (async/put! c msg)) @listeners)
      true)
    false))

(def chat-service (circ/build-service))
