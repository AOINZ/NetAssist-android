package com.example.netassist;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.List;

/**
 * 快捷指令列表适配器
 */
public class QuickCommandAdapter extends BaseAdapter {
    private Context context;
    private List<QuickCommand> commands;
    private OnCommandSendListener sendListener;
    private OnCommandDeleteListener deleteListener;

    public interface OnCommandSendListener {
        void onCommandSend(QuickCommand command);
    }

    public interface OnCommandDeleteListener {
        void onCommandDelete(QuickCommand command, int position);
    }

    public QuickCommandAdapter(Context context, List<QuickCommand> commands) {
        this.context = context;
        this.commands = commands;
    }

    public void setOnCommandSendListener(OnCommandSendListener listener) {
        this.sendListener = listener;
    }

    public void setOnCommandDeleteListener(OnCommandDeleteListener listener) {
        this.deleteListener = listener;
    }

    @Override
    public int getCount() {
        return commands.size();
    }

    @Override
    public Object getItem(int position) {
        return commands.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_command, parent, false);
            holder = new ViewHolder();
            holder.tvName = convertView.findViewById(R.id.tv_command_name);
            holder.tvData = convertView.findViewById(R.id.tv_command_data);
            holder.btnSend = convertView.findViewById(R.id.btn_send_command);
            holder.btnDelete = convertView.findViewById(R.id.btn_delete_command);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        QuickCommand command = commands.get(position);
        holder.tvName.setText(command.getName());
        holder.tvData.setText(command.getData());

        holder.btnSend.setOnClickListener(v -> {
            if (sendListener != null) {
                sendListener.onCommandSend(command);
            }
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (deleteListener != null) {
                deleteListener.onCommandDelete(command, position);
            }
        });

        return convertView;
    }

    static class ViewHolder {
        TextView tvName;
        TextView tvData;
        ImageButton btnSend;
        ImageButton btnDelete;
    }
}
