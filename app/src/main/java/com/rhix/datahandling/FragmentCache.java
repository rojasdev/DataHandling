package com.rhix.datahandling;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import androidx.fragment.app.Fragment;

import java.util.List;

public class FragmentCache extends Fragment {

    private ListView listView;
    private UserAdapter userAdapter;
    private List<User> userList;
    private DatabaseHelper databaseHelper;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main, container, false);

        listView = view.findViewById(R.id.userListViewData);
        databaseHelper = new DatabaseHelper(getContext());

        // Retrieve data from the SQLite database
        userList = databaseHelper.getAllUsers();

        // Initialize adapter and set it to the ListView
        userAdapter = new UserAdapter(getContext(), userList);
        listView.setAdapter(userAdapter);

        return view;
    }
}
