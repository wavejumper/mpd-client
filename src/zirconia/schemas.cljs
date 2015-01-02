(ns zirconia.schemas
  (:require [schema.core :as s]))

(def Command
  s/Str)

(def Event
  {:command s/Keyword
   :args [s/Str]
   (s/optional-key :response) s/Any})
