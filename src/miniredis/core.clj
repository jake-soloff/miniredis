(ns miniredis.core
	(:require [clojure.string :as str]
			  [ring.adapter.jetty :as jetty]
			  [ring.util.response :refer [response]]
			  [compojure.core :refer [GET POST defroutes]]
			  [compojure.coercions :refer [as-int]]
              [compojure.route :as route]
              [compojure.handler :as handler]
			  [ring.middleware.json :as middleware]))

;; State of db initialized to empty map
(def state (atom {}))

(defn reset [] (swap! state (fn [s] {})))

;; Return the value identified by key k
(defn get_cmd [arg] 
	(let [k (first arg)]
		(if (contains? @state k)
			(str "Key " k " corresponds to value " (@state k))
			(str "Key " k " not found"))))

;; Instantiate or overwrite value identified by key k with value v
(defn set_cmd [arg] 
	(let [k (first  arg)
		  v (second arg)]
		(if (or (string? v) (coll? v) (map? v))
			(do (swap! state (fn [s] (assoc s k v)))
				(str "Setting key " k " to value " v))
			(str "We only deal in strings, list of strings, and maps"))))

;; Delete the value identified by key k
(defn del_cmd [arg] 
	(let [k (first arg)]
		(swap! state (fn [s] (dissoc s k)))
		(str "Deleting key " k " if present")))

;; Append a String value v to the end of the List identified by key k
(defn app_cmd [arg]
	(let [k (first  arg)
		  v (second arg)]
		  (if (coll? (@state k))
		  	  (do (swap! state (fn [s] (assoc s k (conj (s k) v))))
		  	  	  (str "Added " v " to list at key " k))
		  	  (str "Value not a list!"))))

;; does the work of switching state for the POP command
(defn help_pop [k]
	(let [v (last (@state k))]
		(do (swap! state (fn [s] (assoc s k (butlast (s k)))))
			(str "Popped " v " from list at key " k))))

;; Remove the last element in the List identified by key k, 
;; and return that element (Calls help_pop)
(defn pop_cmd [arg]
	(let [k (first arg)]
		(if (coll? (@state k))
			(if (empty? (@state k)) 
				(str "Cannot pop from empty list!") 
				(help_pop k))
			(str "Value not a list!"))))

;; Return the String identified by mapkey mk from within the 
;; Map identified by key k
(defn mpg_cmd [arg]
	(let [k  (first arg)
		  mk (second arg)]
		  (if (and (contains? @state k) (contains? (@state k) mk))
		  	  (str "Retrieved value is " ((@state k) mk))
		  	  (str "Must provide valid keys"))))

;; Add the mapping mk -> mv to the Map identified by key k
(defn mps_cmd [arg] 
	(let [k  (first arg)
		  mk (second arg)
		  mv (nth arg 2)]
		  (swap! state (fn [s] (assoc s k (assoc (s k) mk mv))))
		  (str "Setting mapkey " mk " at key " k " to value " mv)))

;; Delete the value identified by mk from the Map identified by key k
(defn mpd_cmd [arg] 
	(let [k  (first arg)
		  mk (second arg)]
		  (swap! state (fn [s] (assoc s k (dissoc (s k) mk))))
		  (str "Deleting key " mk " from map at key " k ", if present")))

;; Returns a List<String> containing all keys k such that query is a prefix of k
(defn srchcmd [arg] 
	(let [query (first arg)]
		(str "Search returned: "
			 (vec (filter (fn [s] str/starts-with? s query) (keys @state))))))

;; Each route is attempted until a match is made. The condition in the post request
;; matches the provided command to a function above.
(defroutes my_routes
	;(GET "/api/db" [] (response @state))
	(POST "/api/cmd" [cmd arg :as req]
		(cond
			(= cmd "GET")         (get_cmd arg)
			(= cmd "SET")         (set_cmd arg)
			(= cmd "DELETE")      (del_cmd arg)
			(= cmd "APPEND")      (app_cmd arg)
			(= cmd "POP")         (pop_cmd arg)
			(= cmd "MAPGET")      (mpg_cmd arg)
			(= cmd "MAPSET")      (mps_cmd arg)
			(= cmd "MAPDELETE")   (mpd_cmd arg)
			(= cmd "SEARCH-KEYS") (srchcmd arg)
			:else (str "Provided command not currently supported")))
	(route/not-found "Don't even think about going there!"))

;; Add middleware to wrap json params 
(def app (middleware/wrap-json-params my_routes))

;; EXAMPLE CALL: 
;; curl -H "Content-Type: application/json" -X POST 
;;      -d '{"cmd":"SET","arg":["mykey",["lv1",lv2"]]}'  
;;      localhost:1729/api/cmd
(defn -main [] (jetty/run-jetty app {:port 3000}))