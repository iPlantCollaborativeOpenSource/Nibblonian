(ns nibblonian.validators
  (:use clj-jargon.jargon
        clojure-commons.error-codes
        [slingshot.slingshot :only [try+ throw+]]))

(defn user-exists
  [cm user]
  (when-not (user-exists? cm user)
    (throw+ {:error_code ERR_NOT_A_USER
             :user user})))

(defn all-users-exist
  [cm users]
  (when-not (every? #(user-exists? cm %) users)
    (throw+ {:error_code ERR_NOT_A_USER
             :users (filterv
                     #(not (user-exists? cm %1))
                     users)})))

(defn path-exists
  [cm path]
  (when-not (exists? cm path)
    (throw+ {:error_code ERR_DOES_NOT_EXIST
             :path path})))

(defn all-paths-exist
  [cm paths]
  (when-not (every? #(exists? cm %) paths)
    (throw+ {:error_code ERR_DOES_NOT_EXIST
             :paths (filterv #(not (exists? cm  %1)) paths)})))

(defn no-paths-exist
  [cm paths]
  (when (some #(exists? cm %) paths)
    (throw+ {:error_code ERR_EXISTS
             :paths (filterv #(exists? cm %) paths)})))

(defn path-readable
  [cm user path]
  (when-not (is-readable? cm user path)
    (throw+ {:error_code ERR_NOT_READABLE
             :path path
             :user user})))

(defn all-paths-readable
  [cm user paths]
  (when-not (every? #(is-readable? cm user %) paths)
    (throw+ {:error_code ERR_NOT_READABLE
             :path (filterv #(not (is-readable? cm user %)) paths)})))

(defn path-writeable
  [cm user path]
  (when-not (is-writeable? cm user path)
    (throw+ {:error_code ERR_NOT_WRITEABLE
             :path path})))

(defn all-paths-writeable
  [cm user paths]
  (when-not (paths-writeable? cm user paths)
    (throw+ {:paths (filterv #(not (is-writeable? cm user %)) paths)
             :error_code ERR_NOT_WRITEABLE})))

(defn path-not-exists
  [cm path]
  (when (exists? cm path)
    (throw+ {:path path
             :error_code ERR_EXISTS})))

(defn path-is-dir
  [cm path]
  (when-not (is-dir? cm path)
    (throw+ {:error_code ERR_NOT_A_FOLDER
             :path path})))

(defn path-is-file
  [cm path]
  (when-not (is-file? cm path)
    (throw+ {:error_code ERR_NOT_A_FILE
             :path path})))

(defn path-satisfies-predicate
  [cm path pred-func? pred-err]
  (when-not (pred-func? cm  path)
    (throw+ {:paths path
             :error_code pred-err})))

(defn paths-satisfy-predicate
  [cm paths pred-func? pred-err]
  (when-not  (every? true? (mapv #(pred-func? cm %) paths))
    (throw+ {:error_code pred-err
             :paths (filterv #(not (pred-func? cm %)) paths)})))

(defn ownage?
  [cm user path]
  (owns? cm user path))

(defn user-owns-path
  [cm user path]
  (when-not (owns? cm user path)
    (throw+ {:error_code ERR_NOT_OWNER
             :user user
             :path path})))

(defn user-owns-paths
  [cm user paths]
  (let [belongs-to? (partial ownage? cm user)]
    (when-not (every? #(belongs-to? %) paths)
      (throw+ {:error_code ERR_NOT_OWNER
               :user user
               :paths (filterv #(not (belongs-to? %)) paths)}))))

(defn ticket-exists
  [cm user ticket-id]
  (when-not (ticket? cm user ticket-id)
    (throw+ {:error_code ERR_TICKET_DOES_NOT_EXIST
             :user user
             :ticket-id ticket-id})))

(defn ticket-does-not-exist
  [cm user ticket-id]
  (when (ticket? cm user ticket-id)
    (throw+ {:error_code ERR_TICKET_EXISTS
             :user user
             :ticket-id ticket-id})))

(defn all-tickets-exist
  [cm user ticket-ids]
  (when-not (every? #(ticket? cm user %) ticket-ids)
    (throw+ {:ticket-ids (filterv #(not (ticket? cm user %)) ticket-ids)
             :error_code ERR_TICKET_DOES_NOT_EXIST})))

(defn all-tickets-nonexistant
  [cm user ticket-ids]
  (when (some #(ticket? cm user %) ticket-ids)
    (throw+ {:ticket-ids (filterv #(ticket? cm user %) ticket-ids)
             :error_code ERR_TICKET_EXISTS})))



