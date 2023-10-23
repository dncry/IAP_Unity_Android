using System;
using UnityEngine;
using System.Collections;
using System.Collections.Generic;
using Newtonsoft.Json;

namespace IAP
{
    public class IAPSendMessage
    {
        class BuyProductData
        {
            public string productId;
            public bool isConsumable;
        }

        class BuyProductData2
        {
            public string[] productIds;
        }

        private AndroidJavaObject javaObject;

        private IAPSendMessage()
        {
            if (Application.platform != RuntimePlatform.Android)
                return;
            javaObject = new AndroidJavaObject("com.iap.util.MainActivity");
        }

        private volatile static IAPSendMessage _instance = null;
        private static readonly object lockHelper = new object();

        public static IAPSendMessage Instance()
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
            //publicKey 当前无作用

            Debug.Log("[IAPBridge]Init：" + goName + "=====" + publicKey);
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
            BuyProductData2 buyProductData = new BuyProductData2();
            buyProductData.productIds = productIds;
            string jsonData = JsonConvert.SerializeObject(buyProductData);

            Debug.Log(jsonData);

            RequestProducts(jsonData);
        }

        private void RequestProducts(string jsonData)
        {
            Debug.Log("[IAPBridge]RequestProduct：" + jsonData);
            if (Application.platform != RuntimePlatform.Android)
                return;
            javaObject.Call("RequestProduct", jsonData);
        }

        public void BuyProduct(string productId, bool isConsumable)
        {
            BuyProductData buyProductData = new BuyProductData();
            buyProductData.productId = productId;
            buyProductData.isConsumable = isConsumable;
            string jsonData = JsonConvert.SerializeObject(buyProductData);
            Debug.Log("[IAPBridge]BuyProduct：" + jsonData);
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