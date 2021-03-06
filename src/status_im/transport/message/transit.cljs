(ns ^{:doc "Transit custom readers and writers, required when adding a new record implementing StatusMessage protocol"}
    status-im.transport.message.transit
  (:require [status-im.transport.message.v1.contact :as v1.contact]
            [status-im.transport.message.v1.protocol :as v1.protocol]
            [status-im.transport.message.v1.group-chat :as v1.group-chat]
            [cognitect.transit :as transit]))

;; When adding a new reccord implenting the StatusMessage protocol it is required to implement:
;; - a handler that will turn the clojure record into a javascript datastructure.
;; - a reader that will turn the javascript datastructure back into a clojure record.

;; Use the existing types as exemples of how this is done

;;
;; Writer handlers
;;

;; Each writer defines a tag and a representation
;; The tag will determine which reader is used to recreate the clojure record
;; When migrating a particular record, it is important to use a different type and still handle the previous
;; gracefully for compatibility
(deftype NewContactKeyHandler []
  Object
  (tag [this v] "c1")
  (rep [this {:keys [sym-key topic message]}]
    #js [sym-key topic message]))

(deftype ContactRequestHandler []
  Object
  (tag [this v] "c2")
  (rep [this {:keys [name profile-image address fcm-token]}]
    #js [name profile-image address fcm-token]))

(deftype ContactRequestConfirmedHandler []
  Object
  (tag [this v] "c3")
  (rep [this {:keys [name profile-image address fcm-token]}]
    #js [name profile-image address fcm-token]))

(deftype ContactUpdateHandler []
  Object
  (tag [this v] "c6")
  (rep [this {:keys [name profile-image]}]
    #js [name profile-image]))

(deftype MessageHandler []
  Object
  (tag [this v] "c4")
  (rep [this {:keys [content content-type message-type clock-value timestamp]}]
    #js [content content-type message-type clock-value timestamp]))

(deftype MessagesSeenHandler []
  Object
  (tag [this v] "c5")
  (rep [this {:keys [message-ids]}]
    (clj->js message-ids)))

(deftype NewGroupKeyHandler []
  Object
  (tag [this v] "g1")
  (rep [this {:keys [chat-id sym-key message]}]
    #js [chat-id sym-key message]))

(deftype GroupAdminUpdateHandler []
  Object
  (tag [this v] "g2")
  (rep [this {:keys [chat-name participants]}]
    #js [chat-name participants]))

(deftype GroupLeaveHandler []
  Object
  (tag [this v] "g3")
  (rep [this _]
    (clj->js nil)))

(def writer (transit/writer :json
                            {:handlers
                             {v1.contact/NewContactKey           (NewContactKeyHandler.)
                              v1.contact/ContactRequest          (ContactRequestHandler.)
                              v1.contact/ContactRequestConfirmed (ContactRequestConfirmedHandler.)
                              v1.contact/ContactUpdate           (ContactUpdateHandler.)
                              v1.protocol/Message                (MessageHandler.)
                              v1.protocol/MessagesSeen           (MessagesSeenHandler.)
                              v1.group-chat/NewGroupKey          (NewGroupKeyHandler.)
                              v1.group-chat/GroupAdminUpdate     (GroupAdminUpdateHandler.)
                              v1.group-chat/GroupLeave           (GroupLeaveHandler.)}}))

;;
;; Reader handlers
;;

;; Here we only need to call the record with the arguments parsed from the clojure datastructures
(def reader (transit/reader :json
                            {:handlers
                             {"c1" (fn [[sym-key topic message]]
                                     (v1.contact/NewContactKey. sym-key topic message))
                              "c2" (fn [[name profile-image address fcm-token]]
                                     (v1.contact/ContactRequest. name profile-image address fcm-token))
                              "c3" (fn [[name profile-image address fcm-token]]
                                     (v1.contact/ContactRequestConfirmed. name profile-image address fcm-token))
                              "c4" (fn [[content content-type message-type clock-value timestamp]]
                                     (v1.protocol/Message. content content-type message-type clock-value timestamp))
                              "c5" (fn [message-ids]
                                     (v1.protocol/MessagesSeen. message-ids))
                              "c6" (fn [[name profile-image]]
                                     (v1.contact/ContactUpdate. name profile-image))
                              "g1" (fn [[chat-id sym-key message]]
                                     (v1.group-chat/NewGroupKey. chat-id sym-key message))
                              "g2" (fn [[chat-name participants]]
                                     (v1.group-chat/GroupAdminUpdate. chat-name participants))
                              "g3" (fn [_]
                                     (v1.group-chat/GroupLeave.))}}))


(defn serialize
  "Serializes a record implementing the StatusMessage protocol using the custom writers"
  [o]
  (transit/write writer o))

(defn deserialize
  "Deserializes a record implementing the StatusMessage protocol using the custom readers"
  [o]
  (try (transit/read reader o) (catch :default e nil)))
