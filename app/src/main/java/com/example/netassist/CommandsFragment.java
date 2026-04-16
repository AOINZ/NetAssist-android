package com.example.netassist;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import java.util.List;

public class CommandsFragment extends Fragment {

    private LinearLayout llCommandsContainer;
    private Button btnAddCommand;
    private List<QuickCommand> quickCommands;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_commands, container, false);

        llCommandsContainer = view.findViewById(R.id.ll_commands_container);
        btnAddCommand = view.findViewById(R.id.btn_add_command);

        btnAddCommand.setOnClickListener(v -> showAddCommandDialog());

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshCommands();
    }

    public void refreshCommands() {
        if (getActivity() != null) {
            quickCommands = ((MainActivity) getActivity()).getQuickCommands();
            renderCommands();
        }
    }

    private void renderCommands() {
        llCommandsContainer.removeAllViews();

        for (int i = 0; i < quickCommands.size(); i++) {
            QuickCommand command = quickCommands.get(i);
            View itemView = createCommandItemView(command, i);
            llCommandsContainer.addView(itemView);
        }
    }

    private View createCommandItemView(QuickCommand command, int position) {
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.item_command, llCommandsContainer, false);

        TextView tvName = view.findViewById(R.id.tv_command_name);
        TextView tvData = view.findViewById(R.id.tv_command_data);
        ImageButton btnSend = view.findViewById(R.id.btn_send_command);
        ImageButton btnDelete = view.findViewById(R.id.btn_delete_command);

        tvName.setText(command.getName());
        tvData.setText(command.getData());

        btnSend.setOnClickListener(v -> {
            if (TcpManager.getInstance().isConnected()) {
                TcpManager.getInstance().send(command.getData());
                Toast.makeText(requireContext(), "已发送: " + command.getName(), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(), "未连接到服务器", Toast.LENGTH_SHORT).show();
            }
        });

        btnDelete.setOnClickListener(v -> {
            new AlertDialog.Builder(requireContext())
                    .setTitle("删除指令")
                    .setMessage("确定要删除 \"" + command.getName() + "\" 指令吗？")
                    .setPositiveButton("删除", (dialog, which) -> {
                        quickCommands.remove(position);
                        renderCommands();
                        // 保存到存储
                        if (getActivity() != null) {
                            ((MainActivity) getActivity()).refreshCommands();
                        }
                        Toast.makeText(requireContext(), "已删除", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("取消", null)
                    .show();
        });

        return view;
    }

    private void showAddCommandDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_command, null);
        EditText etName = dialogView.findViewById(R.id.et_command_name);
        EditText etData = dialogView.findViewById(R.id.et_command_data);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel);
        Button btnSave = dialogView.findViewById(R.id.btn_save);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setCancelable(true)
                .create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String data = etData.getText().toString().trim();

            if (name.isEmpty()) {
                Toast.makeText(requireContext(), "请输入指令名称", Toast.LENGTH_SHORT).show();
                return;
            }

            if (data.isEmpty()) {
                Toast.makeText(requireContext(), "请输入指令内容", Toast.LENGTH_SHORT).show();
                return;
            }

            quickCommands.add(new QuickCommand(name, data));
            renderCommands();
            // 保存到存储
            if (getActivity() != null) {
                ((MainActivity) getActivity()).refreshCommands();
            }
            dialog.dismiss();
            Toast.makeText(requireContext(), "已添加", Toast.LENGTH_SHORT).show();
        });

        dialog.show();
    }
}
