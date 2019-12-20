package org.godotengine.godot;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.util.Log;
import android.support.annotation.NonNull;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.Purchase;
import com.revenuecat.purchases.EntitlementInfo;
import com.revenuecat.purchases.Offerings;
import com.revenuecat.purchases.PurchaserInfo;
import com.revenuecat.purchases.Purchases;
import com.revenuecat.purchases.PurchasesError;
import com.revenuecat.purchases.interfaces.MakePurchaseListener;
import com.revenuecat.purchases.interfaces.ReceivePurchaserInfoListener;
import com.revenuecat.purchases.interfaces.ReceiveOfferingsListener;

public class GodotRevenueCat extends Godot.SingletonBase {
    private Activity activity = null;
    private int instanceId = 0;
    private Context context;

    private static boolean _running = true;

    private Offerings offerings = null;
    public boolean isDebug;

    static public Godot.SingletonBase initialize(Activity p_activity) {
        return new GodotRevenueCat(p_activity);
    }

    public GodotRevenueCat(Activity p_activity) {
        registerClass("RevenueCat", new String[]{
            "init",
            "purchase_product",
            "restore_transactions",
            "check_subscription_status"
        });

        activity = p_activity;
        context = activity.getApplicationContext();

    }

    public void init(int instanceId, String apiKey, boolean p_isDebug) {
        this.instanceId = instanceId;
        Purchases.setDebugLogsEnabled(p_isDebug);
        isDebug = p_isDebug;
        Purchases.configure(this.context, apiKey, null);
        Purchases.getSharedInstance().getOfferings(new ReceiveOfferingsListener() {
            @Override
            public void onReceived(@NonNull Offerings offerings) {
                setOfferings(offerings);
            }
            
            @Override
            public void onError(@NonNull PurchasesError error) {}
        });
    }

    public void purchase_product(final String product_id, String revenue_cat_offering_id){
        Purchases.getSharedInstance().purchasePackage(
            this.activity,
            this.offerings.get(revenue_cat_offering_id).get(product_id),
            new MakePurchaseListener() {
                @Override
                public void onCompleted(@NonNull Purchase purchase, @NonNull PurchaserInfo purchaserInfo) {
                    Dictionary info = create_info_dict(purchaserInfo.getEntitlements().get("subscription"), product_id);
                    if (purchaserInfo.getEntitlements().get("subscription").isActive()) {
                    // Unlock that great "pro" content
                        if (instanceId != 0){
                            GodotLib.calldeferred(instanceId, "revenuecat_purchase_product_succeeded", new Object[]{product_id, info});
                        }
                    }
                }

                @Override
                public void onError(@NonNull PurchasesError error, boolean userCancelled) {
                    // No purchase
                    if (userCancelled){
                        if (instanceId != 0){
                            GodotLib.calldeferred(instanceId, "revenuecat_purchase_product_failed", new Object[]{product_id, "Purchase cancelled by user"});
                        }
                    }else{
                        if (instanceId != 0){
                            GodotLib.calldeferred(instanceId, "revenuecat_purchase_product_failed", new Object[]{product_id, error.getMessage()});
                        }
                    }
                }
            }
        );
    }

    public void restore_transactions(){
        Purchases.getSharedInstance().restorePurchases(new ReceivePurchaserInfoListener() {
            @Override
            public void onReceived(@NonNull PurchaserInfo purchaserInfo) {
                //... check purchaserInfo to see if entitlement is now active
                if (purchaserInfo.getEntitlements().get("subscription").isActive()){
                    if (instanceId != 0){
                        GodotLib.calldeferred(instanceId, "revenuecat_restore_transactions_succeeded", new Object[]{});
                    }    
                }else{
                    if (instanceId != 0){
                        GodotLib.calldeferred(instanceId, "revenuecat_restore_transactions_failed", new Object[]{"Restore was not successful"});
                    }
                }
            }
            @Override
            public void onError(@NonNull PurchasesError error) {
                if (instanceId != 0){
                    GodotLib.calldeferred(instanceId, "revenuecat_restore_transactions_failed", new Object[]{error.getMessage()});
                }
            }
        });
    }

    public void check_subscription_status(final String subscription_id) {
        Purchases.getSharedInstance().getPurchaserInfo(new ReceivePurchaserInfoListener() {
            @Override
            public void onReceived(@NonNull PurchaserInfo purchaserInfo) {
                // access latest purchaserInfo
                Dictionary info = create_info_dict(purchaserInfo.getEntitlements().get("subscription"), subscription_id);
                if (instanceId != 0){
                    GodotLib.calldeferred(instanceId, "revenuecat_check_subscription_succeeded", new Object[]{info.get("isActive"), info});
                }
            }

            @Override
            public void onError(@NonNull PurchasesError error) {
                if (instanceId != 0){
                    GodotLib.calldeferred(instanceId, "revenuecat_check_subscription_failed", new Object[]{error.getMessage()});
                }
            }
        });
    }

    private Dictionary create_info_dict(EntitlementInfo entitlementInfo, String product_id) {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-mm-dd hh:mm:ss");
        
        Dictionary info = new Dictionary();
        info.put("isActive", entitlementInfo.isActive());
        info.put("productId", product_id);
        info.put("identifier", entitlementInfo.getIdentifier());
        info.put("productIdentifier", entitlementInfo.getProductIdentifier());
        info.put("willRenew", entitlementInfo.getWillRenew());
        info.put("isSandbox", entitlementInfo.isSandbox());
        info.put("periodType", (int)entitlementInfo.getPeriodType().ordinal());
        info.put("latestPurchaseDate", (String)dateFormat.format(entitlementInfo.getLatestPurchaseDate()));
        info.put("originalPurchaseDate", (String)dateFormat.format(entitlementInfo.getOriginalPurchaseDate()));
        info.put("expirationDate", (String)dateFormat.format(entitlementInfo.getExpirationDate()));

        return info;
    }

    public void setOfferings(Offerings offerings){
        this.offerings = offerings;
    }
}