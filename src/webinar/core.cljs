(ns webinar.core
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs.core.async :as async
             :refer [>! <! put! take! chan timeout alts!]]
            [goog.events :as events])
  (:import [goog.events EventType]))

(enable-console-print!)

;; =============================================================================
;; Utilities

(defn by-id [id]
  (.getElementById js/document id))

(defn events->chan
  ([el event-type] (events->chan el event-type nil))
  ([el event-type xform]
   (let [c (chan 1 xform)]
     (events/listen el event-type
       (fn [e] (put! c e)))
     c)))

(defn mouse-loc->vec [e]
  [(.-clientX e) (.-clientY e)])

(defn show! [id msg]
  (let [p (.createElement js/document "p")]
    (set! (.-innerHTML p) msg)
    (.appendChild (.getElementById js/document id) p)))

;; =============================================================================
;; Example 1

(defn ex1 []
  (let [clicks (events->chan (by-id "ex1-button") EventType.CLICK)
        show!  (partial show! "ex1-messages")]
    (go
      (show! "Waiting for a click ...")
      (<! clicks)
      (show! "Got a click!"))))

(ex1)

;; =============================================================================
;; Example 2

(defn ex2 []
  (let [clicks (events->chan (by-id "ex2-button") EventType.CLICK)
        show!  (partial show! "ex2-messages")]
    (go
      (show! "Waiting for a click ...")
      (<! clicks)
      (show! "Got a click!")
      (show! "Waiting for another click ...")
      (<! clicks)
      (show! "Done!"))))

(ex2)

