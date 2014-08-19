(ns webinar.core
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs.core.async :as async
             :refer [>! <! put! take! chan timeout alts!]]
            [goog.events :as events])
  (:import [goog.events EventType]))

(enable-console-print!)

;; =============================================================================
;; Utilities

(defn events->chan
  ([el event-type] (events->chan el event-type nil))
  ([el event-type xform]
   (let [c (chan 1 xform)]
     (events/listen el event-type
       (fn [e] (put! c e)))
     c)))

(defn mouse-loc->vec [e]
  [(.-clientX e) (.-clientY e)])

(defn show! [id s]
  (set! (.-innerHTML (.getElementById js/document id)) s))

;; =============================================================================
;; Example 1

(defn ex1 []
  (let [c (events->chan js/document EventType.MOUSEMOVE
            (comp (map mouse-loc->vec)))]
    (go (loop []
          (let [v (<! c)]
            (show! "ex1-display" (pr-str v))
            (recur))))))

(ex1)