using System;
using System.Collections.Generic;

namespace IAP
{
    [Serializable]
    public class IAPProductInfoData
    {
        public List<IAPSkuItem> skuItems; //请求到的产品列表
        public string[] invalidIds; //无效产品id
    }
    
    [Serializable]
    public struct IAPSkuItem
    {
        public string productId; //后台产品id
        public string title; //后台标题
        public string desc; //后台描述
        public double price; //无单位价格
        public string formatPrice; //格式化价格，包括其货币符号
        public string priceCurrencyCode; //货币代码
        public string skuType; //内购还是订阅 subscription/inapp

        public override string ToString()
        {
            return string.Format(
                "[productId]:{0} [title]:{1} [desc]:{2} [price]:{3} [formatPrice]:{4} [priceCurrencyCode]:{5} [skuType:]{6}",
                productId, title, desc, price, formatPrice, priceCurrencyCode, skuType);
        }
    }

    [Serializable]
    public struct IAPProvideData
    {
        public string cfgId;
        public string title;
        public string desc;
        public string formatPrice;
    }
}