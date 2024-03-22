package com.ads.control.event;

import android.app.Application;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.ads.control.billing.AppPurchase;
import com.ads.control.funtion.AdType;
import com.applovin.mediation.MaxAd;
import com.applovin.sdk.AppLovinSdk;
import com.appsflyer.AFInAppEventParameterName;
import com.appsflyer.AFInAppEventType;
import com.appsflyer.AppsFlyerLib;
import com.appsflyer.adrevenue.AppsFlyerAdRevenue;
import com.appsflyer.adrevenue.adnetworks.generic.MediationNetwork;
import com.appsflyer.adrevenue.adnetworks.generic.Scheme;
import com.appsflyer.api.PurchaseClient;
import com.appsflyer.api.Store;
import com.appsflyer.attribution.AppsFlyerRequestListener;
import com.appsflyer.internal.models.InAppPurchaseValidationResult;
import com.appsflyer.internal.models.ProductPurchase;
import com.appsflyer.internal.models.SubscriptionPurchase;
import com.appsflyer.internal.models.SubscriptionValidationResult;
import com.appsflyer.internal.models.ValidationFailureData;
import com.google.android.gms.ads.AdValue;

import java.util.Currency;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class RKAppsflyer {
    private static final String TAG = "Appsflyer";
    private Context context;
    private static RKAppsflyer rkAppsflyer;
    public static boolean enableAppsflyer = false;

    public RKAppsflyer() {
    }

    public static RKAppsflyer getInstance(){
        if (rkAppsflyer ==null)
            rkAppsflyer = new RKAppsflyer();
        return rkAppsflyer;
    }

    public void init(Application context, String devKey) {
        init(context, devKey, false);
    }

    public void init(Application context, String devKey, boolean enableDebugLog) {
        this.context = context;
        AppsFlyerLib.getInstance().init(devKey, null, context);
        AppsFlyerLib.getInstance().start(context);

        AppsFlyerAdRevenue.Builder afRevenueBuilder = new AppsFlyerAdRevenue.Builder(context);
        AppsFlyerAdRevenue.initialize(afRevenueBuilder.build());

        AppsFlyerLib.getInstance().setDebugLog(enableDebugLog);

        startCheckPurchase(context);
    }

    public void onTrackEventAddToCard(String contentId){
        Map<String, Object> eventValues = new HashMap<String, Object>();
        eventValues.put(AFInAppEventParameterName.CONTENT_ID, contentId);
        AppsFlyerLib.getInstance().logEvent(context,
                AFInAppEventType.ADD_TO_CART, eventValues, new AppsFlyerRequestListener() {
                    @Override
                    public void onSuccess() {
                        Log.i(TAG, "onTrackEventAddToCard contentId:" + contentId + " success ");
                    }

                    @Override
                    public void onError(int i, @NonNull String s) {
                        Log.i(TAG, "onTrackEventAddToCard contentId:" + contentId + " error: " + s);
                    }
                });
    }

    public void onTrackingAfAdRevenue(float price, String currency, String contentId,String contentType) {
        Map<String, Object> eventValues = new HashMap<String, Object>();
        eventValues.put(AFInAppEventParameterName.PRICE, price);
        eventValues.put(AFInAppEventParameterName.CONTENT_ID, contentId);
        eventValues.put(AFInAppEventParameterName.CURRENCY, currency);
        eventValues.put(AFInAppEventParameterName.CONTENT_TYPE, contentType);
        AppsFlyerLib.getInstance().logEvent(context,
                "af_ad_revenue", eventValues);
    }

    void onTrackRevenuePurchase(float price, String currency, String contentId, int typeIAP) {
        String type = "";
        if (typeIAP == AppPurchase.TYPE_IAP.PURCHASE)
            type = "inapp";
        else
            type = "af_purchase";
        Map<String, Object> eventValues = new HashMap<String, Object>();
        eventValues.put(AFInAppEventParameterName.REVENUE, price);
        eventValues.put(AFInAppEventParameterName.CONTENT_ID, contentId);
        eventValues.put(AFInAppEventParameterName.CURRENCY, currency);
        eventValues.put(AFInAppEventParameterName.CONTENT_TYPE, type);
        eventValues.put("renewal", true);
        AppsFlyerLib.getInstance().logEvent(context, AFInAppEventType.SUBSCRIBE, eventValues);
    }


    public void pushTrackEventAdmob(AdValue adValue, String idAd, AdType adType) {
        Log.i(TAG, "pushTrackEventAdmob  enableAppsflyer:"+enableAppsflyer+ " --- value: "+adValue.getValueMicros() / 1000000.0 + " -- adType: " +adType.toString());
        if (enableAppsflyer) {
            Map<String, String> customParams = new HashMap<>();
            customParams.put(Scheme.AD_UNIT, idAd);
            customParams.put(Scheme.AD_TYPE, adType.toString());

            AppsFlyerAdRevenue.logAdRevenue(
                    "Admob",
                    MediationNetwork.googleadmob,
                    Currency.getInstance(Locale.US),
                    adValue.getValueMicros() / 1000000.0,
                    customParams
            );
        }
    }

    public void pushTrackEventApplovin(MaxAd ad,   AdType adType) {
        Log.i(TAG, "pushTrackEventApplovin  enableAppsflyer:"+enableAppsflyer+ " --- value: "+ad.getRevenue()  + " -- adType: " +adType.toString());
        if (enableAppsflyer) {
            Map<String, String> customParams = new HashMap<>();
            customParams.put(Scheme.AD_UNIT, ad.getAdUnitId());
            customParams.put(Scheme.AD_TYPE, adType.toString());
            customParams.put(Scheme.COUNTRY, AppLovinSdk.getInstance(context).getConfiguration().getCountryCode());
            customParams.put(Scheme.PLACEMENT, ad.getPlacement());
            AppsFlyerAdRevenue.logAdRevenue(
                    "Max",
                    MediationNetwork.applovinmax,
                    Currency.getInstance(Locale.US),
                    ad.getRevenue() / 1000000.0,
                    customParams
            );
        }
    }


    public void startCheckPurchase(Application application) {

        PurchaseClient afPurchaseClient = new PurchaseClient.Builder(application, Store.GOOGLE)
                // Enable Subscriptions auto logging
                .logSubscriptions(true)
                // Enable In Apps auto logging
                .autoLogInApps(true)
                // set production environment
                .setSandbox(false)
                // Subscription Purchase Event Data source listener. Invoked before sending data to AppsFlyer servers
                // to let customer add extra parameters to the payload
                .setSubscriptionPurchaseEventDataSource(purchaseEvents -> {
                    Map<String, Object> map = new HashMap<String, Object>();
                    map.put("af_purchase", "value");
                    map.put("type", "Subscription");
                    return map;
                })
                // In Apps Purchase Event Data source listener. Invoked before sending data to AppsFlyer servers
                // to let customer add extra parameters to the payload
                .setInAppPurchaseEventDataSource(purchaseEvents -> {
                    Map<String, Object> map = new HashMap<String, Object>();
                    map.put("af_purchase", "value");
                    map.put("type", "InApps");
                    return map;
                })
                // Subscriptions Purchase Validation listener. Invoked after getting response from AppsFlyer servers
                // to let customer know if purchase was validated successfully
                .setSubscriptionValidationResultListener(new PurchaseClient.SubscriptionPurchaseValidationResultListener() {
                    @RequiresApi(api = Build.VERSION_CODES.N)
                    @Override
                    public void onResponse(@Nullable Map<String, ? extends SubscriptionValidationResult> result) {
                        if (result == null) {
                            return;
                        }
                        result.forEach((k, v) -> {
                            if (v.getSuccess()) {
                                Log.d(TAG, "[PurchaseConnector]: Subscription with ID " + k + " was validated successfully");
                                SubscriptionPurchase subscriptionPurchase = v.getSubscriptionPurchase();
                                Log.d(TAG, subscriptionPurchase.toString());
                            } else {
                                Log.d(TAG, "[PurchaseConnector]: Subscription with ID " + k + " wasn't validated successfully");
                                ValidationFailureData failureData = v.getFailureData();
                                Log.d(TAG, failureData.toString());
                            }
                        });
                    }

                    @Override
                    public void onFailure(@NonNull String result, @Nullable Throwable error) {
                        Log.d(TAG, "[PurchaseConnector]: Validation fail: " + result);
                        if (error != null) {
                            error.printStackTrace();
                        }
                    }
                })
                // In Apps Purchase Validation listener. Invoked after getting response from AppsFlyer servers
                // to let customer know if purchase was validated successfully
                .setInAppValidationResultListener(new PurchaseClient.InAppPurchaseValidationResultListener() {
                    @RequiresApi(api = Build.VERSION_CODES.N)
                    @Override
                    public void onResponse(@Nullable Map<String, ? extends InAppPurchaseValidationResult> result) {
                        if (result == null) {
                            return;
                        }
                        result.forEach((k, v) -> {
                            if (v.getSuccess()) {
                                Log.d(TAG, "[PurchaseConnector]: Product with Purchase Token " + k + " was validated successfully");
                                ProductPurchase productPurchase = v.getProductPurchase();
                                Log.d(TAG, productPurchase.toString());
                            } else {
                                Log.d(TAG, "[PurchaseConnector]: Subscription with Purchase Token " + k + " wasn't validated successfully");
                                ValidationFailureData failureData = v.getFailureData();
                                Log.d(TAG, failureData.toString());
                            }
                        });
                    }

                    @Override
                    public void onFailure(@NonNull String result, @Nullable Throwable error) {
                        Log.d(TAG, "[PurchaseConnector]: Validation fail: " + result);
                        if (error != null) {
                            error.printStackTrace();
                        }
                    }
                })
                // Build the client
                .build();

        // Start the SDK instance to observe transactions.
        afPurchaseClient.startObservingTransactions();
    }


    public void updateServerUninstallToken(Application context, String token ) {
        AppsFlyerLib.getInstance().updateServerUninstallToken(context, token);
    }
    public void onTrackRevenue(Context context, String eventName, float revenue, String currency) {

    }

    public void onTrackRevenuePurchase(float revenue, String currency) {

    }
}
