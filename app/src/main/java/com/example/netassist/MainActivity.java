package com.example.netassist;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private TabLayout tabLayout;
    private ViewPager2 viewPager;

    // 快捷指令相关
    private LinearLayout llCommandsContainer;
    private Button btnAddCommand;
    private List<QuickCommand> quickCommands;
    private QuickCommandManager commandManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        setupViewPager();
        initQuickCommands();
    }

    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        tabLayout = findViewById(R.id.tab_layout);
        viewPager = findViewById(R.id.view_pager);
    }

    private void setupViewPager() {
        // 创建Fragment列表
        List<Fragment> fragments = new ArrayList<>();
        fragments.add(new MainFragment());
        fragments.add(new CommandsFragment());
        fragments.add(new SettingsFragment());

        // 设置适配器
        FragmentStateAdapter adapter = new FragmentStateAdapter(this) {
            @NonNull
            @Override
            public Fragment createFragment(int position) {
                return fragments.get(position);
            }

            @Override
            public int getItemCount() {
                return fragments.size();
            }
        };

        viewPager.setAdapter(adapter);

        // 设置Tab标题
        String[] titles = {"通信", "快捷指令", "设置"};
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            tab.setText(titles[position]);
        }).attach();
    }

    private void initQuickCommands() {
        commandManager = new QuickCommandManager(this);
        quickCommands = commandManager.loadCommands();
    }

    public List<QuickCommand> getQuickCommands() {
        return quickCommands;
    }

    public void refreshCommands() {
        // 保存指令到存储
        commandManager.saveCommands(quickCommands);
        
        // 通知CommandsFragment刷新
        Fragment fragment = getSupportFragmentManager().findFragmentByTag("f1");
        if (fragment instanceof CommandsFragment) {
            ((CommandsFragment) fragment).refreshCommands();
        }
    }
}
