(ns nibblonian.prov
  (:use [slingshot.slingshot :only [try+]])
  (:require [clojure-commons.provenance :as p]
            [clojure-commons.file-utils :as f]
            [nibblonian.config :as cfg]
            [clj-jargon.jargon :as jg]
            [clojure.tools.logging :as log])
  (:import [org.irods.jargon.datautils.shoppingcart FileShoppingCart]
           [org.irods.jargon.core.pub.domain IRODSDomainObject]))

;;;Event Names
(def root "root")
(def home "home")
(def file-exists "file-exists")
(def dir-exists "directory-exists")
(def stat-file "stat-file")
(def stat-dir "stat-directory")
(def download "download")
(def download-cart "download-cart")
(def upload-cart "upload-cart")
(def create-dir "create-directory")
(def list-dir "list-directory")
(def rename-dir "rename-directory")
(def rename-file "rename-file")
(def delete-dir "delete-directory")
(def delete-file "delete-file")
(def move-dir "move-directory")
(def move-file "move-file")
(def preview-file "preview-file")
(def file-manifest "file-manifest")
(def get-file-metadata "get-file-metadata")
(def set-file-metadata "set-file-metadata")
(def del-file-metadata "delete-file-metadata")
(def get-tree-urls "get-tree-urls")
(def set-tree-urls "set-tree-urls")
(def set-file-metadata-batch "set-file-metadata-batch")
(def get-dir-metadata "get-directory-metadata")
(def set-dir-metadata "set-directory-metadata")
(def del-dir-metadata "del-dir-metadata")
(def set-dir-metadata-batch "set-directory-metadata-batch")
(def share-file "share-file")
(def share-dir "share-directory")
(def unshare-file "unshare-file")
(def unshare-dir "unshare-directory")
(def quota "quota")
(def get-user-file-perms "get-user-file-permissions")
(def get-user-dir-perms "get-user-directory-permissions")
(def restore-file "restore-file")
(def restore-dir "restore-dir")
(def copy-file "copy-file")
(def copy-dir "copy-directory")

;;;Category Names

(def irods-file "irods-file")
(def irods-dir "irods-directory")
(def irods-avu "irods-avu")
(def irods-cart "irods-cart")
(def irods-listing "irods-listing")

;;;Utility functions

(defn avu?
  "Predicate that returns true if obj represents an iRODS AVU. This means
   that it is a map that has the following keys: :attr :value :unit :path."
  [cm obj]
  (and (map? obj)
       (contains? obj :attr)
       (contains? obj :value)
       (contains? obj :unit)
       (contains? obj :path)))

(defn listing?
  "Predicate that returns true if obj represents a directory listing. That
   means that is has the following keys at the top level: :id :label
   :permissions :date-created :date-modified."
  [cm obj]
  (and (map? obj)
        (contains? obj :id)
        (contains? obj :label)
        (contains? obj :permissions)
        (contains? obj :date-created)
        (contains? obj :date-modified)))

(defn cart?
  "Predicate that determines if the obj is a shopping cart."
  [cm obj]
  (and (map? obj)
       (contains? obj :action)
       (or (= (:action obj) "download")
           (= (:action obj) "upload"))))

(defn path?
  [cm obj]
  (and (string? obj)
       (or (jg/is-dir? cm obj)
           (jg/is-file? cm obj))))

(defn determine-category
  "Figures out the provenance category that is appropriate for the object
   passed in. 'cm' is a clj-jargon context map."
  [cm obj]
  (cond
   (and (path? cm obj)
        (jg/is-dir? cm obj))  irods-dir
        
   (and (path? cm obj)
        (jg/is-file? cm obj)) irods-file
        
   (and (map? obj)
        (avu? cm obj))        irods-avu

   (and (map? obj)
        (listing? cm obj))    irods-listing
        
   (and (map? obj)
        (cart? cm obj))       irods-cart
        
   :else                      nil))

(defn irods-domain-obj
  "Returns a domain object for obj.

   If obj is a string and directory in iRODS, then a Collection instance
   is returned.
"
  [cm obj]
  (cond
   (and (path? cm obj)
        (jg/is-dir? cm obj))
   (jg/collection cm obj)

   (and (path? cm obj)
        (jg/is-file? cm obj))
   (jg/data-object cm obj)

   (cart? cm obj)
   obj

   (avu? cm obj)
   obj

   (listing? cm obj)
   obj
   
   :else obj))

(defn object-id
  [cm user obj]
  (let [domain-obj (irods-domain-obj cm obj)]
    (cond
     (path? cm obj)
     (str (.. domain-obj getCreatedAt getTime)
          "||"
          (cfg/irods-zone)
          "||"
          (.. domain-obj getAbsolutePath))
     
     (cart? cm domain-obj)
     (str (.getTime (java.util.Date.))
          "||"
          user
          "||"
          (cfg/irods-zone))

     (avu? cm obj)
     (str (.getTime (java.util.Date.))
          "||"
          (:path obj)
          "||"
          (:attr obj) "-" (:value obj) "-" (:unit obj))

     (listing? cm obj)
     (str (.getTime (java.util.Date.))
          "||"
          (:id obj))
     
     :else
     "This is a string")))

(defn object-name
  [cm user obj]
  (let [domain-obj (irods-domain-obj cm obj)]
    (cond
     (path? cm obj)
     (f/basename (.getAbsolutePath domain-obj))

     (cart? cm domain-obj)
     (str "shopping-cart-" user)

     (avu? cm domain-obj)
     (str (:attr domain-obj)
          "-"
          (:value domain-obj)
          "-"
          (:unit domain-obj))

     (listing? cm domain-obj)
     (str (:id domain-obj))

     :else
     "This is another string.")))

(defn arg-map
  [cm user obj-id event category &
   {:keys [proxy-user data]
    :or {proxy-user (cfg/irods-user)
         data       nil}}]
  (let [svc    (cfg/service-name)
        purl   (cfg/prov-url)]
    (p/prov-map purl obj-id user svc event category proxy-user data)))

(defn lookup
  [cm user obj]
  (when (cfg/prov-enabled?)
    (try 
      (let [purl (cfg/prov-url)
            oid  (object-id cm user obj)]
        (if (p/exists? purl oid)
          (p/lookup purl oid)))
      (catch Exception e
        (log/warn e))
      (catch java.net.ConnectException ce 
        (log/warn ce))
      (catch Throwable t
        (log/warn t)))))

(defn register
  [cm user obj & [parent-uuid desc]]
  (when (cfg/prov-enabled?)
    (try 
      (let [obj-id (object-id cm user obj)
            obj-nm (object-name cm user obj)]
        (if-not (p/exists? (cfg/prov-url) obj-id)
          (p/register (cfg/prov-url) obj-id obj-nm desc parent-uuid))
        obj-id)
      (catch Exception e
        (log/warn e))
      (catch java.net.ConnectException ce 
        (log/warn ce))
      (catch Throwable t
        (log/warn t)))))

(defn register-parent
  [cm user obj & [parent-uuid desc]]
  (when (cfg/prov-enabled?)
    (try 
      (let [obj-id (object-id cm user obj)
            obj-nm (object-name cm user obj)]
        (if-not (p/exists? (cfg/prov-url) obj-id)
          (p/register (cfg/prov-url) obj-id obj-nm desc parent-uuid))
        (p/lookup (cfg/prov-url) obj-id))
      (catch Exception e
        (log/warn e))
      (catch java.net.ConnectException ce 
        (log/warn ce))
      (catch Throwable t
        (log/warn t)))))

(defn send-provenance
  [cm user obj-id event category & {:keys [data]}]
  (when (cfg/prov-enabled?)
    (try
      (log/warn
       (str "Log Provenance: "
            (p/log (arg-map cm user obj-id event category :data data))))
      (catch Exception e
        (log/warn e))
      (catch java.net.ConnectException ce 
        (log/warn ce))
      (catch Throwable t
        (log/warn t)))))

(defn log-provenance
  [cm user obj event & {:keys [parent-uuid data]}]
  (when (cfg/prov-enabled?)
    (let [obj-id  (register cm user obj parent-uuid)
          obj-cat (determine-category cm obj)]
      (log/warn (str "Object: " obj "\tID: " obj-id  "\tCategory: " obj-cat))
      (send-provenance cm user obj-id event obj-cat :data data)
      obj)))