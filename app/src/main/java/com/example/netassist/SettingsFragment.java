package com.example.netassist;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class SettingsFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_settings, container, false);

        Button btnGithub = root.findViewById(R.id.btn_open_github);
        Button btnCopyGithub = root.findViewById(R.id.btn_copy_github);

        btnGithub.setOnClickListener(v -> openUrl(getString(R.string.github_url)));
        btnCopyGithub.setOnClickListener(v ->
                copyText(getString(R.string.github_label), getString(R.string.github_url))
        );

        return root;
    }

    private void openUrl(String url) {
        if (getContext() == null) {
            return;
        }

        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        if (intent.resolveActivity(requireContext().getPackageManager()) != null) {
            startActivity(intent);
        } else {
            Toast.makeText(getContext(), R.string.no_browser_found, Toast.LENGTH_SHORT).show();
        }
    }

    private void copyText(String label, String value) {
        if (getContext() == null) {
            return;
        }

        ClipboardManager clipboardManager =
                (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboardManager == null) {
            Toast.makeText(getContext(), R.string.copy_failed, Toast.LENGTH_SHORT).show();
            return;
        }

        clipboardManager.setPrimaryClip(ClipData.newPlainText(label, value));
        Toast.makeText(getContext(), R.string.copy_success, Toast.LENGTH_SHORT).show();
    }
}
