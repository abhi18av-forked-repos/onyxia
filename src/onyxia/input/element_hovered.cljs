(ns onyxia.input.element-hovered
  (:require [onyxia.attributes :refer [add-event-handler]]
            [ysera.error :refer [error]]))

(def definition
  {:name         "element-hovered"
   :get-instance (fn [{on-state-changed                :on-state-changed
                       still-to-initiate-hover         :still-to-initiate-hover
                       still-to-remain-hovered         :still-to-remain-hovered
                       mouse-position-input-definition :mouse-position-input-definition
                       should-update?                  :should-update?}]
                   (when (and still-to-initiate-hover (not mouse-position-input-definition))
                     (error ":mouse-position-input-definition is required when :still is true."))
                   (when (and (not still-to-initiate-hover) still-to-remain-hovered)
                     (error ":still-to-remain-hovered cannot be true without :still-to-initiate-hover being true. That doesn't make sense."))
                   (let [should-update? (or should-update? (fn [] true))
                         state-atom (atom {:hover-value      nil
                                           :still            false
                                           :still-timeout-id nil})
                         on-still-timeout (fn []
                                            (when (should-update?)
                                              (swap! state-atom assoc :still true :still-timeout-id nil)))
                         mouse-position-instance (when still-to-initiate-hover
                                                   ((:get-instance mouse-position-input-definition)
                                                     {:on-state-changed (fn []
                                                                          (let [state (deref state-atom)]
                                                                            (when-let [still-timeout-id (:still-timeout-id state)]
                                                                              (js/clearTimeout still-timeout-id))
                                                                            (when (and (:hover-value state)
                                                                                       (or still-to-remain-hovered
                                                                                           (not (:still state))))
                                                                              (when (should-update?)
                                                                                (swap! state-atom assoc :still false :still-timeout-id (js/setTimeout on-still-timeout 100))))))}))]
                     (add-watch state-atom :state-change-notifier (fn [_ _ old-state new-state]
                                                                    (let [hover-changed (not= (:hover-value old-state) (:hover-value new-state))]
                                                                      (cond
                                                                        still-to-initiate-hover
                                                                        (when (or hover-changed
                                                                                  (not= (:still old-state) (:still new-state)))
                                                                          (on-state-changed))

                                                                        :else
                                                                        (when hover-changed
                                                                          (on-state-changed))))))
                     {:ready?                     (fn [] true)
                      :get-value                  (fn []
                                                    (let [state (deref state-atom)]
                                                      (and (or (not still-to-initiate-hover) (:still state))
                                                           (:hover-value state))))
                      :element-attribute-modifier (fn [{attributes :attributes}]
                                                    (when-let [hover-value (:element-hovered-value attributes)]
                                                      (-> attributes
                                                          (dissoc :element-hovered-value)
                                                          (add-event-handler :on-mouse-enter (fn []
                                                                                               (when (should-update?)
                                                                                                 (swap! state-atom assoc :hover-value hover-value :still false))))
                                                          (add-event-handler :on-mouse-leave (fn []
                                                                                               (when (should-update?)
                                                                                                 (swap! state-atom assoc :hover-value nil :still false)))))))
                      :will-unmount               (fn [args]
                                                    (when mouse-position-instance
                                                      ((:will-unmount mouse-position-instance) args)))}))})
