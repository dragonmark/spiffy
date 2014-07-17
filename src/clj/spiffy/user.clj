(ns spiffy.user
  (:require [crypto.password.bcrypt :as crypto]
            [clojure.java.jdbc :as j]
            [spiffy.services :as ss]
            [clojure.core.async :as async]
            [schema.core :as ps]
  ))

(def db {:subprotocol "h2"
         :subname "./spiffy.db"})

(def User
  {:id ps/Num
   :first_name ps/Str
   :last_name ps/Str
   (ps/optional-key :passwd) ps/Str
   :email ps/Str})

(ps/defn all-users :- [User]
  "return all users"
  []
  (j/query db ["SELECT * FROM users"]))

(ps/defn ^{:service true} test-login :- (ps/maybe User)
  "Given an email address and a password, test the login.
Return the user if the login is valid"
  ([email :- ps/Str
   password :- ps/Str]
  (some->>
   (j/query db ["SELECT * FROM users WHERE email = ?"
                (.toLowerCase email)])
   (filter #(some->> % :passwd (crypto/check password)))
   first))
  )
   

(ps/defn find-user :- (ps/maybe User)
  "Find a user. If the parameter is
a number, the user with that primary key is returned,
otherwise the parameter is assumed to be the email
address"
  [id :- (ps/either ps/Num ps/Str User)]
  (cond
   (number? id) (first (j/query db ["SELECT * FROM users WHERE id = ?" id]))
   (string? id) (first (j/query db ["SELECT * FROM users WHERE email = ?"
                                    (.toLowerCase id)]))
   (map? id) id
   :else nil)
  )

(ps/defn save-user :- User
  "Saves the user. If an ID field is present, does an update,
otherwise does an insert. Returns the updated record"
  [user :- User]
  (let [user (dissoc user :passwd)]
    (if (:id user)
      (j/update! db :users user ["id = ?" (:id user)])
      (let [id (j/insert! db :users user)]
        (assoc user :id (-> id first vals first))))))

(ps/defn set-password
  "sets the password for the user"
  [user password]
  (let [user (find-user user)]
    (j/update! db :users {:passwd (crypto/encrypt password)}
               ["id = ?" (:id user)])))

(defn test-password
  "Tests the password for the user"
  [user password]
  (let [user (find-user user)]
    (crypto/check password (:passwd user))))

(def ^:dynamic dog nil)

(ps/defn ^:service moose [] 
  (println "moose" (Thread/currentThread))
  dog)

  (let [c (ss/register 'db/user)
        r (async/chan)]
    (binding [dog 44]
      (println "main" (Thread/currentThread))
      (async/>!! c (with-meta {:_cmd "moose" :email "d@athena.com" :password "foo" :_answer r}
                     {:bound-fn (let [bindings (get-thread-bindings)] (fn [f p] (with-bindings* bindings f p)))}
                     ))
      (println "Got " (async/<!! r))
      (async/close! r)))
