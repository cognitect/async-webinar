(ns webinar.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :as async
             :refer [>! <! put! chan alts!]]
            [goog.events :as events]
            [goog.dom.classes :as classes])
  (:import [goog.events EventType]))

(enable-console-print!)

;; =============================================================================
;; Utilities

(defn by-id
  "Short-hand for document.getElementById(id)"
  [id]
  (.getElementById js/document id))

(defn events->chan
  "Given a target DOM element and event type return a channel of
  observed events. Can supply the channel to receive events as third
  optional argument."
  ([el event-type] (events->chan el event-type (chan)))
  ([el event-type c]
   (events/listen el event-type
     (fn [e] (put! c e)))
   c))

(defn mouse-loc->vec
  "Given a Google Closure normalized DOM mouse event return the
  mouse x and y position as a two element vector."
  [e]
  [(.-clientX e) (.-clientY e)])

(defn show!
  "Given a CSS id and a message string append a child paragraph element
  with the given message string."
  [id msg]
  (let [el (.getElementById js/document id)
        p  (.createElement js/document "p")]
    (set! (.-innerHTML p) msg)
    (.appendChild el p)))

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
                 (chan 1 (map mouse-loc->vec)))
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
                 (chan 1 (comp (map mouse-loc->vec)
                               (filter (fn [[_ y]] (zero? (mod y 5)))))))
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

(defn set-html!
  "Given a CSS id, replace the matching DOM element's
  content with the supplied string."
  [id s]
  (set! (.-innerHTML (by-id id)) s))

(defn ex9 []
  (let [prev-button (by-id "ex9-button-prev")
        next-button (by-id "ex9-button-next")
        prev        (events->chan prev-button EventType.CLICK)
        next        (events->chan next-button EventType.CLICK)
        animals     [:aardvark :beetle :cat :dog :elk :ferret
                     :goose :hippo :ibis :jellyfish :kangaroo]
        max-idx     (dec (count animals))
        set-html!   (partial set-html! "ex9-card")]
    (go
      (loop [idx 0]
        (if (zero? idx)
          (classes/add prev-button "disabled")
          (classes/remove prev-button "disabled"))
        (if (== idx max-idx)
          (classes/add next-button "disabled")
          (classes/remove next-button "disabled"))
        (set-html! (nth animals idx))
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
;; Example 10

(defn style-buttons!
  "Given a current index and an upper bound disable
  or enable the given previous and next controls."
  [i max prev next]
  (if (zero? i)
    (classes/add prev "disabled")
    (classes/remove prev "disabled"))
  (if (== i max)
    (classes/add next "disabled")
    (classes/remove next "disabled")))

(defn disable-buttons!
  "Given a list of buttons disable them all. The
  first element should be a start/stop button for
  Example 10."
  [[start-stop-button :as buttons]]
  (set! (.-innerHTML start-stop-button) "Done")
  (doseq [button buttons]
    (classes/add button "disabled")))

(defn keys-chan
  "Return a channel of :previous and :next events
  sourced from left and right arrow key presses."
  []
  (events->chan js/window EventType.KEYDOWN
    (chan 1 (comp (map #(.-keyCode %))
                  (filter #{37 39})
                  (map {37 :previous 39 :next})))))

(defn ex10 [animals]
  (let [start-stop-button (by-id "ex10-button-start-stop")
        prev-button (by-id "ex10-button-prev")
        next-button (by-id "ex10-button-next")
        start-stop  (events->chan start-stop-button EventType.CLICK)
        prev        (events->chan prev-button EventType.CLICK
                      (chan 1 (map (constantly :previous))))
        next        (events->chan next-button EventType.CLICK
                      (chan 1 (map (constantly :next))))
        max-idx     (dec (count animals))
        set-html!  (partial set-html! "ex10-card")]
    (go
      ;; wait to start
      (<! start-stop)
      ;; start listening to key events now
      (let [keys    (keys-chan)
            actions (async/merge [prev next keys])]
        (set! (.-innerHTML start-stop-button) "Stop!")
        (loop [idx 0]
          (style-buttons! idx max-idx prev-button next-button)
          (set-html! (nth animals idx))
          ;; wait for next action
          (let [[action c] (alts! [actions start-stop])]
            (if (= c start-stop)
              (do
                (events/removeAll js/window EventType.KEYDOWN)
                (disable-buttons! [start-stop-button prev-button next-button])
                (set-html! ""))
              (condp = action
                :previous (if (pos? idx)
                            (recur (dec idx))
                            (recur idx))
                :next (if (< idx max-idx)
                        (recur (inc idx))
                        (recur idx))
                (recur idx)))))))))

(ex10 [:aardvark :beetle :cat :dog :elk :ferret
       :goose :hippo :ibis :jellyfish :kangaroo])