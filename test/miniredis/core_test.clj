(ns miniredis.core-test
  (:require [clojure.string :as str]
  			[clojure.test :refer :all]
            [miniredis.core :refer :all]))

(deftest basic-set-get-del
	(testing "GET standalone"
		(let [k "myKey"]
			(is (= (get_cmd [k]) 
				   (str "Key " k " not found")))))
	(testing "SET then GET; overwrite then GET; DELETE"
		(let [k  "myKey"
			  v  "myVal"
			  v2 "otherVal"]
			(is (= (set_cmd [k v]) 
				   (str "Setting key " k " to value " v)))
			(is (= (@state k) v))
			(is (= (get_cmd [k]) 
				   (str "Key " k " corresponds to value " v)))
			(is (= (set_cmd [k v2]) 
				   (str "Setting key " k " to value " v2)))
			(is (= (@state k) v2))
			(is (= (del_cmd [k]) 
				   (str "Deleting key " k " if present")))
			(is (= @state {})))))

(deftest list-val-functionality
	(testing "SET then overwrite then APPEND then POP"
		(let [k  "myKey"
			  l  ["a" "b"]
			  v  "c"
			  v2 "d"]
			(set_cmd [k v])
			(is (= (app_cmd [k v2]) (str "Value not a list!")))
			(set_cmd [k l])
			(is (= (app_cmd [k v]) (str "Added " v " to list at key " k)))
			(is (= (@state k) (conj l v)))
			(is (= (pop_cmd [k]) (str "Popped " v " from list at key " k)))
			(is (= (@state k) l))
			(reset)
			(is (= @state {})))))

(deftest map-val-functionality
	(testing "SET then MAPSET then MAPGET then MAPDELETE then MAPGET"
		(let [k "myKey"
			  v {"a" "b" "c" "d"}
			  mk "e"
			  mv "f"]
			(set_cmd [k v]) 
			(is (= (mpg_cmd [k "a"]) (str "Retrieved value is b")))
			(mps_cmd [k mk mv])
			(is (= (mpg_cmd [k mk]) (str "Retrieved value is " mv)))
			(mpd_cmd [k "c"])
			(is (= (mpg_cmd [k "c"]) (str "Must provide valid keys")))
			(reset))))



(comment
(deftest search-functionality
	(testing "SET then SEARCH-KEYS"
		(let [k  "myKey"
			  k2 "myOtherKey"
			  v  "myVal"
			  q1 "my"
			  q2 "myO"]
			  (set_cmd [k v])
			  (set_cmd [k2 v])
			  (is (= (srchcmd [q1]) (str "Search returned: " [k k2])))
			  (is (= (srchcmd [q2]) (str "Search returned: " [k2]))))))
)



;; consider use-fixtures to call reset function at the end of every test