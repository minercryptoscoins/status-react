(ns status-im.data-store.core
  (:require [cljs.core.async :as async]
            [re-frame.core :as re-frame]
            [status-im.data-store.realm.core :as core]
            status-im.data-store.chats
            status-im.data-store.messages
            status-im.data-store.contacts
            status-im.data-store.transport
            status-im.data-store.browser
            status-im.data-store.accounts
            status-im.data-store.local-storage
            status-im.data-store.contact-groups
            status-im.data-store.requests))

(defn init []
  (core/reset-account))

(defn change-account [address new-account? handler]
  (core/change-account address new-account? handler))

(re-frame/reg-fx
  :data-store/base-tx
  (fn [transaction]
    (async/go (async/>! core/realm-queue (fn [] 
                                           (core/write #(doseq [transaction transactions]
                                                          (transaction core/base-realm))))))))

(re-frame/reg-fx
  :data-store/tx
  (fn [transaction]
    (async/go (async/>! core/realm-queue (fn []
                                           (let [realm @core/account-realm]
                                             (core/write #(doseq [transaction transactions]
                                                            (transaction realm)))))))))
