using System;
using UnityEngine;
using Newtonsoft.Json;

namespace IAP
{
    [Serializable]
    public class IAPBuyProductData
    {
        public string productId;
        public bool isConsumable;
    }

    [Serializable]
    public class IAPBuyProductData2
    {
        public string[] productIds;
    }

    [Serializable]
    public class IAPSendMessage
    {
        private AndroidJavaObject javaObject;

        private IAPSendMessage()
        {
            if (Application.platform != RuntimePlatform.Android)
                return;
            javaObject = new AndroidJavaObject("com.iap.util.MainActivity");
        }

        private volatile static IAPSendMessage _instance = null;
        private static readonly object lockHelper = new object();

        public static IAPSendMessage getInstance()
        {
            if (_instance == null)
            {
                lock (lockHelper)
                {
                    if (_instance == null)
                        _instance = new IAPSendMessage();
                }
            }

            return _instance;
        }


        /// <summary>
        ///  初始调用   Init + RequestProducts + Restore
        ///  IAPSendMessage.getInstance().Init("IAPBridge", "AAAAAAAA");
        ///  string[] arr = new[] { Constant.Store.Item1};
        ///  IAPSendMessage.getInstance().RequestProducts(arr);
        ///  IAPSendMessage.getInstance().GetRestoreProductList();
        /// </summary>
        public void Init(string goName, string publicKey)
        {
            //publicKey未起作用

            Debug.Log("unity- [IAPBridge]Init：" + goName + "=====" + publicKey);
            if (Application.platform != RuntimePlatform.Android)
                return;
            javaObject.Call("Init", goName, publicKey);
        }

        public bool IsIAPSupported()
        {
            if (Application.platform != RuntimePlatform.Android)
                return false;
            return javaObject.Call<bool>("IsIAPSupported");
        }


        public void RequestProducts(string[] productIds)
        {
            IAPBuyProductData2 iapBuyProductData = new IAPBuyProductData2();
            iapBuyProductData.productIds = productIds;
            string jsonData = JsonConvert.SerializeObject(iapBuyProductData);

            Debug.Log($"unity-  请求产品列表{jsonData}");

            RequestProducts(jsonData);
        }

        private void RequestProducts(string jsonData)
        {
            Debug.Log("unity-[IAPBridge]RequestProduct：" + jsonData);
            if (Application.platform != RuntimePlatform.Android)
                return;
            javaObject.Call("RequestProduct", jsonData);
        }

        public void BuyProduct(string productId, bool isConsumable)
        {
            IAPBuyProductData iapBuyProductData = new IAPBuyProductData();
            iapBuyProductData.productId = productId;
            iapBuyProductData.isConsumable = isConsumable;
            string jsonData = JsonConvert.SerializeObject(iapBuyProductData);
            Debug.Log("unity-[IAPBridge]BuyProduct：" + jsonData);
            if (Application.platform != RuntimePlatform.Android)
                return;
            javaObject.Call("BuyProduct", jsonData);
        }

        public void GetRestoreProductList()
        {
            if (Application.platform != RuntimePlatform.Android)
                return;
            javaObject.Call("GetRestoreProductList");
        }
    }
}