(ns onyxia.input.mouse-position
  (:require [ysera.error :refer [error]]))

(def system-state-atom (atom {:mouse-listener            nil
                              :view-on-state-changed-fns #{}
                              :x                         nil
                              :y                         nil
                              :previous-x                nil
                              :previous-y                nil}))

(defn on-mouse-move
  [e]
  (swap! system-state-atom (fn [system-state]
                             (-> system-state
                                 (assoc :previous-x (:x system-state))
                                 (assoc :previous-y (:y system-state))
                                 (assoc :x (.-pageX e))
                                 (assoc :y (.-pageY e))))))

(when (nil? (:mouse-listener @system-state-atom))
  (.addEventListener js/document.body "mousemove" on-mouse-move)
  (swap! system-state-atom assoc :mouse-listener on-mouse-move))

(add-watch system-state-atom :system-watch (fn [_ _ _ system-state]
                                             (doseq [on-state-changed-fn (:view-on-state-changed-fns system-state)]
                                               (on-state-changed-fn))))

(defn get-mouse-position-input-value
  [system-state]
  {:x          (:x system-state)
   :y          (:y system-state)
   :previous-x (:previous-x system-state)
   :previous-y (:previous-y system-state)})

(def definition
  {:name         "mouse-position"
   :get-instance (fn [{on-state-changed :on-state-changed
                       should-update?   :should-update?
                       input-key        :input-key}]
                   (let [should-update? (or should-update? (fn [] true))
                         on-this-instance-state-changed (fn [] (when (should-update?) (on-state-changed)))]
                     (swap! system-state-atom (fn [system-state]
                                                (update-in system-state [:view-on-state-changed-fns] conj on-this-instance-state-changed)))
                     {:ready?       (fn [] true)
                      :get-input    (fn []
                                      {input-key (get-mouse-position-input-value @system-state-atom)})
                      :will-unmount (fn [_] (swap! system-state-atom (fn [system-state]
                                                                       (update-in system-state [:view-on-state-changed-fns] disj on-this-instance-state-changed))))}))})
