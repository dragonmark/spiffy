(ns spiffy.services)

(defmacro register
  "registers a service"
  [name]
  (let [services (->> (ns-publics *ns*)
                      vals
                      (filter #(-> % meta :service)))]
    (println "Services " services)
    nil))
