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

;; =============================================================================
;; Example 3

(defn ex3 []
  (let [clicks-a (events->chan (by-id "ex3-button-a") EventType.CLICK)
        clicks-b (events->chan (by-id "ex3-button-b") EventType.CLICK)
        show!    (partial show! "ex3-messages")]
    (go
      (show! "Waiting for a click from Button A ...")
      (<! clicks-a)
      (show! "Got a click!")
      (show! "Waiting for a click from Button B ...")
      (<! clicks-b)
      (show! "Done!"))))

(ex3)

;; =============================================================================
;; Example 4

(defn ex4 []
  (let [clicks (events->chan (by-id "ex4-button-a") EventType.CLICK)
        c0     (chan)
        show!  (partial show! "ex4-messages")]
    (go
      (show! "Waiting for click.")
      (<! clicks)
      (show! "Putting a value on channel c0, cannot proceed until someone takes")
      (>! c0 (js/Date.))
      (show! "We'll never get this far!")
      (<! c0))))

(ex4)

;; =============================================================================
;; Example 5

(defn ex5 []
  (let [clicks (events->chan (by-id "ex5-button-a") EventType.CLICK)
        c0     (chan)
        show!  (partial show! "ex5-messages")]
    (go
      (show! "Waiting for click.")
      (<! clicks)
      (show! "Putting a value on channel c0, cannot proceed until someone takes")
      (>! c0 (js/Date.))
      (show! "Someone took the value from c0!"))
    (go
      (let [v (<! c0)]
        (show! (str "We got a value from c0: " v))))))

(ex5)



