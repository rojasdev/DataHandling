package com.rhix.datahandling;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

public class UserAdapter extends BaseAdapter {
    private Context context;
    private List<User> userList;

    public UserAdapter(Context context, List<User> userList) {
        this.context = context;
        this.userList = userList;
    }

    @Override
    public int getCount() {
        return userList.size();
    }

    @Override
    public Object getItem(int position) {
        return userList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(context);
            convertView = inflater.inflate(R.layout.custom_list_item, parent, false);
        }

        // Get the current User object
        User currentUser = (User) getItem(position);

        // Find TextViews and populate them with data
        TextView tvId = convertView.findViewById(R.id.tv_id);
        TextView tvLastName = convertView.findViewById(R.id.tv_last_name);
        TextView tvFirstName = convertView.findViewById(R.id.tv_first_name);
        TextView tvEmail = convertView.findViewById(R.id.tv_email);

        tvId.setText("ID: " + currentUser.getId());
        tvLastName.setText("Last Name: " + currentUser.getLastName());
        tvFirstName.setText("First Name: " + currentUser.getFirstName());
        tvEmail.setText("Email: " + currentUser.getEmail());

        return convertView;
    }
}
