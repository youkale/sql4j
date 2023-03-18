(ns sql4j.core
  (:require
    [clojure.java.io :as io]
    [clojure.string :as str]
    [clojure.tools.logging :as log]
    [clojure.walk :as walk]
    [hugsql.core :as hug]
    [sql4j.comm :refer [prim-cast-fn]])
  (:import (clojure.lang ILookup)
           (com.github.youkale.sql4j MethodSignature)
           (com.github.youkale.sql4j.annotation Param)
           (java.io File)
           (java.lang.reflect Parameter)
           (java.util ArrayList List Map Set)))
(def ^:private config (atom {}))

(defn def-db-fns [sql-file options]
  (do (hug/def-db-fns sql-file options)
      (when (:debug options)
        (hug/def-sqlvec-fns sql-file options))))

(defn init-mappers
  ([group-name dir] (init-mappers group-name dir ".sql" {}))
  ([group-name dir suffix & {:as options}]
   (let [{:keys [debug] :or {debug false}
          :as   options} (-> (into {} options)
                             (walk/keywordize-keys))
         ns-symbol (-> (str group-name ".mappers")
                       (symbol)
                       (create-ns))
         _ (swap! config update group-name (constantly (assoc options :ns ns-symbol)))]
     (binding [*ns* ns-symbol]
       (doseq [f (filter #(and (.isFile ^File %)
                               (str/ends-with? (.getName ^File %) suffix))
                         (file-seq (io/as-file (io/resource dir))))]
         (def-db-fns f options))))))

(deftype ParamMetaValue [param obj]
  ILookup
  (valAt [this key]
    (.valAt this key nil))
  (valAt [_ key not-found]
    (if-let [p (.getAnnotation ^Parameter param Param)]
      (if (or (= (.value p) key) (= (keyword (.value p)) key)) obj not-found)
      (or (get obj key)
          (get obj (keyword key) not-found)))))

(defn- make-param-meta-value [param object]
  (ParamMetaValue.
    param
    (cond
      (instance? List object)
      (into [] object)
      (instance? Set object)
      (into #{} object)
      (instance? Map object)
      (walk/keywordize-keys (into {} object))
      (map? object)
      (walk/keywordize-keys (into {} object))
      :else
      (bean object))))

(defn- create-params-fn [params objs]
  (reify ILookup
    (valAt [this a]
      (.valAt this a nil))
    (valAt [_ a not-found]
      (loop [args (seq (into [] objs))
             pm (seq (into [] params))
             obj (Object.)
             res not-found]
        (if (and args pm)
          (let [pmv (make-param-meta-value (first pm) (first args))
                r (get pmv a obj)]
            (if (= obj r)
              (recur (next args) (next pm) obj res)
              (if (nil? r)
                (recur (next args) (next pm) obj res)
                (recur nil nil obj r))))
          res)))))
(def ^:private boolean-array-type (class (make-array Boolean 0)))
(def ^:private byte-array-type (class (make-array Byte 0)))
(def ^:private short-array-type (class (make-array Short 0)))
(def ^:private integer-array-type (class (make-array Integer 0)))
(def ^:private long-array-type (class (make-array Long 0)))
(def ^:private float-array-type (class (make-array Float 0)))
(def ^:private double-array-type (class (make-array Double 0)))
(def ^:private string-array-type (class (make-array String 0)))

(defmulti command-options (fn [& [x]]
                            (if
                              (some #(= x %)
                                    [Boolean Boolean/TYPE Byte Byte/TYPE Short Short/TYPE
                                     Integer Integer/TYPE Long Long/TYPE Float Float/TYPE Double Double/TYPE])
                              :prim
                              (condp = x
                                boolean-array-type (Class/forName "[Z")
                                byte-array-type (Class/forName "[B")
                                short-array-type (Class/forName "[S")
                                integer-array-type (Class/forName "[I")
                                long-array-type (Class/forName "[J")
                                float-array-type (Class/forName "[F")
                                double-array-type (Class/forName "[D")
                                x))))

(defmethod command-options :prim [& [x]]
  {:raw? true :row-fn (prim-cast-fn x)})

(defmethod command-options (Class/forName "[Z") [& _]
  {:raw?          true
   :result-set-fn (fn [x] (into-array (Class/forName "[Z") x))
   :row-fn        (prim-cast-fn Boolean/TYPE)})

(defmethod command-options (Class/forName "[B") [& _]
  {:raw? true :row-fn (prim-cast-fn Byte/TYPE) :result-set-fn (fn [x] (into-array Byte/TYPE x))})

(defmethod command-options (Class/forName "[S") [& _]
  {:raw? true :row-fn (prim-cast-fn Short/TYPE) :result-set-fn (fn [x] (into-array Short/TYPE x))})
(defmethod command-options (Class/forName "[I") [& _]
  {:raw? true :row-fn (prim-cast-fn Integer/TYPE) :result-set-fn (fn [x] (into-array Integer/TYPE x))})

(defmethod command-options (Class/forName "[J") [& _]
  {:raw? true :row-fn (prim-cast-fn Long/TYPE) :result-set-fn (fn [x] (into-array Long/TYPE x))})

(defmethod command-options (Class/forName "[F") [& _]
  {:raw? true :row-fn (prim-cast-fn Float/TYPE) :result-set-fn (fn [x] (into-array Float/TYPE x))})

(defmethod command-options (Class/forName "[D") [& _]
  {:raw? true :row-fn (prim-cast-fn Double/TYPE) :result-set-fn (fn [x] (into-array Double/TYPE x))})
(defmethod command-options String [& _]
  {:raw? true})
(defmethod command-options string-array-type [& _]
  {:raw? true :result-set-fn (fn [x] (into-array String x))})
(defmethod command-options List [& [_ [gen-type] mapping]]
  (println gen-type)
  (println mapping)
  {:keywordize? false :result-set-fn (fn [x] (ArrayList. x))
   :row-fn      (fn [r]
                  (reduce-kv
                    (fn [init k v]
                      (let [prop (symbol (str "set" (str/upper-case (first v)) (apply str (rest v))))
                            db-val (get r k)]
                        (. init prop db-val)
                        init))
                    (construct-proxy gen-type)
                    mapping))})
(defmethod command-options Map [& _]
  {:keywordize? false :result-set-fn first})

(defmethod command-options :default [& _]
  {:keywordize? false :result-set-fn first})

(defn- result-handle [^MethodSignature method-signature {:keys [result command]}]
  (let [res (hug/hugsql-result-fn result)
        cmd (hug/hugsql-command-fn command)]
    (when (and (= cmd 'hugsql.adapter/query)
               (some #(= res %) ['hugsql.adapter/result-one 'hugsql.adapter/result-many]))
      (let [rt-type (.getReturnType method-signature)
            rt-gen-type (.getGenericReturnTypes method-signature)
            mapping (into {} (.getResultMapping method-signature))]
        (cond
          (= res 'hugsql.adapter/result-one)
          {:command-options [(command-options rt-type rt-gen-type mapping)]}
          (= res 'hugsql.adapter/result-many)
          {:command-options [(command-options rt-type rt-gen-type mapping)]})))))

(defn create-executor [group-name class-with-method-or-hug-name ^MethodSignature method-signature]
  (let [{:keys [ns debug quoting]} (get @config group-name)
        ns-fns (ns-publics ns)
        executor (get ns-fns
                      (symbol class-with-method-or-hug-name))
        executor-debug (get ns-fns (symbol (str class-with-method-or-hug-name "-sqlvec")))
        executor (if (nil? executor)
                   (throw
                     (ex-info
                       (str "namespace '" (ns-name ns) "' not found")
                       {:group-name  group-name
                        :mapper-name class-with-method-or-hug-name}))
                   executor)
        result-fn (result-handle method-signature (meta executor))]
    (fn [conn args]
      (let [run-id (gensym "exec")
            param-fn (create-params-fn (.getParameters method-signature) args)]
        (when debug
          (log/debugf "%s %s => %s " run-id class-with-method-or-hug-name (executor-debug param-fn)))
        (let [res (executor {:connection conn} param-fn
                            (cond-> {:quoting quoting}
                                    (not (nil? result-fn)) (merge result-fn)))]
          (when debug
            (log/debugf "%s %s <= %s " run-id class-with-method-or-hug-name res))
          res)))))
