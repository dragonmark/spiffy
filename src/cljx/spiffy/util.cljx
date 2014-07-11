(ns spiffy.util
  #+cljs (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
   clojure.string
   #+clj [clojure.pprint :as pp]
   #+clj [clojure.core.async :as async :refer [go chan timeout]]
   #+cljs [cljs.core.async :as async :refer [chan timeout]]
   )
  
  #+clj (:import
         [clojure.core.async.impl.channels ManyToManyChannel]
         [java.io Writer])
  )

(defn pretty
  "Pretty-formats the Clojure data structure"
  [x]
  (cond
   (string? x) x
   
   :else 
   #+clj (with-out-str (pp/pprint x))
   #+cljs (js/JSON.stringify (clj->js x))
   ))

(defn log
  "A platform-neutral logging facility."
  [ & rest]
  #+cljs (apply js/console.log rest)
  #+clj (let [string (clojure.string/join "" (map pretty rest))]
          (println string)))

#+clj (def ^:private secure-random (java.security.SecureRandom.))

(def ^:private counter (atom 100000000000))

(defn- random-chars
  "generate random characters"
  []
  #+cljs (str (.getRandomString goog.string) (.getRandomString goog.string))
  #+clj (locking secure-random
          (let [a (.nextLong secure-random)
                b (.nextLong secure-random)]
            (str (Long/toString (Math/abs a) 36)
                 (Long/toString (Math/abs b) 36)
            )))
  )

(defn next-guid
  "Generate a monotonically increasing GUID with a ton of randomness"
  []
  (str "S" (swap! counter inc) (random-chars)))

(defn next-clock
  "Return a monotonically increasing number"
  []
  (swap! counter inc))

(defn- find-guid-for-chan
  "Given a GUID, find a channel and create one if it's not found"
  [guid]
  (throw (#+clj Exception.
                #+cljs js/Exception. 
                "FIXME")))

#+clj (defmethod print-method ManyToManyChannel [chan, ^Writer w]
   (let [guid (find-guid-for-chan chan)]
  (.write w "#guid-chan\"")
  (.write w guid)
  (.write w "\"")))

;; the proxy to the channel that is handling the
;; current request... for the purposes of sending close messages
(def ^:dynamic current-proxy nil)

(def ^:private chan-to-guid (atom {}))

(def ^:private guid-to-chan (atom {}))



(defn register-chan
  "Registers a channel and a GUID"
  [chan guid]
  (swap! chan-to-guid assoc chan guid)
  (swap! guid-to-chan assoc guid chan)
  #+clj (add-watch (.closed chan) :key (fn [key atom old new]))
  #+cljs (let [current current-proxy]
           (.watch chan "closed"
                   (fn [id old new]))))
  
