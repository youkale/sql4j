(ns sql4j.comm)

(defmulti prim-cast-fn (fn [& [x]]
                         (condp = x
                           Boolean Boolean/TYPE
                           Byte Byte/TYPE
                           Short Short/TYPE
                           Integer Integer/TYPE
                           Long Long/TYPE
                           Float Float/TYPE
                           Double Double/TYPE
                           x)))
(defmethod prim-cast-fn Boolean/TYPE [& _]
  (fn [x] (cond
            (instance? Boolean/TYPE x)
            (boolean x)
            (instance? Number x)
            (> (.intValue x) 0))))
(defmethod prim-cast-fn Byte/TYPE [_]
  (fn [x] (byte x)))

(defmethod prim-cast-fn Short/TYPE [_]
  (fn [x] (short x)))

(defmethod prim-cast-fn Integer/TYPE [_]
  (fn [x] (int x)))
(defmethod prim-cast-fn Long/TYPE [_]
  (fn [x] (long x)))

(defmethod prim-cast-fn Float/TYPE [_]
  (fn [x] (float x)))

(defmethod prim-cast-fn Double/TYPE [_]
  (fn [x] (double x)))

(defn prim-cast [clazz x]
  ((prim-cast-fn clazz) x))