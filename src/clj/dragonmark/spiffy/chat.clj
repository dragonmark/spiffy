(ns dragonmark.spiffy.chat
  (:require
   [clojure.core.async :as async]
   [clojure.core.async.impl.protocols :as asyncp]
   [dragonmark.circulate.core :as circ]))

(defonce ^:private listeners (atom []))

(defonce ^:private messages (atom ["moo" "foo"]))

(defn ^:service add-listener
  "Adds a listener. Pass in a channel to
get messages"
  [the-chan]
  (swap! listeners conj the-chan)
  (async/put! the-chan  (into [] (take 50 @messages)))
  true)

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
                          (fn [c] (not (asyncp/closed? c))) x)))
      (mapv (fn [c] (async/put! c msg)) @listeners)
      true)
    false))

(def chat-service (circ/build-service))
