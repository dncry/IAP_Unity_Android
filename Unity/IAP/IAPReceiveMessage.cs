using System;
using UnityEngine;
using Newtonsoft.Json;

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

        public Action<IAPProductInfoData> OnReceiveProductInfoSuccess;
        public Action<string> OnReceiveProductInfoFailed;
        public Action<string, string, string> OnBuyProductSuccess;
        public Action<string, string> OnBuyProductFailed;
        public Action<string> OnBuyProductCanceled;
        public Action<string> OnGetProvideContent;
        public Action<string[]> OnGetPurchaseHistory;

        public IAPProductInfoData productInfoData = new IAPProductInfoData();

        private static IAPReceiveMessage _instance = null;

        public static IAPReceiveMessage getInstance()
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

            OnReceiveProductInfoSuccess?.Invoke(infoData);
        }

        //产品列表请求失败
        void ProductRequestFail(string message)
        {
            OnReceiveProductInfoFailed?.Invoke(message);
        }

        //购买成功回调
        void ProductBuyComplete(string jsonData)
        {
            var infoData = JsonConvert.DeserializeObject<BuyCompleteData>(jsonData);

            OnBuyProductSuccess?.Invoke(infoData.productId, infoData.originalJson, infoData.signature);
        }

        //购买失败回调
        void ProductBuyFailed(string jsonData)
        {
            var infoData = JsonConvert.DeserializeObject<BuyFailData>(jsonData);
            OnBuyProductFailed?.Invoke(infoData.productId, infoData.error);
        }


        //购买取消回调
        void ProductBuyCanceled(string productId)
        {
            OnBuyProductCanceled?.Invoke(productId);
        }

        //获取商品回执回调
        void ProvideContent(string msg)
        {
            OnGetProvideContent?.Invoke(msg);
        }

        //购买记录
        void ReceivePurchaseHistory(string productIdList)
        {
            var idGroup = productIdList.Split('+');
            OnGetPurchaseHistory?.Invoke(idGroup);
        }

        #endregion
    }
}
