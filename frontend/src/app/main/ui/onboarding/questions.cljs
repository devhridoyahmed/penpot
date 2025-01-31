;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) KALEIDOS INC

(ns app.main.ui.onboarding.questions
  "External form for onboarding questions."
  (:require-macros [app.main.style :as stl])
  (:require
   [app.common.data :as d]
   [app.common.data.macros :as dm]
   [app.main.data.events :as-alias ev]
   [app.main.data.users :as du]
   [app.main.store :as st]
   [app.main.ui.components.forms :as fm]
   [app.main.ui.icons :as i]
   [app.util.i18n :as i18n :refer [tr]]
   [cljs.spec.alpha :as s]
   [cuerdas.core :as str]
   [potok.v2.core :as ptk]
   [rumext.v2 :as mf]))

(mf/defc step-container
  {::mf/props :obj}
  [{:keys [form step on-next on-prev children class label]}]

  (let [on-next*
        (mf/use-fn
         (mf/deps on-next step label)
         (fn [form event]
           (let [params (-> (:clean-data @form)
                            (assoc :label label)
                            (assoc :step step)
                            (assoc ::ev/name "onboarding-step"))]
             (st/emit! (ptk/data-event ::ev/event params))
             (on-next form event))))]

    [:& fm/form {:form form
                 :on-submit on-next*
                 :class (dm/str class " " (stl/css :form-wrapper))}
     [:div {:class (stl/css :paginator)} (str/ffmt "%/5" step)]

     children

     [:div {:class (stl/css :action-buttons)}

      (when (some? on-prev)
        [:button {:class (stl/css :prev-button)
                  :on-click on-prev}
         (tr "labels.previous")])

      [:> fm/submit-button*
       {:label (if (< step 5)
                 (tr "labels.next")
                 (tr "labels.start"))
        :class (stl/css :next-button)}]]]))

(s/def ::questions-form-step-1
  (s/keys :req-un [::planning
                   ::expected-use]
          :opt-un [::planning-other]))

(defn- step-1-form-validator
  [errors data]
  (let [planning       (:planning data)
        planning-other (:planning-other data)]
    (cond-> errors
      (and (= planning "other")
           (str/blank? planning-other))
      (assoc :planning-other {:code "missing"})

      (not= planning "other")
      (assoc :planning-other nil)

      (str/blank? planning)
      (assoc :planning {:code "missing"}))))

(mf/defc step-1
  {::mf/props :obj}
  [{:keys [on-next form]}]
  (let [use-options
        (mf/with-memo []
          (shuffle [{:label (tr "onboarding.questions.use.work") :value "work"}
                    {:label (tr "onboarding.questions.use.education") :value "education"}
                    {:label (tr "onboarding.questions.use.personal") :value "personal"}]))

        planning-options
        (mf/with-memo []
          (-> (shuffle [{:label (tr "labels.select-option")
                         :value "" :key "questions:what-brings-you-here"
                         :disabled true}
                        {:label (tr "onboarding.questions.reasons.exploring")
                         :value "discover-more-about-penpot"
                         :key "discover-more-about-penpot"}
                        {:label (tr "onboarding.questions.reasons.fit")
                         :value "test-penpot-to-see-if-its-a-fit-for-team"
                         :key "test-penpot-to-see-if-its-a-fit-for-team"}
                        {:label (tr "onboarding.questions.reasons.alternative")
                         :value "alternative-to-figma"
                         :key "alternative-to-figma"}
                        {:label (tr "onboarding.questions.reasons.testing")
                         :value "try-out-before-using-penpot-on-premise"
                         :key "try-out-before-using-penpot-on-premise"}])
              (conj {:label (tr "labels.other-short") :value "other"})))

        current-planning
        (dm/get-in @form [:data :planning])]

    [:& step-container {:form form
                        :step 1
                        :label "questions:about-you"
                        :on-next on-next
                        :class (stl/css :step-1)}

     [:img {:class (stl/css :header-image)
            :src "images/form/use-for-1.png"
            :alt (tr "onboarding.questions.lets-get-started")}]
     [:h1 {:class (stl/css :modal-title)}
      (tr "onboarding.questions.step1.title")]
     [:p {:class (stl/css :modal-text)}
      (tr "onboarding.questions.step1.subtitle")]

     [:div {:class (stl/css :modal-question)}
      [:h3 {:class (stl/css :modal-subtitle)}
       (tr "onboarding.questions.step1.question1")]

      [:& fm/radio-buttons {:options use-options
                            :name :expected-use
                            :class (stl/css :radio-btns)}]

      [:h3 {:class (stl/css :modal-subtitle)}
       (tr "onboarding.questions.step1.question2")]

      [:& fm/select
       {:options planning-options
        :select-class (stl/css :select-class)
        :default ""
        :name :planning
        :dropdown-class (stl/css :question-dropdown)}]

      (when (= current-planning "other")
        [:& fm/input {:name :planning-other
                      :class (stl/css :input-spacing)
                      :placeholder (tr "labels.other")
                      :label ""}])]]))

(s/def ::questions-form-step-2
  (s/keys :req-un [::experience-design-tool]
          :opt-un [::experience-design-tool-other]))

(defn- step-2-form-validator
  [errors data]
  (let [experience       (:experience-design-tool data)
        experience-other (:experience-design-tool-other data)]

    (cond-> errors
      (and (= experience "other")
           (str/blank? experience-other))
      (assoc :experience-design-tool-other {:code "missing"})

      (not= experience "other")
      (assoc :experience-design-tool-other nil))))

(mf/defc step-2
  {::mf/props :obj}
  [{:keys [on-next on-prev form]}]
  (let [design-tool-options
        (mf/with-memo []
          (-> (shuffle [{:label (tr "labels.figma")  :img-width "48px" :img-height "60px"
                         :value "figma" :image "images/form/figma.png"}
                        {:label (tr "labels.sketch") :img-width "48px" :img-height "60px"
                         :value "sketch" :image "images/form/sketch.png"}
                        {:label (tr "labels.adobe-xd") :img-width "48px" :img-height "60px"
                         :value "adobe-xd" :image "images/form/adobe-xd.png"}
                        {:label (tr "labels.canva") :img-width "48px" :img-height "60px"
                         :value "canva" :image "images/form/canva.png"}
                        {:label (tr "labels.invision")  :img-width "48px" :img-height "60px"
                         :value "invision" :image "images/form/invision.png"}])
              (conj {:label (tr "labels.other-short")  :value "other" :icon i/curve})))

        current-experience
        (dm/get-in @form [:clean-data :experience-design-tool])

        on-design-tool-change
        (mf/use-fn
         (mf/deps current-experience)
         (fn []
           (when (not= current-experience "other")
             (swap! form d/dissoc-in [:data :experience-design-tool-other])
             (swap! form d/dissoc-in [:errors :experience-design-tool-other]))))]

    [:& step-container {:form form
                        :step 2
                        :label "questions:experience-design-tool"
                        :on-next on-next
                        :on-prev on-prev
                        :class (stl/css :step-2)}

     [:h1 {:class (stl/css :modal-title)}
      (tr "onboarding.questions.step2.title")]
     [:div {:class (stl/css :radio-wrapper)}
      [:& fm/image-radio-buttons {:options design-tool-options
                                  :img-width "48px"
                                  :img-height "60px"
                                  :name :experience-design-tool
                                  :image true
                                  :class (stl/css :image-radio)
                                  :on-change on-design-tool-change}]

      (when (= current-experience "other")
        [:& fm/input {:name :experience-design-tool-other
                      :class (stl/css :input-spacing)
                      :placeholder (tr "labels.other")
                      :label ""}])]]))

(s/def ::questions-form-step-3
  (s/keys :req-un [::team-size ::role ::responsability]
          :opt-un [::role-other ::responsability-other]))

(defn- step-3-form-validator
  [errors data]
  (let [role                 (:role data)
        role-other           (:role-other data)
        responsability       (:responsability data)
        responsability-other (:responsability-other data)]

    (cond-> errors
      (and (= role "other")
           (str/blank? role-other))
      (assoc :role-other {:code "missing"})

      (not= role "other")
      (assoc :role-other nil)

      (and (= responsability "other")
           (str/blank? responsability-other))
      (assoc :responsability-other {:code "missing"})

      (not= responsability "other")
      (assoc :responsability-other nil))))

(mf/defc step-3
  {::mf/props :obj}
  [{:keys [on-next on-prev form]}]
  (let [role-options
        (mf/with-memo []
          (-> (shuffle [{:label (tr "labels.select-option") :value "" :key "role" :disabled true}
                        {:label (tr "labels.product-design") :value "ux" :key "ux"}
                        {:label (tr "labels.developer") :value "developer"  :key "developer"}
                        {:label (tr "labels.student-teacher") :value "student-teacher" :key "student"}
                        {:label (tr "labels.graphic-design") :value "designer" :key "design"}
                        {:label (tr "labels.marketing") :value "marketing" :key "marketing"}
                        {:label (tr "labels.product-management") :value "manager" :key "manager"}])
              (conj {:label (tr "labels.other-short") :value "other"})))

        responsability-options
        (mf/with-memo []
          (-> (shuffle [{:label (tr "labels.select-option") :value "" :key "responsability" :disabled true}
                        {:label (tr "labels.team-leader") :value "team-leader"}
                        {:label (tr "labels.team-member") :value "team-member"}
                        {:label (tr "labels.freelancer") :value "freelancer"}
                        {:label (tr "labels.founder") :value "ceo-founder"}
                        {:label (tr "labels.director") :value "director"}])
              (conj {:label (tr "labels.other-short") :value "other"})))


        team-size-options
        (mf/with-memo []
          [{:label (tr "labels.select-option") :value "" :key "team-size" :disabled true}
           {:label (tr "onboarding.questions.team-size.more-than-50") :value "more-than-50" :key "more-than-50"}
           {:label (tr "onboarding.questions.team-size.31-50") :value "31-50"  :key "31-50"}
           {:label (tr "onboarding.questions.team-size.11-30") :value "11-30" :key "11-30"}
           {:label (tr "onboarding.questions.team-size.2-10") :value "2-10" :key "2-10"}
           {:label (tr "onboarding.questions.team-size.freelancer") :value "freelancer" :key "freelancer"}
           {:label (tr "onboarding.questions.team-size.personal-project") :value "personal-project" :key "personal-project"}])

        current-role
        (dm/get-in @form [:data :role])

        current-responsability
        (dm/get-in @form [:data :responsability])]

    [:& step-container {:form form
                        :step 3
                        :label "questions:about-your-job"
                        :on-next on-next
                        :on-prev on-prev
                        :class (stl/css :step-3)}

     [:h1 {:class (stl/css :modal-title)}
      (tr "onboarding.questions.step3.title")]
     [:div {:class (stl/css :modal-question)}
      [:h3 {:class (stl/css :modal-subtitle)} (tr "onboarding.questions.step3.question1")]
      [:& fm/select {:options role-options
                     :select-class (stl/css :select-class)
                     :default ""
                     :name :role}]

      (when (= current-role "other")
        [:& fm/input {:name :role-other
                      :class (stl/css :input-spacing)
                      :placeholder (tr "labels.other")
                      :label ""}])]

     [:div {:class (stl/css :modal-question)}
      [:h3 {:class (stl/css :modal-subtitle)} (tr "onboarding.questions.step3.question2")]
      [:& fm/select {:options responsability-options
                     :select-class (stl/css :select-class)
                     :default ""
                     :name :responsability}]

      (when (= current-responsability "other")
        [:& fm/input {:name :responsability-other
                      :class (stl/css :input-spacing)
                      :placeholder (tr "labels.other")
                      :label ""}])]

     [:div {:class (stl/css :modal-question)}
      [:h3 {:class (stl/css :modal-subtitle)} (tr "onboarding.questions.step3.question3")]
      [:& fm/select {:options team-size-options
                     :default ""
                     :select-class (stl/css :select-class)
                     :name :team-size}]]]))

(s/def ::questions-form-step-4
  (s/keys :req-un [::start-with]
          :opt-un [::start-with-other]))

(defn- step-4-form-validator
  [errors data]
  (let [start       (:start-with data)
        start-other (:start-with-other data)]
    (cond-> errors
      (and (= start "other")
           (str/blank? start-other))
      (assoc :start-with-other {:code "missing"})

      (not= start "other")
      (assoc :start-with-other nil))))

(mf/defc step-4
  {::mf/props :obj}
  [{:keys [on-next on-prev form]}]
  (let [start-options
        (mf/with-memo []
          (-> (shuffle [{:label (tr "onboarding.questions.start-with.ui")
                         :value "ui" :image "images/form/Design.png"}
                        {:label (tr "onboarding.questions.start-with.wireframing")
                         :value "wireframing" :image "images/form/templates.png"}
                        {:label (tr "onboarding.questions.start-with.prototyping")
                         :value "prototyping" :image "images/form/Prototype.png"}
                        {:label (tr "onboarding.questions.start-with.ds")
                         :value "ds" :image "images/form/components.png"}
                        {:label (tr "onboarding.questions.start-with.code")
                         :value "code" :image "images/form/design-and-dev.png"}])
              (conj {:label (tr "labels.other-short") :value "other" :icon i/curve})))

        current-start (dm/get-in @form [:data :start-with])

        on-start-change
        (mf/use-fn
         (mf/deps current-start)
         (fn [_ _]
           (when (not= current-start "other")
             (swap! form d/dissoc-in [:data :start-with-other])
             (swap! form d/dissoc-in [:errors :start-with-other]))))]

    [:& step-container {:form form
                        :step 4
                        :label "questions:how-start"
                        :on-next on-next
                        :on-prev on-prev
                        :class (stl/css :step-4)}

     [:h1 {:class (stl/css :modal-title)} (tr "onboarding.questions.step4.title")]
     [:div {:class (stl/css :radio-wrapper)}
      [:& fm/image-radio-buttons {:options start-options
                                  :img-width "159px"
                                  :img-height "120px"
                                  :class (stl/css :image-radio)
                                  :name :start-with
                                  :on-change on-start-change}]

      (when (= current-start "other")
        [:& fm/input {:name :start-with-other
                      :class (stl/css :input-spacing)
                      :label ""
                      :placeholder (tr "labels.other")}])]]))

(s/def ::questions-form-step-5
  (s/keys :req-un [::referer]
          :opt-un [::referer-other]))

(defn- step-5-form-validator
  [errors data]
  (let [referer       (:referer data)
        referer-other (:referer-other data)]
    (cond-> errors
      (and (= referer "other")
           (str/blank? referer-other))
      (assoc :referer-other {:code "missing"})

      (not= referer "other")
      (assoc :referer-other nil))))

(mf/defc step-5
  {::mf/props :obj}
  [{:keys [on-next on-prev form]}]
  (let [referer-options
        (mf/with-memo []
          (-> (shuffle [{:label (tr "labels.youtube") :value "youtube"}
                        {:label (tr "labels.event") :value "event"}
                        {:label (tr "labels.search") :value "search"}
                        {:label (tr "labels.social") :value "social"}
                        {:label (tr "labels.article") :value "article"}])
              (conj {:label (tr "labels.other-short") :value "other"})))

        current-referer
        (dm/get-in @form [:data :referer])

        on-referer-change
        (mf/use-fn
         (mf/deps current-referer)
         (fn []
           (when (not= current-referer "other")
             (swap! form d/dissoc-in [:data :referer-other])
             (swap! form d/dissoc-in [:errors :referer-other]))))]

    [:& step-container {:form form
                        :step 5
                        :label "questions:referer"
                        :on-next on-next
                        :on-prev on-prev
                        :class (stl/css :step-5)}

     [:h1 {:class (stl/css :modal-title)} (tr "onboarding.questions.step5.title")]
     [:div {:class (stl/css :radio-wrapper)}
      [:& fm/radio-buttons {:options referer-options
                            :class (stl/css :radio-btns)
                            :name :referer
                            :on-change on-referer-change}]
      (when (= current-referer "other")
        [:& fm/input {:name :referer-other
                      :class (stl/css :input-spacing)
                      :label ""
                      :placeholder (tr "labels.other")}])]]))

(mf/defc questions-modal
  []
  (let [container   (mf/use-ref)
        step        (mf/use-state 1)
        clean-data  (mf/use-state {})

        ;; Forms are initialized here because we can go back and forth between the steps
        ;; and we want to keep the filled info
        step-1-form (fm/use-form
                     :initial {}
                     :validators [step-1-form-validator]
                     :spec ::questions-form-step-1)

        step-2-form (fm/use-form
                     :initial {}
                     :validators [step-2-form-validator]
                     :spec ::questions-form-step-2)

        step-3-form (fm/use-form
                     :initial {}
                     :validators [step-3-form-validator]
                     :spec ::questions-form-step-3)

        step-4-form (fm/use-form
                     :initial {}
                     :validators [step-4-form-validator]
                     :spec ::questions-form-step-4)

        step-5-form (fm/use-form
                     :initial {}
                     :validators [step-5-form-validator]
                     :spec ::questions-form-step-5)

        on-next
        (mf/use-fn
         (fn [form]
           (swap! step inc)
           (swap! clean-data merge (:clean-data @form))))

        on-prev
        (mf/use-fn
         (fn []
           (swap! step dec)))

        on-submit
        (mf/use-fn
         (mf/deps @clean-data)
         (fn [form]
           (let [data (merge @clean-data (:clean-data @form))]
             (reset! clean-data data)
             (st/emit! (du/mark-questions-as-answered data)))))]

    [:div {:class (stl/css-case
                   :modal-overlay true)}
     [:div {:class (stl/css :modal-container)
            :ref container}

      (case @step
        1 [:& step-1 {:on-next on-next :on-prev on-prev :form step-1-form}]
        2 [:& step-2 {:on-next on-next :on-prev on-prev :form step-2-form}]
        3 [:& step-3 {:on-next on-next :on-prev on-prev :form step-3-form}]
        4 [:& step-4 {:on-next on-next :on-prev on-prev :form step-4-form}]
        5 [:& step-5 {:on-next on-submit :on-prev on-prev :form step-5-form}])]]))
