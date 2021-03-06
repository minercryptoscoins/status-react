(ns status-im.data-store.realm.transport
  (:require [status-im.data-store.realm.core :as realm])
  (:refer-clojure :exclude [exists?]))

(defn get-all
  []
  (realm/all-clj (realm/get-all @realm/account-realm :transport) :transport))

(defn exists?
  [chat-id]
  (realm/exists? @realm/account-realm :transport {:chat-id chat-id}))

(defn save
  [{:keys [chat-id] :as chat}]
  (realm/save @realm/account-realm :transport chat (exists? chat-id)))

(defn delete
  [chat-id]
  (when-let [chat (realm/single (realm/get-by-field @realm/account-realm :transport :chat-id chat-id))]
    (realm/delete @realm/account-realm chat)))
