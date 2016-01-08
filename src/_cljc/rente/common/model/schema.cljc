(ns rente.common.model.schema)

(def partitions
  #{:db.part/test})

(def ^{:doc "This schema is not in conformity with Datomic or DataScript formats.
             it is sugar which gets transformed into these more verbose formats
             via |db/block->schemas|."}
  schemas {:todo/text       [:string  :one]
           :todo/completed? [:boolean :one {:index? true}]
           :todo/id         [:long    :one {:index? true}]})