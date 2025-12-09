# IAP_Unity_Android
* unity android **原生**谷歌支付


## unity部分

将 **IAP.unitypackage** 导入unity

***

1. **json解析**  "com.unity.nuget.newtonsoft-json": "3.2.1"


2. 将 预制体 **IAPBridge** 放入**不会销毁的场景**，用来接收IAP消息


3. **初始化IAP** 示例代码为

    **注册回调**

        private void InitAction()
        {
            IAPReceiveMessage.getInstance().OnReceiveProductInfoSuccess += OnProductInfoReceived;
            IAPReceiveMessage.getInstance().OnReceiveProductInfoFailed += OnProductInfoFail;
            IAPReceiveMessage.getInstance().OnBuyProductSuccess += OnProductBuyComplete;
            IAPReceiveMessage.getInstance().OnBuyProductFailed += OnBuyProductFail;
            IAPReceiveMessage.getInstance().OnBuyProductCanceled += OnBuyProductCanceled;
            IAPReceiveMessage.getInstance().OnGetProvideContent += OnGetProvideContent;
            IAPReceiveMessage.getInstance().OnGetPurchaseHistory += OnReceivePurchaseHistory;
        }


    **请求商品**

        private async UniTask InitIAP()
        {
            IAPSendMessage.Instance().Init("IAPBridge", "AAA");//参数1:预制体名字, 参数2:当前无作用

            for (int i = 0; i < 30; i++)
            {
                if (IAPSendMessage.Instance().IsIAPSupported())
                {
                    break;
                }

                Debug.Log($"IAP READY FALSE");

                await UniTask.Delay(TimeSpan.FromSeconds(0.5f));
            }

            Debug.Log($"IAP READY TRUE");

            string[] arr = new[]
                { "item1","item2","item3"}; //请求的所有商品id

            string[] arr2 = new[]
                { "item1","item2"}; //非消耗品的商品id

            IAPSendMessage.Instance().RequestProducts(arr,aar2);
            IAPSendMessage.Instance().GetRestoreProductList();

            await UniTask.Delay(TimeSpan.FromSeconds(5f));

            IAPSendMessage.getInstance().CompleteUnfinishedProductList();
        }

4.购买代码:  `IAPSendMessage.Instance().BuyProduct("商品id");`

5.购买回调:  `IAPReceiveMessage` 类中

6.请求的所有商品信息:  `IAPReceiveMessage.productInfoData`

 	        //接收到产品信息
	        void OnProductInfoReceived(IAPProductInfoData info)
	        {
	            Debug.Log("[IAPMessage]Unity接收到商品信息个数:" + info.skuItems.Count);

	            IAPReceiveMessage.getInstance().productInfoData = info;
	
	            foreach (var skuItem in IAPReceiveMessage.getInstance().productInfoData.skuItems)
	            {
	                Debug.Log("[IAPMessage]Unity接收到商品信息:" + skuItem.productId);
	            }
	        }

