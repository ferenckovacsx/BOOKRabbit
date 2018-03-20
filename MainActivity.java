package com.ferenckovacsx.rabbit;

import android.animation.ValueAnimator;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.ferenckovacsx.rabbit.util.IabHelper;
import com.ferenckovacsx.rabbit.util.IabResult;
import com.ferenckovacsx.rabbit.util.Inventory;
import com.ferenckovacsx.rabbit.util.Purchase;

import java.io.IOException;

import pl.droidsonroids.gif.GifDrawable;

public class MainActivity extends AppCompatActivity {

    static final String TAG = "MainActivity";
    static final String SKU_SUBSCRIBE = "carrot_subscription";
    static final String SKU_CARROT = "carrot";
    static final String base64EncodedPublicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAn3U1RFdCwsSru9+ASzi/AM7Zxrha34KsJekqLPIAL/JdZR9nS1W4jfIMnZ5G16MEqKW53onO7NF85kk5FBp0ypfiS/rkU58QHs9PlBMTZm6kcV158AFQsc6W/5KC3QZ9t1FMRL/bCQCALM5qed+nwXtPRQN5jmwjTcVMvzcex9fFVDU74/L40FImfDNmh8lWUMtLE57O/ZNgWfs1QywQZC2HacUcE5U6RLUbpVA+hLgpmXG0ZzNLdPs72rAF+SYN9IaYmhVmmqTOc81ir7+o/k4SVt7Uo7WYThii7ZaiLb/foXNFHywBVVXuB0OCKA45P1GZgAJ6ZczRvZ03lNNCfwIDAQAB";

    ImageView rabbitImageView, grassImageView1, grassImageView2, carrotImageView, pebbleImageView;
    TextView carrotCountTextView, subscriptionStatusTextView, instructionsTextView;
    GifDrawable rabbitAnimation;
    ValueAnimator grassAnimation;

    int carrotCount;
    boolean firstRun;
    boolean isRunning = false;
    boolean isSubscribed;
    IabHelper inAppBillingHelper;
    SharedPreferences carrotPreferences;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //bind UI elements
        rabbitImageView = findViewById(R.id.rabbit);
        grassImageView1 = findViewById(R.id.grass1);
        grassImageView2 = findViewById(R.id.grass2);
        carrotImageView = findViewById(R.id.carrot_imageview);
        carrotCountTextView = findViewById(R.id.carrot_counter_textview);
        subscriptionStatusTextView = findViewById(R.id.subscription_status_textview);
        instructionsTextView = findViewById(R.id.instructionsTextView);
        pebbleImageView = findViewById(R.id.pebble_imageview);


        //update counter, show/hide instructions
        carrotPreferences = getSharedPreferences("preferences", Context.MODE_PRIVATE);
        carrotCount = carrotPreferences.getInt("carrotCount", 1);
        firstRun = carrotPreferences.getBoolean("isFirstRun", true);
        carrotCountTextView.setText(String.valueOf(carrotCount));
        if (firstRun) instructionsTextView.setVisibility(View.VISIBLE);

        //init billing and animations
        setupInAppBilling();
        initiateRabbitAnimation();
        initiateGrassAnimation();

        //click listeners
        rabbitImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleRunningAnimation();
            }

        });
        carrotImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {if (!isSubscribed) startPurchaseFlow(SKU_CARROT);}
        });
        subscriptionStatusTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {if (!isSubscribed) startPurchaseFlow(SKU_SUBSCRIBE);}
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        Log.d(TAG, "onActivityResult(" + requestCode + "," + resultCode + "," + data);
        if (!inAppBillingHelper.handleActivityResult(requestCode, resultCode, data)) {
            super.onActivityResult(requestCode, resultCode, data);
        } else {
            Log.d(TAG, "onActivityResult handled by IABUtil.");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        //save number of carrots even if user leaves application
        SharedPreferences.Editor editor = carrotPreferences.edit();
        editor.putInt("carrotCount", carrotCount);
        editor.putBoolean("isFirstRun", false);
        editor.apply();
    }

    void toggleRunningAnimation() {

        instructionsTextView.setVisibility(View.GONE);

        //If user is subscribed toggle run animation
        if (isSubscribed) {
            if (isRunning) {
                grassAnimation.end();
                rabbitAnimation.stop();
                isRunning = false;
            } else {
                rabbitAnimation.start();
                grassAnimation.start();
                isRunning = true;
            }
        } else {
            //If user is not subscribed and has no carrots, ask him/her to buy a carrot or subscribe
            if (carrotCount < 1) {
                final Dialog dialog = new Dialog(MainActivity.this);
                dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
                dialog.setContentView(R.layout.custom_dialog);
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

                TextView subscribeButton = dialog.findViewById(R.id.dialog_carrot_bunch_imageview);
                TextView buyCarrotButton = dialog.findViewById(R.id.dialog_single_carrot_imageview);

                buyCarrotButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog.dismiss();
                        startPurchaseFlow(SKU_CARROT);
                    }
                });

                subscribeButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog.dismiss();
                        startPurchaseFlow(SKU_SUBSCRIBE);
                    }
                });

                dialog.show();

            } else {

                //If user has carrots, consume one carrot/run
                carrotCount -= 1;
                carrotCountTextView.setText(String.valueOf(carrotCount));

                //Start running. Animate grass and rabbit.
                rabbitAnimation.start();
                grassAnimation.start();
                pebbleImageView.setVisibility(View.GONE);

                //Run for 3 seconds, then stop.
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        grassAnimation.end();
                        rabbitAnimation.stop();
                        pebbleImageView.setVisibility(View.VISIBLE);
                    }
                }, 3000);
            }
        }
    }

    void initiateRabbitAnimation() {
        try {
            rabbitAnimation = new GifDrawable(getResources(), R.drawable.rabbit_running);
            rabbitImageView.setImageDrawable(rabbitAnimation);
            rabbitAnimation.stop();
        } catch (IOException io) {
            io.printStackTrace();
        }
    }

    void initiateGrassAnimation() {
        grassAnimation = ValueAnimator.ofFloat(1.0f, 0.0f);
        grassAnimation.setRepeatCount(ValueAnimator.INFINITE);
        grassAnimation.setInterpolator(new LinearInterpolator());
        grassAnimation.setDuration(3000L);
        grassAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                final float progress = (float) animation.getAnimatedValue();
                final float width = grassImageView1.getWidth();
                final float translationX = width * progress;
                grassImageView1.setTranslationX(translationX);
                grassImageView2.setTranslationX(translationX - width);
            }
        });
    }

    void setupInAppBilling() {

        //set up in app billing and get inventory
        inAppBillingHelper = new IabHelper(this, base64EncodedPublicKey);
        inAppBillingHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
            public void onIabSetupFinished(IabResult result) {
                Log.d(TAG, "Setup finished.");
                if (!result.isSuccess()) {
                    Log.e(TAG, "In-App Billing setup was unsuccessful: " + result);
                    return;
                }
                if (inAppBillingHelper == null) return;
                try {
                    inAppBillingHelper.queryInventoryAsync(inventoryFinishedListener);
                } catch (IabHelper.IabAsyncInProgressException e) {
                    Log.e(TAG, "Error querying inventory. Another async operation in progress.");
                }
            }
        });
    }

    void startPurchaseFlow(String purchaseType) {

        try {
            if (purchaseType.equals(SKU_SUBSCRIBE)) {
                inAppBillingHelper.launchSubscriptionPurchaseFlow(MainActivity.this, SKU_SUBSCRIBE, 1, purchaseFinishedListener);
            } else {
                inAppBillingHelper.launchPurchaseFlow(MainActivity.this, SKU_CARROT, 1, purchaseFinishedListener);
            }
        } catch (IabHelper.IabAsyncInProgressException e) {
            Log.e(TAG, "Error launching purchase flow. Another async operation in progress.");
            Toast.makeText(MainActivity.this, "Error launching process: " + e.toString(), Toast.LENGTH_LONG).show();
        }
    }

    //inventory finished listener
    IabHelper.QueryInventoryFinishedListener inventoryFinishedListener = new IabHelper.QueryInventoryFinishedListener() {
        public void onQueryInventoryFinished(IabResult result, Inventory inventory) {
            Log.d(TAG, "Query inventory finished.");

            if (inAppBillingHelper == null) return;

            if (result.isFailure()) {
                Log.e(TAG, "Failed to query inventory: " + result);
                return;
            }

            Log.d(TAG, "Query inventory was successful.");

            //Find out if user has a subscription
            Purchase carrotSubscription = inventory.getPurchase(SKU_SUBSCRIBE);
            if (carrotSubscription != null)
                isSubscribed = carrotSubscription.isAutoRenewing();

            Log.d(TAG, "User " + (isSubscribed ? "HAS" : "DOES NOT HAVE") + " subscription.");

            if (isSubscribed) {
                carrotImageView.setImageDrawable(getResources().getDrawable(R.drawable.ic_carrot_bunch));
                carrotCountTextView.setText("∞");
                pebbleImageView.setVisibility(View.GONE);
                subscriptionStatusTextView.setText(getResources().getString(R.string.you_are_subscribed));
            } else {
                carrotImageView.setImageDrawable(getResources().getDrawable(R.drawable.ic_carrot_single));
                carrotCountTextView.setText(String.valueOf(carrotCount));
                subscriptionStatusTextView.setSelected(true);
                subscriptionStatusTextView.setText(getResources().getString(R.string.tap_here_to_subscribe));
            }

            //Find out if user has unconsumed carrots. If yes, consume.
            Purchase singleCarrot = inventory.getPurchase(SKU_CARROT);
            if (singleCarrot != null) {

                try {
                    inAppBillingHelper.consumeAsync(inventory.getPurchase(SKU_CARROT), consumeFinishedListener);
                } catch (IabHelper.IabAsyncInProgressException e) {
                    Log.e(TAG, "Error consuming carrot. Another async operation in progress.");
                }
            }

        }
    };

    //purchase finished listener
    IabHelper.OnIabPurchaseFinishedListener purchaseFinishedListener = new IabHelper.OnIabPurchaseFinishedListener() {
        public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
            Log.d(TAG, "Purchase finished: " + result + ", purchase: " + purchase);
            if (inAppBillingHelper == null) return;

            if (result.isFailure()) {
                Log.e(TAG, "Error purchasing: " + result);
                return;
            }

            //If user has purchased a carrot, consume this carrot immediately.
            if (purchase.getSku().equals(SKU_CARROT)) {
                try {
                    inAppBillingHelper.consumeAsync(purchase, consumeFinishedListener);
                } catch (IabHelper.IabAsyncInProgressException e) {
                    Toast.makeText(MainActivity.this, "Purchase was unsuccessful. Error code: " + e, Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Error purchasing. Another async operation in progress.");
                }

            } else if (purchase.getSku().equals(SKU_SUBSCRIBE)) {
                Log.d(TAG, "Subscription purchased.");
                Toast.makeText(MainActivity.this, "Your are now subscribed. Thank you!", Toast.LENGTH_LONG).show();

                isSubscribed = true;
                carrotImageView.setImageDrawable(getResources().getDrawable(R.drawable.ic_carrot_bunch));
                carrotCountTextView.setText("∞");
                subscriptionStatusTextView.setText(getResources().getString(R.string.you_are_subscribed));
                pebbleImageView.setVisibility(View.GONE);
            }
        }
    };


    //carrot consumed listener
    IabHelper.OnConsumeFinishedListener consumeFinishedListener = new IabHelper.OnConsumeFinishedListener() {
        public void onConsumeFinished(Purchase purchase, IabResult result) {
            Log.d(TAG, "Consumption finished. Purchase: " + purchase + ", result: " + result);

            if (inAppBillingHelper == null) return;

            //if carrot was consumed, increment carrot counter and update carrotCountTextView
            if (result.isSuccess()) {

                carrotCount += 1;
                Log.i(TAG, "carrot count: " + carrotCount);
                Toast.makeText(MainActivity.this, "You bought a carrot. Thank you!", Toast.LENGTH_SHORT).show();
                carrotCountTextView.setText(String.valueOf(carrotCount));

            } else {
                Log.e(TAG, "Error while consuming: " + result);
            }
            Log.d(TAG, "End consumption flow.");
        }
    };
}
