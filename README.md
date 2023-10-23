# IAP_Unity_Android
unity android 原生谷歌支付


*unity

1. json解析  "com.unity.nuget.newtonsoft-json": "3.1.0"
2. 将 预制体 IAPBridge 放入场景，用来接收IAP消息
3. 初始化IAP
   示例代码为
   
            private async UniTask InitIAP()
            {
            IAPSendMessage.Instance().Init("IAPBridge", "AAA");

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
                { "item1","item2","item3"};
            IAPSendMessage.Instance().RequestProducts(arr);
            IAPSendMessage.Instance().GetRestoreProductList();
        }

