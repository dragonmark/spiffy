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
  [(-> info :name name)
   `(fn [x])])

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
        ]
    (.println System/out (str "ns " my-ns " and sym " cmds ))
    `(let [c# (chan)
           funcs# {"_commands" 
                   (fn [x#] ~cmds)
                   }]
       (go
        (loop [it# (<! c#)]
          (if (nil? it#) nil
              (let [cmd# (:_cmd it#)
                    answer# (:_answer it#)
                    the-func# (funcs# cmd#)
                    ]
                ~(wrap-in-thread 
                  &env
                  `(let [res# (if the-func# 
                                ~(if cljs-macro
                                   `(try 
                                      {:result (the-func# it#)}
                                      (catch js/Object 
                                          excp# 
                                        {:exception excp#}))
                                   `(try 
                                      {:result (the-func# it#)}
                                      (catch Exception
                                          excp# 
                                        {:exception excp#})))
                                {:error (str "Command " cmd# " not found")})
                         ]
                     (when answer#
                       (go (>! answer# res#)))))
                (recur (<! c#))
              )
              )))
       c#
       )
  ))

(println "moo")
