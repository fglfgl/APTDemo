package com.fdz.aptdemo;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import com.fdz.apt.annotation.annotation.BindActivity;
import com.fdz.apt.annotation.annotation.BindView;

import java.util.logging.Logger;

@BindActivity
public class MainActivity extends AppCompatActivity {

    @BindView(R.id.tv_hello_world)
    TextView tvHelloWorld;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        APTMainActivity.bindView(this);
    }
}
