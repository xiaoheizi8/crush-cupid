package cn.yzfy.crushApp;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import cn.yzfy.crushApp.api.AppPrefs;
import cn.yzfy.crushApp.api.AuthApi;
import cn.yzfy.crushApp.ui.HomeFragment;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppPrefs.init(this);
        setContentView(R.layout.activity_main);
        if (savedInstanceState == null) {
            int enter = R.anim.fade_scale_in;
            if (AuthApi.isLoggedIn()) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .setCustomAnimations(enter, 0)
                        .replace(R.id.fragment_container, new HomeFragment())
                        .commit();
            } else {
                getSupportFragmentManager()
                        .beginTransaction()
                        .setCustomAnimations(enter, 0)
                        .replace(R.id.fragment_container, new cn.yzfy.crushApp.ui.LoginFragment())
                        .commit();
            }
        }
    }
}