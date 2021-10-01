(ns nft.core
  (:require ["@alch/alchemy-web3" :refer (createAlchemyWeb3)]
            ["dotenv" :as env]

            [cljs.core.async :refer [go <! timeout]]
            [cljs.core.async.interop :refer-macros [<p!]]))

(defn main [& args]
  (js/console.log "Hi there!"))

(def contract-address "0x179Cc59676223ff26bDcFeb3aE7872aa12b3B0dA")

(defn node-slurp [path]
  (let [fs (js/require "fs")]
    (.readFileSync fs path "utf8")))

(defn promise-val
  [promise]
  (js/console.log promise)
  (let [result (atom {})]
    (go
      (let [promise-ch (<p! promise)]
        (js/console.log "promise channel" promise-ch)
        (reset! result {:result promise-ch})))
    (js/console.log "leaving promise code")
    (:result @result))
  #_(let [result (atom {})]
      (-> promise
          (.then (fn [r] (js/console.log "Promise value" r) (reset! result {:result r}))))
      (:result @result)

      #_(loop [i 0]
          (if (or (= i 5) (:result @result))
            (:result @result)
            (recur (inc i))))))

(defn mint-nft
  [token-uri]
  (let [config (:parsed (js->clj (.config env) :keywordize-keys true))
        web3 (createAlchemyWeb3 (:API_URL config))
        public-key (:PUBLIC_KEY config)
        private-key (:PRIVATE_KEY config)
        contract (->> "./artifacts/contracts/MyNFT.sol/MyNFT.json"
                      (node-slurp)
                      (.parse js/JSON))
        web3-eth (-> web3 .-eth)
        nft-contract (-> web3-eth (. -Contract) (new (. contract -abi) contract-address))
        nonce (.getTransactionCount web3-eth public-key "latest")]

    (-> nonce
        (.then (fn [n]
                 (let [tx {:from public-key
                           :to contract-address
                           :nonce n
                           :gas 500000
                           :maxPriorityFeePerGas 1999999987
                           :data (-> nft-contract (. -methods) (. mintNFT public-key token-uri) (. encodeABI) js->clj)}]
                   (-> web3-eth .-accounts (.signTransaction (clj->js tx) private-key)
                       (.then (fn [signed-tx]
                                (.sendSignedTransaction web3-eth (.-rawTransaction signed-tx)
                                                        (fn [err tx-hash]
                                                          (println err tx-hash)))))
                       (.catch #(js/console.log %)))))))

    #_(-> web3-eth .-accounts (.signTransaction (clj->js tx) private-key)
          (.then (fn [signed-tx]
                   (.sendSignedTransation web3-eth (.-rawTransaction signed-tx)
                                          (fn [err tx-hash]
                                            (println err tx-hash)))))
          (.catch #(js/console.log %)))))
;;actual nft
#_(mint-nft "https://=gateway.pinata.cloud/ipfs/QmQJmcrFAJM2ZKPzEzRBGumDpet8SSXTmf1vt7GaAEthit")   ;; nil 0x0ba91687e4764c92dac82a6b0c5794b14a5f61fe8f01ba48bae976914d5e38eb
;;the metadata
#_(mint-nft "https://gateway.pinata.cloud/ipfs/Qmf3hHTFBbAeoJhzVqnjWfoRfue2fkYpjRdG9eqgZVKJ3s")  ;;nil 0x1833918d2c87551ce84307c6c322195b5455c84ff064552339e9986f2ddcf2c3
