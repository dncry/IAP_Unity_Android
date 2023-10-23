package com.iap.util;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.AcknowledgePurchaseResponseListener;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ConsumeParams;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.ProductDetailsResponseListener;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchaseHistoryRecord;
import com.android.billingclient.api.PurchaseHistoryResponseListener;
import com.android.billingclient.api.PurchasesResponseListener;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryPurchaseHistoryParams;
import com.android.billingclient.api.QueryPurchasesParams;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;
import com.android.billingclient.api.SkuDetailsResponseListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends BaseMainActivity implements PurchasesUpdatedListener, BillingClientStateListener, PurchasesResponseListener {
    private static final long RECONNECT_TIMER_START_MILLISECONDS = 1L * 1000L;
    private static final long RECONNECT_TIMER_MAX_TIME_MILLISECONDS = 1000L * 60L * 15L; // 15 mins

    private static final Handler handler = new Handler(Looper.getMainLooper());
    private BillingClient billingClient;
    private String[] cacheRequestList;

    private Map<String, ProductDetails> skuDetailsLiveDataMap2 = new HashMap<>();
    private Map<String, SkuDetails> skuDetailsLiveDataMap = new HashMap<>();
    private boolean isConsumable;
    private String buyProductId;
    private boolean billingSetupComplete = false;
    // how long before the data source tries to reconnect to Google play
    private long reconnectMilliseconds = RECONNECT_TIMER_START_MILLISECONDS;

    private boolean isNewStoreVersion = true;

    @Override
    protected void OnInitHandle(String googlePlayPublicKey) {
        super.OnInitHandle(googlePlayPublicKey);

        if (googlePlayPublicKey.contains("CONSTRUCT_YOUR")) {
            throw new RuntimeException("Please put your app's public key in MainActivity.java. See README.");
        }
        billingClient = BillingClient.newBuilder(CurrentActivity()).setListener(this).enablePendingPurchases().build();
        billingClient.startConnection(this);


    }

    private void retryBillingServiceConnectionWithExponentialBackoff() {
        handler.postDelayed(() ->
                        billingClient.startConnection(this),
                reconnectMilliseconds);
        reconnectMilliseconds = Math.min(reconnectMilliseconds * 2,
                RECONNECT_TIMER_MAX_TIME_MILLISECONDS);
    }

    @Override
    protected void OnRequestProduct(String[] productId) {
        super.OnRequestProduct(productId);

        BillingResult billingResult = billingClient.isFeatureSupported(BillingClient.FeatureType.PRODUCT_DETAILS);
        isNewStoreVersion = billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK;

        if (isNewStoreVersion) {

            PrintLog("IAP使用新版Store");

            List<String> skuList = new ArrayList<>();
            skuList.addAll(Arrays.asList(productId));
            cacheRequestList = productId;

            ArrayList<QueryProductDetailsParams.Product> productList = new ArrayList<>();

            for (String sku : skuList) {
                productList.add(
                        QueryProductDetailsParams.Product.newBuilder()
                                .setProductId(sku)
                                .setProductType(BillingClient.ProductType.INAPP)
                                .build()
                );
            }

            QueryProductDetailsParams params = QueryProductDetailsParams.newBuilder()
                    .setProductList(productList)
                    .build();


            billingClient.queryProductDetailsAsync(params, new ProductDetailsResponseListener() {
                @Override
                public void onProductDetailsResponse(@NonNull BillingResult billingResult, @NonNull List<ProductDetails> list) {

                    int responseCode = billingResult.getResponseCode();
                    PrintLog("onSkuDetailsResponse:" + billingResult + " code:" + GetResponseText(responseCode));

                    switch (responseCode) {
                        case BillingClient.BillingResponseCode.OK:
                            ReceiveProducts2(list);
                            break;
                        default:
                            RequestProductsFail("Failed to query inventory: " + billingResult.getDebugMessage());
                            PrintLog("Failed to query inventory: " + billingResult.getDebugMessage());
                            break;
                    }
                }
            });


        } else {

            PrintLog("IAP使用旧版Store");

            List<String> skuList = new ArrayList<>();
            skuList.addAll(Arrays.asList(productId));
            cacheRequestList = productId;

            SkuDetailsParams.Builder params = SkuDetailsParams.newBuilder();
            params.setSkusList(skuList).setType(BillingClient.SkuType.INAPP);
            billingClient.querySkuDetailsAsync(params.build(),
                    new SkuDetailsResponseListener() {
                        @Override
                        public void onSkuDetailsResponse(BillingResult billingResult,
                                                         List<SkuDetails> skuDetailsList) {
                            int responseCode = billingResult.getResponseCode();
                            PrintLog("onSkuDetailsResponse:" + billingResult + " code:" + GetResponseText(responseCode));

                            switch (responseCode) {
                                case BillingClient.BillingResponseCode.OK:
                                    ReceiveProducts(skuDetailsList);
                                    break;
                                default:
                                    RequestProductsFail("Failed to query inventory: " + billingResult.getDebugMessage());
                                    PrintLog("Failed to query inventory: " + billingResult.getDebugMessage());
                                    break;
                            }
                        }
                    });
        }


    }


    private String GetResponseText(int responseCode) {
        switch (responseCode) {
            case BillingClient.BillingResponseCode.OK:
                return "OK";
            case BillingClient.BillingResponseCode.SERVICE_TIMEOUT:
                return "SERVICE_TIMEOUT";
            case BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED:
                return "FEATURE_NOT_SUPPORTED";
            case BillingClient.BillingResponseCode.USER_CANCELED:
                return "USER_CANCELED";
            case BillingClient.BillingResponseCode.SERVICE_DISCONNECTED:
                return "SERVICE_DISCONNECTED";
            case BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE:
                return "SERVICE_UNAVAILABLE";
            case BillingClient.BillingResponseCode.BILLING_UNAVAILABLE:
                return "BILLING_UNAVAILABLE";
            case BillingClient.BillingResponseCode.ITEM_UNAVAILABLE:
                return "ITEM_UNAVAILABLE";
            case BillingClient.BillingResponseCode.DEVELOPER_ERROR:
                return "DEVELOPER_ERROR";
            case BillingClient.BillingResponseCode.ERROR:
                return "ERROR";
            case BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED:
                return "ITEM_ALREADY_OWNED";
            case BillingClient.BillingResponseCode.ITEM_NOT_OWNED:
                return "ITEM_NOT_OWNED";
            default:
                return "UnKnown";
        }
    }

    private void ReceiveProducts2(List<ProductDetails> skuDetailsList) {
        ArrayList<SkuItem> skuItems = new ArrayList<SkuItem>();
        ArrayList<String> invaildIds = new ArrayList<String>();
        PrintLog("cacheRequestList:" + cacheRequestList);
        int length = cacheRequestList.length;
        if (cacheRequestList != null && length > 0) {
            for (int i = 0; i < length; i++) {
                String productId = cacheRequestList[i];
                if (!TextUtils.isEmpty(productId)) {
                    ProductDetails detail = null;
                    for (ProductDetails skuDetails : skuDetailsList) {
                        if (skuDetails.getProductId().equals(productId)) {
                            detail = skuDetails;
                            break;
                        }
                    }

                    if (detail == null) {
                        PrintLog("未找到该产品信息:" + productId);
                        invaildIds.add(productId);
                        continue;
                    }
                    skuDetailsLiveDataMap2.put(productId, detail);

                    //String price = detail.getOneTimePurchaseOfferDetails().getFormattedPrice();

                    double price = ((double)( detail.getOneTimePurchaseOfferDetails().getPriceAmountMicros()))/1000000 ;
                    String formatPrice = detail.getOneTimePurchaseOfferDetails().getFormattedPrice();
                    //String formatPrice = price;

                    SkuItem skuItem = new SkuItem();
                    skuItem.productId = productId;
                    skuItem.title = detail.getTitle();
                    skuItem.desc = detail.getDescription();
                    skuItem.price = price;
                    skuItem.formatPrice = formatPrice;
                    skuItem.priceCurrencyCode = detail.getOneTimePurchaseOfferDetails().getPriceCurrencyCode();
                    skuItem.skuType = detail.getProductType();

                    skuItems.add(skuItem);
                }
            }
        }
        ReceiveProductInfo(skuItems, invaildIds);
    }


    private void ReceiveProducts(List<SkuDetails> skuDetailsList) {
        ArrayList<SkuItem> skuItems = new ArrayList<SkuItem>();
        ArrayList<String> invaildIds = new ArrayList<String>();
        PrintLog("cacheRequestList:" + cacheRequestList);
        int length = cacheRequestList.length;
        if (cacheRequestList != null && length > 0) {
            for (int i = 0; i < length; i++) {
                String productId = cacheRequestList[i];
                if (!TextUtils.isEmpty(productId)) {
                    SkuDetails detail = null;
                    for (SkuDetails skuDetails : skuDetailsList) {
                        if (skuDetails.getSku().equals(productId)) {
                            detail = skuDetails;
                            break;
                        }
                    }

                    if (detail == null) {
                        PrintLog("未找到该产品信息:" + productId);
                        invaildIds.add(productId);
                        continue;
                    }
                    skuDetailsLiveDataMap.put(productId, detail);

                    double price =((double)(detail.getPriceAmountMicros()))/ 1000000 ;    ;
                    String formatPrice =  detail.getOriginalPrice();

                    SkuItem skuItem = new SkuItem();
                    skuItem.productId = productId;
                    skuItem.title = detail.getTitle();
                    skuItem.desc = detail.getDescription();
                    skuItem.price = price;
                    skuItem.formatPrice = formatPrice;
                    skuItem.priceCurrencyCode = detail.getPriceCurrencyCode();
                    skuItem.skuType = detail.getType();

                    skuItems.add(skuItem);
                }
            }
        }
        ReceiveProductInfo(skuItems, invaildIds);
    }


    @Override
    public boolean IsIAPSupported() {
        if (billingClient == null) {
            return false;
        }
        return billingClient.isReady();
    }

    @Override
    protected void OnBuyProduct(String productId, boolean isConsumable) {
        super.OnBuyProduct(productId, isConsumable);

        if (isNewStoreVersion) {

            ProductDetails skuDetails = skuDetailsLiveDataMap2.get(productId);
            if (null != skuDetails) {
                buyProductId = productId;
                this.isConsumable = isConsumable;

                ArrayList<BillingFlowParams.ProductDetailsParams> productParamsArrayList = new ArrayList<>();

                productParamsArrayList.add(BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(skuDetails)
                        .build());

                BillingFlowParams purchaseParams =
                        BillingFlowParams.newBuilder()
                                .setProductDetailsParamsList(productParamsArrayList)
                                .build();

                billingClient.launchBillingFlow(CurrentActivity(), purchaseParams);
            } else {
                BuyFail(productId, "Can not find SkuDetails:" + productId);
                PrintLog("未请求商品数据，请先请求:" + productId);
            }

        } else {

            SkuDetails skuDetails = skuDetailsLiveDataMap.get(productId);
            if (null != skuDetails) {
                buyProductId = productId;
                this.isConsumable = isConsumable;

                BillingFlowParams purchaseParams =
                        BillingFlowParams.newBuilder()
                                .setSkuDetails(skuDetails)
                                .build();

                billingClient.launchBillingFlow(CurrentActivity(), purchaseParams);
            } else {
                BuyFail(productId, "Can not find SkuDetails:" + productId);
                PrintLog("未请求商品数据，请先请求:" + productId);
            }

        }


    }

    protected void OnPurchaseHistory() {

//        QueryPurchaseHistoryParams.Builder builder = QueryPurchaseHistoryParams.newBuilder();
//        builder.setProductType("inapp");
//
//        billingClient.queryPurchaseHistoryAsync(
//                builder.build(), this
//        );

        QueryPurchasesParams.Builder builder = QueryPurchasesParams.newBuilder();
        builder.setProductType("inapp");

        billingClient.queryPurchasesAsync(
                builder.build(), this
        );

    }


    @Override
    public void onPurchasesUpdated(@NonNull BillingResult billingResult, @Nullable List<Purchase> list) {
        int responseCode = billingResult.getResponseCode();

        PrintLog("BillingResult [" + GetResponseText(responseCode) + "]: "
                + billingResult.getDebugMessage());
        switch (responseCode) {
            case BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED:

            case BillingClient.BillingResponseCode.OK:
                FlowFinish(true, null, list);
                break;
            case BillingClient.BillingResponseCode.USER_CANCELED:
                String productId = buyProductId;
                buyProductId = null;
                BuyCancel(productId);
                break;
            default:
                FlowFinish(false, billingResult.getDebugMessage(), list);
                break;
        }
    }


    @Override
    public void onQueryPurchasesResponse(@NonNull BillingResult billingResult, @NonNull List<Purchase> list) {
        PrintLog("IAP购买历史记录");

        String str = "";

        if (list != null && list.size() != 0) {
            for (int i = 0; i < list.size(); i++) {

                List<String> list2 = list.get(i).getProducts();

                for (int j = 0; j < list2.size(); j++) {


                    if (!(i == 0 && j == 0)) {
                        str += "+";
                    }
                    str += (list2.get(j).toString());

                }
            }
        }


        PrintLog("IAP购买历史记录:" + str);

        SendPurchaseHistory(str);
    }


    private void FlowFinish(Boolean isSuccess, String message, List<Purchase> purchases) {

        if (isSuccess) {

            if (purchases==null  || purchases.size() == 0){
                CallBackBuyFail(message);
                return;
            }


            if (buyProductId != null) {
                String productId = buyProductId;
                buyProductId = null;
                String purchaseToken = null;

                String originalJson = null;
                String signature = null;

                if (isNewStoreVersion) {
                    for (Purchase purchase : purchases) {

                        if (purchase.getProducts()==null  || purchase.getProducts().size() == 0){
                            CallBackBuyFail(message);
                            return;
                        }

                        for (String skus : purchase.getProducts()) {
                            //需要校验付款状态
                            if (skus.contains(productId) &&
                                    purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                                purchaseToken = purchase.getPurchaseToken();
                                originalJson = purchase.getOriginalJson();
                                signature = purchase.getSignature();

                                break;
                            }
                            if (purchaseToken != null) break;
                        }
                    }
                }
                else {

                    for (Purchase purchase : purchases) {

                        if (purchase.getSkus()==null  || purchase.getSkus().size() == 0){
                            CallBackBuyFail(message);
                            return;
                        }

                        for (String skus : purchase.getSkus()) {
                            //需要校验付款状态
                            if (skus.contains(productId) &&
                                    purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                                purchaseToken = purchase.getPurchaseToken();
                                originalJson = purchase.getOriginalJson();
                                signature = purchase.getSignature();

                                break;
                            }
                            if (purchaseToken != null) break;
                        }
                    }


                }


                if (isConsumable) {
                    if (purchaseToken == null) {
                        CallBackBuyFail("unknown purchaseToken:" + productId);
                    } else {
                        ConsumeParams consumeParams =
                                ConsumeParams.newBuilder()
                                        .setPurchaseToken(purchaseToken)
                                        .build();

                        String finalOriginalJson = originalJson;
                        String finalSignature = signature;

                        billingClient.consumeAsync(consumeParams, (billingResult, token) -> {
                            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                                BuyComplete(productId, finalOriginalJson, finalSignature);
                            } else {
                                CallBackBuyFail(billingResult.getDebugMessage());
                            }
                        });

                    }
                } else {

                    if (purchaseToken == null) {
                        CallBackBuyFail("unknown purchaseToken:" + productId);
                    } else {

                        AcknowledgePurchaseParams acknowledgePurchaseParams =
                                AcknowledgePurchaseParams.newBuilder()
                                        .setPurchaseToken(purchaseToken)
                                        .build();

                        String finalOriginalJson = originalJson;
                        String finalSignature = signature;

                        AcknowledgePurchaseResponseListener acknowledgePurchaseResponseListener = new AcknowledgePurchaseResponseListener() {
                            @Override
                            public void onAcknowledgePurchaseResponse(@NonNull BillingResult billingResult) {
                                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                                    BuyComplete(productId, finalOriginalJson, finalSignature);
                                } else {
                                    CallBackBuyFail(billingResult.getDebugMessage());
                                }
                            }
                        };
                        billingClient.acknowledgePurchase(acknowledgePurchaseParams, acknowledgePurchaseResponseListener);
                    }

                }
            }
        } else {
            if (buyProductId != null) {
                CallBackBuyFail(message);
            }
        }
    }

    private void CallBackBuyFail(String message) {
        String productId = buyProductId;
        buyProductId = null;
        BuyFail(productId, message);
        PrintLog("Error purchasing: " + message);
    }

    @Override
    public void onBillingServiceDisconnected() {
        PrintLog("onBillingServiceDisconnected");
        billingSetupComplete = false;
        retryBillingServiceConnectionWithExponentialBackoff();
    }

    @Override
    public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
        int responseCode = billingResult.getResponseCode();
        String debugMessage = billingResult.getDebugMessage();
        PrintLog("onBillingSetupFinished: " + debugMessage + "(" + GetResponseText(responseCode) + ")", false);
        switch (responseCode) {
            case BillingClient.BillingResponseCode.OK:
                // The billing client is ready. You can query purchases here.
                // This doesn't mean that your app is set up correctly in the console -- it just
                // means that you have a connection to the Billing service.
                reconnectMilliseconds = RECONNECT_TIMER_START_MILLISECONDS;
                billingSetupComplete = true;
                break;
            case BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE:
            case BillingClient.BillingResponseCode.BILLING_UNAVAILABLE:
                PrintLog("Billing Service Unavailable:" + debugMessage, false);
                break;
            default:
                retryBillingServiceConnectionWithExponentialBackoff();
                break;
        }
    }


}
