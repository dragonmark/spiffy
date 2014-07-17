(ns spiffy.services
  (:require [schema.core :as sc]
            [schema.macros :as sm])
  )

(sc/defn ^:private expand-symbol :- [(sc/one sc/Symbol "symbol") 
                                     (sc/one sc/Keyword "keyword") 
                                     (sc/one sc/Str "string")]
  "Expand a symbol into a Vector of symbol, keyword, string"
  [in :- sc/Symbol]
  [in (keyword in) (name in)])

(defn- build-func
  "Takes a function defintion and builds a name/function pair
where the function applies the function with the named parameters"
  [info]
  (let [xn `x#
        params (->> info :arglists (sort #(> (count %1) (count %2))))
        params (map #(map keyword %) params)
        the-cond `(cond 
                   ~@(mapcat (fn [arity]
                               [`(and 
                                  ~@(map (fn [p] `(contains? ~xn ~p)) arity)
                                  )
                                `(apply ~(:name info) [~@(map (fn [p] `(~p ~xn)) arity)])]) params)
                   :else (with-meta {:error (str "parameters not matched. expecting " ~(str (into [] params)) " but got " (keys ~xn))} {:error true}))
        ]
    
  [(-> info :name name)
   `(fn [~xn]
      ~the-cond
      )]))

(defn- wrap-in-thread
  "Wraps the call in a thread if it's on the JVM"
  [env s-exp]
  (if (:ns env) s-exp
      `(future ~s-exp)))

(defmacro register
  "registers a service"
  [the-name]
  (let [cljs-macro (boolean (:ns &env))

        my-ns (if cljs-macro cljs.analyzer/*cljs-ns* *ns*)

        [chan go <! >!] (if cljs-macro '[cljs.core.async/chan
                                         cljs.core.async.macros/go
                                         cljs.core.async/<!
                                         cljs.core.async/>!]

                            '[clojure.core.async/chan
                              clojure.core.async/go
                              clojure.core.async/<!
                              clojure.core.async/>!])

        info 
        (if cljs-macro
         (some->> (cljs.analyzer/get-namespace my-ns) :defs vals 
                  (filter :service)
                  (into []))
         (->> (ns-publics my-ns)
              vals
              (filter #(-> % meta :service))
              (map meta)
              (into [])))
        the-funcs (map build-func info)


        cmds (into {} (map (fn [x] [(-> x :name name) (:doc x)]) info))

        built-funcs (merge {"_commands" 
                            `(fn [x#] ~cmds)
                            }
                           (into {} the-funcs))

        answer `answer#

        the-func `the-func#

        it `it#

        cmd `cmd#

        wrapper `wrapper#
        ]
    (.println System/out (str "ns " my-ns " and sym " cmds ))
    `(let [c# (~chan)
           funcs# ~built-funcs]
       (~go
        (loop [~it (~<! c#)]
          (if (nil? ~it) nil
              (let [~cmd (:_cmd ~it)
                    ~answer (:_answer ~it)
                    ~the-func (funcs# ~cmd)
                    ~wrapper (or (-> ~it meta :bound-fn)
                                 (fn [f# p#] (f# p#))) 
                    ]
                ~(wrap-in-thread 
                  &env
                  `(let [res# (if ~the-func 
                                ~(if cljs-macro
                                   `(try 
                                      (let [result# (~wrapper ~the-func ~it)]
                                        (if (-> result# meta :error) result# {:result result#}))
                                      (catch js/Object 
                                          excp# 
                                        {:exception excp#}))
                                   `(try 
                                      (let [result# (~wrapper ~the-func ~it)]
                                        (if (-> result# meta :error) result# {:result result#}))
                                      (catch Exception
                                          excp# 
                                        {:exception excp#})))
                                {:error (str "Command " ~cmd " not found")})
                         ]
                     (when ~answer
                       (~go (~>! ~answer res#)))))
                (recur (~<! c#))
              )
              )))
       c#
       )
  ))

(println "moo")
