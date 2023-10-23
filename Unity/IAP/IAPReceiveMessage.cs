using System;
using System.Collections.Generic;
using Newtonsoft.Json;
using Newtonsoft.Json.Linq;
using UnityEngine;

namespace IAP
{
    /// <summary>
    /// 该类主要用于接收iOS和Android回调，做一个桥接用途
    /// </summary>
    public class IAPReceiveMessage : MonoBehaviour
    {
        class BuyCompleteData
        {
            public string productId;
            public string originalJson;
            public string signature;
        }

        class BuyFailData
        {
            public string productId;
            public string error;
        }


        private static IAPReceiveMessage _instance = null;

        public static IAPReceiveMessage Instance()
        {
            return _instance;
        }

        private void Awake()
        {
            DontDestroyOnLoad(this.gameObject);
            _instance = this;
        }


        #region callback from Objective-c/JAR

        //获取到产品列表回调
        void ReceiveProductInfos(string jsonData)
        {
            if (string.IsNullOrEmpty(jsonData)) return;
            var infoData = JsonConvert.DeserializeObject<IAPProductInfoData>(jsonData);
            OnProductInfoReceived(infoData);
        }

        //产品列表请求失败
        void ProductRequestFail(string message)
        {
            OnProductInfoFail(message);
        }

        //购买成功回调
        void ProductBuyComplete(string jsonData)
        {
            var infoData = JsonConvert.DeserializeObject<BuyCompleteData>(jsonData);
            OnProductBuyComplete(infoData.productId, infoData.originalJson, infoData.signature);
        }

        //购买失败回调
        void ProductBuyFailed(string jsonData)
        {
            var infoData = JsonConvert.DeserializeObject<BuyFailData>(jsonData);
            OnBuyProductFail(infoData.productId, infoData.error);
        }

        //获取商品回执回调
        void ProvideContent(string msg)
        {
        }

        //购买取消回调
        void ProductBuyCanceled(string productId)
        {
            OnBuyProductCanceled(productId);
        }

        //购买记录
        void ReceivePurchaseHistory(string productIdList)
        {
            var idGroup = productIdList.Split('+');
            OnReceivePurchaseHistory(idGroup);
        }

        #endregion


        #region 游戏逻辑

        public IAPProductInfoData productInfoData = new IAPProductInfoData();

        //接收到产品信息
        void OnProductInfoReceived(IAPProductInfoData info)
        {
            Debug.Log("[IAPMessage]Unity接收到商品信息");

            productInfoData = info;

            foreach (var skuItem in productInfoData.skuItems)
            {
                Debug.Log("[IAPMessage]Unity接收到商品信息:" + skuItem.productId);
            }
        }

        //接收到产品信息
        void OnProductInfoFail(string error)
        {
            Debug.Log("[IAPMessage]Unity商品信息请求失败:" + error);
        }

        //购买完成
        void OnProductBuyComplete(string productId, string originalJson, string signature)
        {
            Debug.Log("[IAPMessage]购买完成" + productId);
        }


        //购买失败
        void OnBuyProductFail(string productId, string error)
        {
            Debug.Log($"[IAPMessage]购买失败:{productId} 错误信息{error}");
        }

        //购买取消
        void OnBuyProductCanceled(string productId)
        {
            Debug.Log("[IAPMessage]购买取消" + productId);
        }

        //接收购买记录
        void OnReceivePurchaseHistory(string[] idGroup)
        {
            foreach (var id in idGroup)
            {
                Debug.Log("[IAPMessage]恢复购买:" + id);
            }
        }

        #endregion
    }
}