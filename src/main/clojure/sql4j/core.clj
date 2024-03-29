(ns sql4j.core
  (:require
    [camel-snake-kebab.core :as csk]
    [clojure.java.io :as io]
    [clojure.string :as str]
    [clojure.tools.logging :as log]
    [clojure.walk :as walk]
    [hugsql.core :as hug])
  (:import (clojure.lang ILookup)
           (com.github.youkale.sql4j MethodSignature)
           (com.github.youkale.sql4j.annotation Param)
           (java.beans Introspector PropertyDescriptor)
           (java.io File)
           (java.lang.reflect Method Parameter)
           (java.nio.file FileSystemNotFoundException FileSystems FileVisitOption Files)
           (java.time LocalDate LocalDateTime ZoneId)
           (java.util Date List Map Set)))
(def ^:private config (atom {}))

(defn def-db-fns [sql-file options]
  (do (hug/def-db-fns sql-file options)
      (when (:debug options)
        (hug/def-sqlvec-fns sql-file options))))


(defn- get-mapper-from-jar [dir uri suffix]
  (let [path (-> (try
                   (FileSystems/getFileSystem uri)
                   (catch FileSystemNotFoundException _
                     (FileSystems/newFileSystem uri {})))
                 (.getPath "/" (make-array String 0)))
        fs (iterator-seq (.iterator (Files/walk path (into-array [FileVisitOption/FOLLOW_LINKS]))))]
    (map (fn [x]
           (let [x (.toString x)]
             (if (str/starts-with? x File/separator)
               (subs x 1) x)))
         (filter (fn [x]
                   (let [fname (.getFileName x)]
                     (and fname (str/ends-with? fname suffix)
                          (.endsWith (.getParent x) dir)))) fs))))

(defn- get-mapper-files [dir suffix]
  (let [res-url (io/resource dir)]
    (log/infof "dir %s, suffix %s" dir suffix)
    (if res-url
      (let [uri (.toURI res-url)]
        (if (= "jar" (.getScheme uri))
          (get-mapper-from-jar dir uri suffix)
          (filter #(and (.isFile ^File %)
                        (str/ends-with? (.getName ^File %) suffix))
                  (file-seq (io/as-file (io/resource dir))))))
      (throw (ex-info "get mapper error" {:dir dir :suffix suffix})))))

(defn init-mappers
  [group-name dir suffix & {:as options}]
  (let [{:keys [debug] :or {debug false}
         :as   options} (-> (into {} options)
                            (walk/keywordize-keys))
        ns-symbol (-> (str group-name ".mappers")
                      (symbol)
                      (create-ns))
        _ (swap! config update group-name (constantly (assoc options :ns ns-symbol)))]
    (binding [*ns* ns-symbol]
      (doseq [f (get-mapper-files dir suffix)]
        (def-db-fns f options)))))

(defn- prep-args [arg]
  (cond
    (instance? List arg)
    (into [] (map prep-args arg))
    (instance? Set arg)
    (into #{} (map prep-args arg))
    (instance? Map arg)
    (walk/keywordize-keys (into {} arg))
    (map? arg)
    (walk/keywordize-keys (into {} arg))
    :else
    (if (.isPrimitive (class arg))
      arg
      (bean arg))))

(defn- unwrap-prim [x]
  (condp = x
    Character Character/TYPE
    Boolean Boolean/TYPE
    Byte Byte/TYPE
    Short Short/TYPE
    Integer Integer/TYPE
    Long Long/TYPE
    Float Float/TYPE
    Double Double/TYPE
    x))
(defn- is-prim? [x]
  (.isPrimitive (unwrap-prim (class x))))

(defn- make-args-mapping [params objs]
  (let [args (loop [args (seq (into [] objs))
                    pm (seq (into [] params))
                    obj (Object.)
                    res nil]
               (if (and args pm)
                 (let [a (first args)
                       ca (cond
                            (instance? List a)
                            (into [] a)
                            (instance? Set a)
                            (into #{} a)
                            (instance? Map a)
                            (walk/keywordize-keys (into {} a))
                            (map? a)
                            (walk/keywordize-keys (into {} a))
                            :else
                            (if (or (is-prim? a) (string? a)) a (bean a)))
                       item (if-let [p ^Param (.getAnnotation ^Parameter (first pm) Param)]
                              (assoc res (keyword (.value p)) ca)
                              (merge res ca))]
                   (recur (next args) (next pm) obj item))
                 res))]
    (reify ILookup
      (valAt [this a]
        (.valAt this a nil))
      (valAt [_ a not-found]
        (get args a not-found)))))

(defmulti type-cast-fn (fn [& [x]] (unwrap-prim x)))
(defmethod type-cast-fn Boolean/TYPE [& _]
  (fn [x] (cond
            (instance? Boolean/TYPE x)
            (boolean x)
            (instance? Number x)
            (> (.intValue x) 0))))
(defmethod type-cast-fn Byte/TYPE [_]
  (fn [x] (when x (byte x))))

(defmethod type-cast-fn Short/TYPE [_]
  (fn [x] (when x (short x))))

(defmethod type-cast-fn Integer/TYPE [_]
  (fn [x] (when x (int x))))
(defmethod type-cast-fn Long/TYPE [_]
  (fn [x] (when x (long x))))

(defmethod type-cast-fn Float/TYPE [_]
  (fn [x] (when x (float x))))

(defmethod type-cast-fn Double/TYPE [_]
  (fn [x] (when x (double x))))

(defmethod type-cast-fn Character/TYPE [_]
  (fn [x] (when x (char x))))

(defmethod type-cast-fn Date [_]
  (fn [x]
    (cond (instance? Date x) x
          (instance? LocalDate x)
          (-> (.atStartOfDay x)
              (.atZone (ZoneId/systemDefault))
              (.toInstant)
              (Date/from))
          (instance? LocalDateTime x)
          (-> (.atZone x (ZoneId/systemDefault))
              (.toInstant)
              (Date/from)))))

(defmethod type-cast-fn LocalDate [_]
  (fn [x]
    (cond (instance? Date x)
          (-> (.toInstant x)
              (.atZone (ZoneId/systemDefault))
              (.toLocalDate))
          (instance? LocalDate x) x
          (instance? LocalDateTime x) (.toLocalDate x))))

(defmethod type-cast-fn LocalDateTime [_]
  (fn [x]
    (cond (instance? Date x)
          (-> (.toInstant x)
              (.atZone (ZoneId/systemDefault))
              (.toLocalDateTime))
          (instance? LocalDate x) (.atStartOfDay x)
          (instance? LocalDateTime x) x)))

(defmethod type-cast-fn :default [_])

(def ^:private memoize-bean-write-methods
  (memoize
    (fn [^Class c]
      (loop [props (seq (-> (Introspector/getBeanInfo c)
                            (.getPropertyDescriptors)))
             res (transient {})]
        (if props
          (let [^PropertyDescriptor p (first props)
                n (.getName p)
                w (.getWriteMethod p)]
            (recur (next props)
                   (if (and n p)
                     (assoc! res n w) res)))
          (persistent! res))))))

(defn invoke-bean-write-method [obj property val]
  (let [props (memoize-bean-write-methods (class obj))]
    (when-let [^Method m (get props property)]
      (let [cast-fn (-> (.getParameterTypes m) first type-cast-fn)]
        (.invoke ^Method m obj (into-array [(if cast-fn (cast-fn val) val)]))))
    obj))

(def ^:private boolean-array-type (class (make-array Boolean 0)))
(def ^:private byte-array-type (class (make-array Byte 0)))
(def ^:private short-array-type (class (make-array Short 0)))
(def ^:private integer-array-type (class (make-array Integer 0)))
(def ^:private long-array-type (class (make-array Long 0)))
(def ^:private float-array-type (class (make-array Float 0)))
(def ^:private double-array-type (class (make-array Double 0)))
(def ^:private string-array-type (class (make-array String 0)))

(defmulti command-options (fn [& [x]]
                            (cond
                              (some #(= x %)
                                    [Boolean Boolean/TYPE Byte Byte/TYPE Short Short/TYPE
                                     Integer Integer/TYPE Long Long/TYPE Float Float/TYPE Double Double/TYPE]) :prim
                              (some #(= x %)
                                    [boolean-array-type (Class/forName "[Z")
                                     byte-array-type (Class/forName "[B")
                                     short-array-type (Class/forName "[S")
                                     integer-array-type (Class/forName "[I")
                                     long-array-type (Class/forName "[J")
                                     float-array-type (Class/forName "[F")
                                     double-array-type (Class/forName "[D")]) :prim-vec
                              :else x)))

(defmethod command-options :prim [& [origin]]
  {:raw? true :row-fn (fn [r]
                        ((type-cast-fn origin) (val (first r))))})
(defmethod command-options :prim-vec [& [origin]]
  (let [prim-type (.getComponentType origin)]
    {:raw?          true :row-fn (fn [r]
                                   ((type-cast-fn prim-type) (val (first r))))
     :result-set-fn (fn [x] (into-array prim-type x))}))

(defmethod command-options String [& _]
  {:raw? true})
(defmethod command-options string-array-type [& _]
  {:raw? true :result-set-fn (fn [x] (into-array String x))})
(defmethod command-options List [& [_ [gen-type] mapping]]
  (merge
    (command-options gen-type nil mapping)
    {:keywordize? false :result-set-fn (fn [x] (into [] x))}))
(defmethod command-options Map [& _]
  {:keywordize? false :result-set-fn first})

(defmethod command-options :default [& [^Class def-type _ mapping]]
  (if (.isArray def-type)
    (let [t (.getComponentType def-type)]
      (merge
        (command-options t nil mapping)
        {:keywordize?   false
         :result-set-fn (fn [rs]
                          (into-array t rs))}))
    {:keywordize? false
     :row-fn      (fn [r]
                    (reduce-kv
                      (fn [i k v]
                        (invoke-bean-write-method i (get mapping k (csk/->camelCase k)) v))
                      (construct-proxy def-type) r))}))

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
            param-fn (make-args-mapping (.getParameters method-signature) args)]
        (when debug
          (log/debugf "%s %s => %s " run-id class-with-method-or-hug-name (executor-debug param-fn)))
        (let [res (executor {:connection conn} param-fn
                            (cond-> {:quoting quoting}
                                    (not (nil? result-fn)) (merge result-fn)))]
          (when debug
            (log/debugf "%s %s <= %s " run-id class-with-method-or-hug-name res))
          res)))))
