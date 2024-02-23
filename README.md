# IAP_Unity_Android
* unity android 原生谷歌支付


## unity部分

将 IAP.unitypackage 导入unity

1. json解析  "com.unity.nuget.newtonsoft-json": "3.1.0"

2. 将 预制体 IAPBridge 放入场景，用来接收IAP消息

3. 初始化IAP
   示例代码为

    注册回调

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


    请求商品

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

            IAPSendMessage.Instance().RequestProducts(arr);
            IAPSendMessage.Instance().GetRestoreProductList();
        }

4.购买代码  IAPSendMessage.Instance().BuyProduct("商品id", 是否是消耗品 );

5.购买回调  IAPReceiveMessage类中

6.请求的所有商品信息  IAPReceiveMessage.productInfoData
