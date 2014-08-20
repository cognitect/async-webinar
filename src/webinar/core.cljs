(ns webinar.core
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs.core.async :as async
             :refer [>! <! put! take! chan timeout alts!]]
            [goog.events :as events]
            [goog.dom.classes :as classes])
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
  (let [clicks (events->chan (by-id "ex5-button") EventType.CLICK)
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

;; =============================================================================
;; Example 6

(defn ex6 []
  (let [button (by-id "ex6-button")
        clicks (events->chan button EventType.CLICK)
        mouse  (events->chan js/window EventType.MOUSEMOVE
                 (map mouse-loc->vec))
        show!  (partial show! "ex6-messages")]
    (go
      (show! "Click button to start tracking the mouse!")
      (<! clicks)
      (set! (.-innerHTML button) "Stop!")
      (loop []
        (let [[v c] (alts! [mouse clicks])]
          (cond
            (= c clicks) (show! "Done!")
            :else
            (do
              (show! (pr-str v))
              (recur))))))))

(ex6)

;; =============================================================================
;; Example 7

(defn ex7 []
  (let [button (by-id "ex7-button")
        clicks (events->chan button EventType.CLICK)
        mouse  (events->chan js/window EventType.MOUSEMOVE
                 (comp (map mouse-loc->vec)
                       (filter (fn [[_ y]] (zero? (mod y 5))))))
        show!  (partial show! "ex7-messages")]
    (go
      (show! "Click button to start tracking the mouse!")
      (<! clicks)
      (set! (.-innerHTML button) "Stop!")
      (loop []
        (let [[v c] (alts! [mouse clicks])]
          (cond
            (= c clicks) (show! "Done!")
            :else
            (do
              (show! (pr-str v))
              (recur))))))))

(ex7)

;; =============================================================================
;; Example 8

(defn ex8 []
  (let [clicks (events->chan (by-id "ex8-button") EventType.CLICK)
        show!  (partial show! "ex8-messages")]
    (go
      (show! "Click the button ten times!")
      (<! clicks)
      (loop [i 1]
        (show! (str i " clicks!"))
        (if (> i 9)
          (show! "Done!")
          (do
            (<! clicks)
            (recur (inc i))))))))

(ex8)

;; =============================================================================
;; Example 9

(defn show-card! [id card]
  (set! (.-innerHTML (by-id id)) card))

(defn ex9 []
  (let [prev-button (by-id "ex9-button-prev")
        next-button (by-id "ex9-button-next")
        prev        (events->chan prev-button EventType.CLICK)
        next        (events->chan next-button EventType.CLICK)
        animals     [:aardvark :beetle :cat :dog :elk :ferret
                     :goose :hippo :ibis :jellyfish :kangaroo]
        max-idx     (dec (count animals))
        show-card!  (partial show-card! "ex9-card")]
    (go
      (loop [idx 0]
        (if (zero? idx)
          (classes/add prev-button "disabled")
          (classes/remove prev-button "disabled"))
        (if (== idx max-idx)
          (classes/add next-button "disabled")
          (classes/remove next-button "disabled"))
        (show-card! (nth animals idx))
        (let [[v c] (alts! [prev next])]
          (condp = c
            prev (if (pos? idx)
                   (recur (dec idx))
                   (recur idx))
            next (if (< idx max-idx)
                   (recur (inc idx))
                   (recur idx))))))))

(ex9)

;; =============================================================================
;; Example 9

(defn show-card! [id card]
  (set! (.-innerHTML (by-id id)) card))

(defn init-buttons [i max prev next]
  (if (zero? i)
    (classes/add prev "disabled")
    (classes/remove prev "disabled"))
  (if (== i max)
    (classes/add next "disabled")
    (classes/remove next "disabled")))

(defn ex10 []
  (let [start-stop-button (by-id "ex10-button-start-stop")
        prev-button (by-id "ex10-button-prev")
        next-button (by-id "ex10-button-next")
        start-stop  (events->chan start-stop-button EventType.CLICK)
        prev        (events->chan prev-button EventType.CLICK
                      (map (constantly :previous)))
        next        (events->chan next-button EventType.CLICK
                      (map (constantly :next)))
        animals     [:aardvark :beetle :cat :dog :elk :ferret
                     :goose :hippo :ibis :jellyfish :kangaroo]
        max-idx     (dec (count animals))
        show-card!  (partial show-card! "ex10-card")]
    (go
      (<! start-stop)
      (let [keys    (events->chan js/window EventType.KEYPRESS
                      (comp (map #(.-keyCode %))
                            (filter #{37 39})
                            (map {37 :previous 39 :next})))
            actions (async/merge [prev next keys])]
        (set! (.-innerHTML start-stop-button) "Stop!")
        (loop [idx 0]
          (init-buttons idx max-idx prev-button next-button)
          (show-card! (nth animals idx))
          (let [[v c] (alts! [actions start-stop])]
            (if (= c start-stop)
              (do
                (events/removeAll js/window EventType.KEYPRESS)
                (set! (.-innerHTML start-stop-button) "Done")
                (doseq [button [start-stop-button prev-button next-button]]
                  (classes/add button "disabled"))
                (show-card! ""))
              (condp = v
                :previous (if (pos? idx)
                            (recur (dec idx))
                            (recur idx))
                :next (if (< idx max-idx)
                        (recur (inc idx))
                        (recur idx))
                (recur idx)))))))))

(ex10)