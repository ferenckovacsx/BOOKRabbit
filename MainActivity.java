package com.example.rabbit;

import android.animation.ValueAnimator;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.ImageView;

import java.io.FileNotFoundException;
import java.io.IOException;

import pl.droidsonroids.gif.GifDrawable;

public class MainActivity extends AppCompatActivity {

    ImageView rabbitImageView, grassImageView1, grassImageView2;
    Button runButton;
    GifDrawable gifDrawable;
    Boolean isRunning = false;

    ValueAnimator animator;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        rabbitImageView = findViewById(R.id.rabbit);
        runButton = findViewById(R.id.btn_run);
        grassImageView1 = findViewById(R.id.grass1);
        grassImageView2 = findViewById(R.id.grass2);

        animator = ValueAnimator.ofFloat(1.0f, 0.0f);


//        Animation marquee = AnimationUtils.loadAnimation(this, R.anim.animation_grass);
//        grassImageView1.setAnimation(marquee);






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
}
