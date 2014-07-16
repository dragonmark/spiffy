(ns spiffy.user
  (:require [crypto.password.bcrypt :as crypto]
            [clojure.java.jdbc :as j]
            [spiffy.services :as ss]
  ))

(def db {:subprotocol "h2"
         :subname "./spiffy.db"})

(defn all-users
  "return all users"
  []
  (j/query db ["SELECT * FROM users"]))

(defn find-user
  "Find a user. If the parameter is
a number, the user with that primary key is returned,
otherwise the parameter is assumed to be the email
address"
  [id]
  (cond
   (number? id) (first (j/query db ["SELECT * FROM users WHERE id = ?" id]))
   (string? id) (first (j/query db ["SELECT * FROM users WHERE email = ?"
                                    (.toLowerCase id)]))
   (map? id) id
   :else nil)
  )

(defn ^{:service true} save-user
  "Saves the user. If an ID field is present, does an update,
otherwise does an insert. Returns the updated record"
  [user]
  (let [user (dissoc user :passwd)]
    (if (:id user)
      (j/update! db :users user ["id = ?" (:id user)])
      (let [id (j/insert! db :users user)]
        (assoc user :id (-> id first vals first))))))

(defn set-password
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

(ss/register 'user)
