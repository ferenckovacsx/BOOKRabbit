package com.example.rabbit;

import android.animation.ValueAnimator;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.ImageView;

import com.example.rabbit.util.IabBroadcastReceiver;
import com.example.rabbit.util.IabHelper;
import com.example.rabbit.util.IabResult;
import com.example.rabbit.util.Inventory;
import com.example.rabbit.util.Purchase;

import java.io.IOException;

import pl.droidsonroids.gif.GifDrawable;

public class MainActivity extends AppCompatActivity {

    static final String TAG = "MainActivity";
    static final String SKU_SUBSCRIBE = "carrot_subscription";
    static final String SKU_CARROT = "carrot_single";

    ImageView rabbitImageView, grassImageView1, grassImageView2;
    Button runButton;
    GifDrawable gifDrawable;
    boolean isRunning = false;
    ValueAnimator animator;

    int carrotCount;

    String subscriptionSku;
    String carrotSku;
    boolean isSubscribed;
    boolean isAutoRenewed = false;
    IabHelper inAppBillingHelper;
    IabBroadcastReceiver inAppBillingBroadcastReceiver;
    String base64EncodedPublicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAn3U1RFdCwsSru9+ASzi/AM7Zxrha34KsJekqLPIAL/JdZR9nS1W4jfIMnZ5G16MEqKW53onO7NF85kk5FBp0ypfiS/rkU58QHs9PlBMTZm6kcV158AFQsc6W/5KC3QZ9t1FMRL/bCQCALM5qed+nwXtPRQN5jmwjTcVMvzcex9fFVDU74/L40FImfDNmh8lWUMtLE57O/ZNgWfs1QywQZC2HacUcE5U6RLUbpVA+hLgpmXG0ZzNLdPs72rAF+SYN9IaYmhVmmqTOc81ir7+o/k4SVt7Uo7WYThii7ZaiLb/foXNFHywBVVXuB0OCKA45P1GZgAJ6ZczRvZ03lNNCfwIDAQAB";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        rabbitImageView = findViewById(R.id.rabbit);
        runButton = findViewById(R.id.btn_run);
        grassImageView1 = findViewById(R.id.grass1);
        grassImageView2 = findViewById(R.id.grass2);

        animator = ValueAnimator.ofFloat(1.0f, 0.0f);

        inAppBillingHelper = new IabHelper(this, base64EncodedPublicKey);
        inAppBillingHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
            public void onIabSetupFinished(IabResult result) {
                Log.d(TAG, "Setup finished.");

                if (!result.isSuccess()) {
                    Log.e(TAG, "In-App Billing setup was unsuccessful: " + result);
                    return;
                }
                if (inAppBillingHelper == null) return;

                //Get inventory
                try {
                    inAppBillingHelper.queryInventoryAsync(mGotInventoryListener);
                } catch (IabHelper.IabAsyncInProgressException e) {
                    Log.e(TAG, "Error querying inventory. Another async operation in progress.");
                }
            }
        });


        try {
            gifDrawable = new GifDrawable(getResources(), R.drawable.rabbit_running);
            rabbitImageView.setImageDrawable(gifDrawable);
            gifDrawable.stop();
        } catch (IOException io) {
            io.printStackTrace();
        }


        runButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleRunning();
            }
        });
    }


    void toggleRunning() {
        if (isRunning) {
            isRunning = false;
            gifDrawable.stop();
            animator.end();

        } else {

            isRunning = true;
            gifDrawable.start();

            //animate grass
            animator.setRepeatCount(ValueAnimator.INFINITE);
            animator.setInterpolator(new LinearInterpolator());
            animator.setDuration(3000L);
            animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    final float progress = (float) animation.getAnimatedValue();
                    final float width = grassImageView1.getWidth();
                    final float translationX = width * progress;
                    grassImageView1.setTranslationX(translationX);
                    grassImageView2.setTranslationX(translationX - width);
                }
            });
            animator.start();

        }
    }

    IabHelper.QueryInventoryFinishedListener mGotInventoryListener = new IabHelper.QueryInventoryFinishedListener() {
        public void onQueryInventoryFinished(IabResult result, Inventory inventory) {
            Log.d(TAG, "Query inventory finished.");

            // Have we been disposed of in the meantime? If so, quit.
            if (inAppBillingHelper == null) return;

            // Is it a failure?
            if (result.isFailure()) {
                Log.e(TAG, "Failed to query inventory: " + result);
                return;
            }

            Log.d(TAG, "Query inventory was successful.");


            // First find out which subscription is auto renewing
            Purchase gasMonthly = inventory.getPurchase(SKU_SUBSCRIBE);
            if (gasMonthly != null && gasMonthly.isAutoRenewing()) {
                subscriptionSku = SKU_SUBSCRIBE;
                isAutoRenewed = true;
            } else {
                subscriptionSku = "";
                isAutoRenewed = false;
            }

            isSubscribed = (gasMonthly != null);
            Log.d(TAG, "User " + (isSubscribed ? "HAS" : "DOES NOT HAVE") + " infinite gas subscription.");

//            if (isSubscribed) mTank = TANK_MAX;

            // Check for gas delivery -- if we own gas, we should fill up the tank immediately
            Purchase gasPurchase = inventory.getPurchase(SKU_CARROT);
            if (gasPurchase != null) {

                carrotCount += 1;

//                try {
//                    carrotCount += 1;
////                    inAppBillingHelper.consumeAsync(inventory.getPurchase(SKU_GAS), mConsumeFinishedListener);
//                } catch (IabHelper.IabAsyncInProgressException e) {
//                    Log.e(TAG, "Error consuming gas. Another async operation in progress.");
//                }
                return;
            }

//            updateUi();
//            setWaitScreen(false);
        }
    };

//    @Override
//    public void receivedBroadcast() {
//        // Received a broadcast notification that the inventory of items has changed
//        Log.d(TAG, "Received broadcast notification. Querying inventory.");
//        try {
//            inAppBillingHelper.queryInventoryAsync(mGotInventoryListener);
//        } catch (IabHelper.IabAsyncInProgressException e) {
//            complain("Error querying inventory. Another async operation in progress.");
//        }
//    }


}
