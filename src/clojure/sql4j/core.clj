(ns sql4j.core
  (:require
    [clojure.java.io :as io]
    [clojure.string :as str]
    [clojure.tools.logging :as log]
    [clojure.walk :as walk]
    [hugsql.core :as hug])
  (:import (clojure.lang ILookup)
           (com.github.youkale.sql4j MethodSignature)
           (com.github.youkale.sql4j.annotation Param)
           (java.io File)
           (java.lang.reflect Parameter)
           (java.util List Map Set)))
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

(defn create-executor [group-name class-with-method-or-hug-name ^MethodSignature method-signature]
  (let [{:keys [ns debug quoting]} (get @config group-name)
        ns-fns (ns-publics ns)
        executor (get ns-fns
                      (symbol class-with-method-or-hug-name))
        executor-debug (get ns-fns (symbol (str class-with-method-or-hug-name "-sqlvec")))
        {:keys [result]} (meta executor)
        res (hug/hugsql-result-fn result)
        executor (if (nil? executor)
                   (throw
                     (ex-info
                       (str "namespace '" (ns-name ns) "' not found")
                       {:group-name  group-name
                        :mapper-name class-with-method-or-hug-name}))
                   executor)]
    (fn [conn args]
      (let [run-id (gensym "exec")
            param-fn (create-params-fn (.getParameters method-signature) args)]
        (when debug
          (log/debugf "%s %s => %s " run-id class-with-method-or-hug-name (executor-debug param-fn)))
        (let [res (executor {:connection conn} param-fn {:quoting quoting})]
          (when debug
            (log/debugf "%s %s <= %s " run-id class-with-method-or-hug-name res))
          res)))))
