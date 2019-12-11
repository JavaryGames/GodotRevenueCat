package org.godotengine.godot;

import android.app.Activity;
import android.os.Build;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

import com.android.billingclient.api.SkuDetails;
import com.revenuecat.purchases.Entitlement;
import com.revenuecat.purchases.Offering;
import com.revenuecat.purchases.PurchaserInfo;
import com.revenuecat.purchases.Purchases;

public class GodotRevenueCat extends Godot.SingletonBase {
    private Godot activity = null;
    private int instanceId = 0;

    private static boolean _running = true;

    public boolean isDebug;

    static public Godot.SingletonBase initialize(Activity p_activity) {
        return new GodotRevenueCat(p_activity);
    }

    public RevenueCat(Activity activity) {
        registerClass("GodotRevenueCat", new String[]{
            "init",
            "purchase_product",
            "restore_transactions",
            "check_subscription_status"
        });

        activity = (Godot)p_activity;

    }

    public void init(int instanceId, String apiKey, boolean p_isDebug) {
        this.instanceId = instanceId;
        
        Purchases.setDebugLogsEnabled(p_isDebug);
        isDebug = p_isDebug;
        Purchases.configure(this, apiKey, null);
    }

    public void purchase_product(String product_id, String revenue_cat_offering_id){
        Purchases.getSharedInstance().purchasePackage(
            this,
            product_id,
            new MakePurchaseListener() {
                @Override
                public void onCompleted(@NonNull Purchase purchase, @NonNull PurchaserInfo purchaserInfo) {
                    if (purchaserInfo.getEntitlements().get(purchaserInfo.entitlement.identifier).isActive()) {
                    // Unlock that great "pro" content
                        if (instanceID != 0){
                            GodotLib.calldeferred(instanceID, "revenuecat_purchase_product_succeededs", product_id, error.message)
                        }
                    }
                }

                @Override
                public void onError(@NonNull PurchasesError error, Boolean userCancelled) {
                    // No purchase
                    if (userCancelled){
                        if (instanceID != 0){
                            GodotLib.calldeferred(instanceID, "revenuecat_purchase_product_failed", product_id, "Purchase cancelled by user")
                        }
                    }else{
                        if (instanceID != 0){
                            GodotLib.calldeferred(instanceID, "revenuecat_purchase_product_failed", product_id, error.message)
                        }
                    }
                }
            }
        );
    }

    public void restore_transactions(){
        Purchases.getSharedInstance().restorePurchases(new ReceivePurchaserInfoListener() {
            @Override
            public void onReceived(@android.support.annotation.Nullable PurchaserInfo purchaserInfo, @android.support.annotation.Nullable PurchasesError error) {
                //... check purchaserInfo to see if entitlement is now active
                if (purchaserInfo.getEntitlements().get(purchaserInfo.entitlement.identifier).isActive()){
                    if (instanceID != 0){
                        GodotLib.calldeferred(instanceID, "revenuecat_restore_transactions_succeeded")
                    }    
                }else{
                    if (instanceID != 0){
                        GodotLib.calldeferred(instanceID, "revenuecat_restore_transactions_failed", product_id, "Restore was not successful")
                    }
                }
            }
            @Override
            public void onError(@NonNull PurchasesError error, Boolean userCancelled) {
                if (instanceID != 0){
                    GodotLib.calldeferred(instanceID, "revenuecat_restore_transactions_failed", product_id, error.message)
                }
            }
        });
    }

    public void check_subscription_status(String subscription_id) {
        Purchases.getSharedInstance().getPurchaserInfo(new ReceivePurchaserInfoListener() {
            @Override
            public void onReceived(@NonNull PurchaserInfo purchaserInfo) {
                // access latest purchaserInfo
                Dictionary info = create_info_dict(purchaserInfo.entitlementInfo, subscription_id);
                if (instanceID != 0){
                    GodotLib.calldeferred(instanceID, "revenuecat_check_subscription_succeeded", info["isActive"], info)
                }
            }

            @Override
            public void onError(@NonNull PurchasesError error) {
                if (instanceID != 0){
                    GodotLib.calldeferred(instanceID, "revenuecat_check_subscription_failed", product_id, error.message)
                }
            }
        }
    }

    private Dictionary create_info_dict(EntitlementInfo entitlementInfo, String product_id) {
        Dictionary info = new Dictionary();
        info.put("isActive", entitlement.isActive);
        info.put("productId", product_id);
        info.put("identifier", entitlement.identifier);
        info.put("productIdentifier", entitlement.productIdentifier);
        info.put("willRenew", entitlement.willRenew);
        info.put("isSandbox", entitlement.isSandbox);
        info.put("periodType", (int)entitlement.periodType);
        info.put("latestPurchaseDate", (String)entitlement.latestPurchaseDate);
        info.put("originalPurchaseDate", (String)entitlement.originalPurchaseDate);
        info.put("expirationDate", (String)entitlement.expirationDate);

        return info;
    }

}