package com.iap.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;


public class BaseMainActivity {
    public static String UNITY_GO_NAME = "IAPBridge";
    public static final String LOG_TAG = "GameLog";

    protected Handler uiHandler = new Handler(Looper.getMainLooper());

    //unity项目启动时的上下文
    private Activity unityActivity;
    private Context context;

    public void Init(final String goName, final String googlePlayPublicKey) {
        PrintLog("Init：" + goName + "====" + googlePlayPublicKey);
        uiHandler.post(new Runnable() {

            @Override
            public void run() {
                UNITY_GO_NAME = goName;
                OnInitHandle(googlePlayPublicKey);
            }
        });
    }

    public void PrintLog(final String message, final Boolean toast) {
        android.util.Log.d(LOG_TAG, message);
        uiHandler.post(new Runnable() {
            @Override
            public void run() {
                if (toast) Toast.makeText(getActivity(), message, Toast.LENGTH_LONG).show();
            }
        });
    }

    public boolean IsIAPSupported() {
        return false;
    }

    final public void RequestProduct(final String idsJson) {
        uiHandler.post(new Runnable() {

            @Override
            public void run() {
                String[] realProducts = null;
                try {
                    JSONObject jObject = new JSONObject(idsJson);
                    JSONArray jArray = jObject.getJSONArray("productIds");
                    realProducts = new String[jArray.length()];
                    for (int i = 0; i < jArray.length(); i++) {
                        realProducts[i] = jArray.getString(i);
                    }
                } catch (Exception e) {
                    PrintLog("RequestProduct数据传输错误：" + e.getMessage());
                }
                if (realProducts != null) {
                    OnRequestProduct(realProducts);
                } else {
                    RequestProductsFail("数据解析错误：" + idsJson);
                }
            }
        });
    }

    final public void BuyProduct(final String productJson) {
        uiHandler.post(new Runnable() {

            @Override
            public void run() {
                try {
                    JSONObject jObject = new JSONObject(productJson);
                    String productId = jObject.getString("productId");
                    boolean isConsumable = jObject.getBoolean("isConsumable");

                    OnBuyProduct(productId, isConsumable);
                } catch (Exception e) {
                    PrintLog("BuyProduct数据传输错误：" + e.getMessage());
                }
            }
        });
    }

    final public void GetRestoreProductList() {

        OnPurchaseHistory();
    }


    protected void OnInitHandle(String googlePlayPublicKey) {
    }

    protected void OnRequestProduct(String[] productId) {
    }

    protected void OnBuyProduct(String productId, boolean isConsumable) {
    }

    protected void OnPurchaseHistory() {

    }


    protected void BuyComplete(String productId, String originalJson, String signature) {
        PrintLog("java-  购买成功：" + productId);
        PrintLog("java-  IAP originalJson: " + originalJson);
        PrintLog("java-  IAP signature: " + signature);

        try {
            JSONObject jObject = new JSONObject();
            jObject.put("productId", productId);
            jObject.put("originalJson", originalJson);
            jObject.put("signature", signature);
            SendUnityMessage("ProductBuyComplete", jObject.toString());
        } catch (JSONException e) {
            PrintLog("BuyFail数据错误：" + e.getMessage());
        }


    }

    protected void BuyCancel(String productId) {
        PrintLog("java-  购买取消：" + productId);
        SendUnityMessage("ProductBuyCanceled", productId);
    }

    protected void BuyFail(String productId, String error) {
        PrintLog("java-  购买失败：" + productId + "原因：" + error);
        try {
            JSONObject jObject = new JSONObject();
            jObject.put("productId", productId);
            jObject.put("error", error);
            SendUnityMessage("ProductBuyFailed", jObject.toString());
        } catch (JSONException e) {
            PrintLog("BuyFail数据错误：" + e.getMessage());
        }
    }

    //产品列表请求失败
    protected void RequestProductsFail(String message) {
        SendUnityMessage("ProductRequestFail", message);
    }

    protected void SendPurchaseHistory(String message) {
        SendUnityMessage("ReceivePurchaseHistory", message);
    }


    protected void ReceiveProductInfo(ArrayList<SkuItem> skuItems, ArrayList<String> invalidProductIds) {
        JSONObject jsonObject = new JSONObject();
        try {
            JSONArray skuArray = new JSONArray();
            JSONObject tmpObj = null;
            for (int i = 0; i < skuItems.size(); i++) {
                SkuItem skuItem = skuItems.get(i);
                tmpObj = new JSONObject();
                tmpObj.put("productId", skuItem.productId);
                tmpObj.put("title", skuItem.title);
                tmpObj.put("desc", skuItem.desc);
                tmpObj.put("price", skuItem.price);
                tmpObj.put("formatPrice", skuItem.formatPrice);
                tmpObj.put("priceCurrencyCode", skuItem.priceCurrencyCode);
                tmpObj.put("skuType", skuItem.skuType);
                skuArray.put(tmpObj);
            }

            JSONArray invalidArray = new JSONArray();
            for (int i = 0; i < invalidProductIds.size(); i++) {
                invalidArray.put(invalidProductIds.get(i));
            }
            jsonObject.put("skuItems", skuArray);
            jsonObject.put("invalidIds", invalidArray);
        } catch (JSONException e) {
            PrintLog("Json数据错误：" + e.getMessage());
        }
        String info = jsonObject.toString();
        PrintLog("当前产品信息：" + info);
        SendUnityMessage("ReceiveProductInfos", info);
    }

    public void PrintLog(String message) {
        PrintLog(message, false);
    }

    public void SendUnityMessage(String func, String value) {
        CallUnity(UNITY_GO_NAME, func, value);
    }

    public Activity CurrentActivity() {
        return getActivity();
    }

    /**
     * Android调用Unity的方法
     *
     * @param gameObjectName 调用的GameObject的名称
     * @param functionName   方法名
     * @param args           参数
     * @return 调用是否成功
     */
    boolean CallUnity(String gameObjectName, String functionName, String args) {
        try {
            Class<?> classtype = Class.forName("com.unity3d.player.UnityPlayer");
            Method method = classtype.getMethod("UnitySendMessage", String.class, String.class, String.class);
            method.invoke(classtype, gameObjectName, functionName, args);
            return true;
        } catch (ClassNotFoundException e) {
            System.out.println(e.getMessage());
        } catch (NoSuchMethodException e) {
            System.out.println(e.getMessage());
        } catch (IllegalAccessException e) {
            System.out.println(e.getMessage());
        } catch (InvocationTargetException e) {

        }
        return false;
    }

    /**
     * 利用反射机制获取unity项目的上下文
     *
     * @return
     */
    Activity getActivity() {
        if (null == unityActivity) {
            try {
                Class<?> classtype = Class.forName("com.unity3d.player.UnityPlayer");
                Activity activity = (Activity) classtype.getDeclaredField("currentActivity").get(classtype);
                unityActivity = activity;
                context = activity;
            } catch (ClassNotFoundException e) {
                System.out.println(e.getMessage());
            } catch (IllegalAccessException e) {
                System.out.println(e.getMessage());
            } catch (NoSuchFieldException e) {
                System.out.println(e.getMessage());
            }
        }
        return unityActivity;
    }


}
