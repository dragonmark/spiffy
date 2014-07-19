(ns spiffy.props
  (:import [java.net InetAddress]
           [java.io InputStream]))

;; /**
;;  * Configuration management utilities.
;;  *
;;  * If you want to provide a configuration file for a subset of your application
;;  * or for a specific environment, Lift expects configuration files to be named
;;  * in a manner relating to the context in which they are being used. The standard
;;  * name format is:
;;  *
;;  * <pre>
;;  *   modeName.userName.hostName.props
;;  *
;;  *   examples:
;;  *   dpp.yak.props
;;  *   test.dpp.yak.props
;;  *   production.moose.props
;;  *   staging.dpp.props
;;  *   test.default.props
;;  *   default.props
;;  * </pre>
;;  *
;;  * with hostName and userName being optional, and modeName being one of
;;  * "test", "staging", "production", "pilot", "profile", or "default".
;;  * The standard Lift properties file extension is "props".
;;  */

(def run-modes
  ;; /**
  ;;  * Enumeration of available run modes.
  ;;  */
  [:dev
   :test
   :staging
   :prod
   ])

;  val propFileName = "lift.props"

;  val fileName = "lift.props"

(def run-mode
  (atom
   (or
    (some-> (System/getProperty "run.mode") .toLowerCase keyword)
    (when (->> (Exception.) .getStackTrace (map #(.getClassName %)) (filter #(.startsWith % "clojure.test")) empty? not) :test)
    :dev)))

(defn production-mode? 
  "Is the system running in production mode"
  []
  (or
   (= @run-mode :prod)
   (= @run-mode :staging)))


(defn dev-mode? 
  "Is the system running in production mode"
  []
  (or
   (= @run-mode :dev)))


(defn test-mode? 
  "Is the system running in production mode"
  []
  (or
   (= @run-mode :test)))

(def user-name
  (or
   (System/getProperty "user.name")
   ""))

(def mode-name (name @run-mode))

(def host-name (try
                 (.getHostName (InetAddress/getLocalHost))
                 (catch Exception e "localhost")))

;; A list of propperties to try
(def to-try
  (map
   #(str % ".props")
   [(str "/props/" mode-name "." user-name "." host-name)
    (str "/props/" mode-name "." user-name)
    (str "/props/" mode-name "." host-name)
    (str "/props/" mode-name ".default" )
    "/props/default" 
    (str "/props/" user-name )
    (str "/" mode-name "." user-name "." host-name)
    (str "/" mode-name "." user-name)
    (str "/" mode-name "." host-name)
    (str "/" mode-name ".default" )
    (str "/" user-name )
    "/default"
   ]))

(defn find-files
  "Looks at the list to try and returns a list of input streams"
  []
  (->>
   to-try
   (map (fn [f] 
          (try
            (-> (.getClass dev-mode?) (.getResource f))
            (catch Exception e nil))))
   (remove nil?)))

;; spiffy.props> (-> (.getClass dev-mode?) (.getResource "/public/index.html") .openConnection .getLastModified)

;; spiffy.props> (-> (.getClass dev-mode?) (.getResource "/public/index.html") .openConnection .getContent slurp)


;;   /**
;;    * The map of key/value pairs retrieved from the property file.
;;    */
;;   lazy val props: Map[String, String] = {
;;     import java.io.{ByteArrayInputStream}
;;     import java.util.InvalidPropertiesFormatException
;;     import java.util.{Map => JMap}

;;     var tried: List[String] = Nil

;;     trace("Loading properties. Active run.mode is %s".format(if (modeName=="") "(Development)" else modeName))

;;     def vendStreams: List[(String, () => Box[InputStream])] = whereToLook() :::
;;     toTry.map{
;;       f => {
;;         val name = f() + "props"
;;         name -> {() =>
;;           val res = tryo{getClass.getResourceAsStream(name)}.filter(_ ne null)
;;           trace("Trying to open resource %s. Result=%s".format(name, res))
;;           res
;;         }
;;       }
;;     }

;;     // find the first property file that is available
;;     first(vendStreams){
;;       case (str, streamBox) =>
;;         tried ::= str
;;         for {
;;           stream <- streamBox()
;;         } yield {
;;           val ret = new Properties
;;           val ba = Helpers.readWholeStream(stream)
;;           try {
;;             ret.loadFromXML(new ByteArrayInputStream(ba))
;;             debug("Loaded XML properties from resource %s".format(str))
;;           } catch {
;;             case _: InvalidPropertiesFormatException =>
;;               ret.load(new ByteArrayInputStream(ba))
;;               debug("Loaded key/value properties from resource %s".format(str))
;;           }
;;           ret
;;         }
;;     } match {
;;       // if we've got a propety file, create name/value pairs and turn them into a Map
;;       case Full(prop) =>
;;         Map(prop.entrySet.toArray.flatMap{
;;           case s: JMap.Entry[_, _] => List((s.getKey.toString, s.getValue.toString))
;;           case _ => Nil
;;         } :_*)

;;       case _ =>
;;         error("Failed to find a properties file (but properties were accessed).  Searched: "+tried.reverse.mkString(", "))
;;         Map()
;;     }
;;   }
;; }

