package com.rhix.datahandling;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class FragmentLazyLoadingNative extends Fragment {

    private ListView listView;
    private UserAdapter userAdapter;
    private List<User> userList;
    private DatabaseHelper databaseHelper;

    private boolean isLoading = false;
    private int currentOffset = 0;
    private final int PAGE_SIZE = 10; // Number of users to load per page
    private SwipeRefreshLayout swipeRefreshLayout; // For swipe-to-refresh

    private static final String API_URL = "https://devlab.helioho.st/api/listing.php";
    private static final String API_KEY = "7999b0bd43fe96b083f8430a0de1cc65ecf3902993d15ffb6d3a287f9e939000"; // Replace with your API key

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_swipe, container, false);

        // Initialize SwipeRefreshLayout
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        listView = view.findViewById(R.id.userListViewData);
        databaseHelper = new DatabaseHelper(getContext());
        userList = new ArrayList<>();

        // Initialize adapter and set it to the ListView
        userAdapter = new UserAdapter(getContext(), userList);
        listView.setAdapter(userAdapter);

        // Load the initial data (first page)
        loadUsers(currentOffset);

        // Set a scroll listener to the ListView for lazy loading
        listView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                // No need to handle this for lazy loading
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                if (!isLoading && (firstVisibleItem + visibleItemCount >= totalItemCount) && totalItemCount > 0) {
                    // Load more data when the user scrolls near the end of the list
                    isLoading = true;
                    currentOffset += PAGE_SIZE;
                    loadUsers(currentOffset);
                }
            }
        });

        // Set up the swipe refresh listener
        swipeRefreshLayout.setOnRefreshListener(this::refreshData); // Call refresh data method

        return view;
    }

    // Method to load users from the database
    private void loadUsers(int offset) {
        List<User> newUsers = databaseHelper.getUsers(PAGE_SIZE, offset);
        if (!newUsers.isEmpty()) {
            userList.addAll(newUsers);
            userAdapter.notifyDataSetChanged();
        }
        isLoading = false; // Reset loading state once data is loaded
    }

    // Method to refresh data
    private void refreshData() {
        if (isInternetAvailable()) {
            fetchDataFromApi();
        } else {
            Toast.makeText(getContext(), "No internet connection, loading data from local storage.", Toast.LENGTH_SHORT).show();
            loadUsers(0); // Reload users from SQLite
            swipeRefreshLayout.setRefreshing(false); // Stop the refresh indicator
        }
    }

    // Method to fetch data from the API
    private void fetchDataFromApi() {
        isLoading = true; // Set loading state
        swipeRefreshLayout.setRefreshing(true); // Show loading spinner

        new Thread(() -> {
            try {
                URL url = new URL(API_URL); // Adjust the URL for your API
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

                // Create the POST parameters with pagination
                String postData = "api_key=" + API_KEY + "&limit=" + PAGE_SIZE + "&offset=" + currentOffset;

                // Enable output stream for the POST data
                connection.setDoOutput(true);
                OutputStream outputStream = connection.getOutputStream();
                outputStream.write(postData.getBytes());
                outputStream.flush();
                outputStream.close();

                // Check the response code
                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder stringBuilder = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        stringBuilder.append(line);
                    }

                    reader.close();
                    String jsonResponse = stringBuilder.toString();

                    // Parse JSON and update ListView on main thread
                    parseJsonAndUpdateDatabase(jsonResponse);
                } else {
                    throw new Exception("API returned error response: " + responseCode);
                }

            } catch (Exception e) {
                e.printStackTrace();
                // Show error message on main thread
                getActivity().runOnUiThread(() ->
                        Toast.makeText(getActivity(), "Failed to retrieve data!", Toast.LENGTH_SHORT).show());
            }
            isLoading = false; // Reset loading state
            swipeRefreshLayout.setRefreshing(false); // Hide loading spinner
        }).start();
    }

    // Method to parse JSON and update the database
    private void parseJsonAndUpdateDatabase(String jsonResponse) {
        try {
            // Parse the main JSON object
            JSONObject jsonObject = new JSONObject(jsonResponse);
            // Extract the "users" array from the response
            JSONArray usersArray = jsonObject.getJSONArray("users");

            // Clear existing users in the database
            databaseHelper.deleteAllUsers(); // Clear the database before adding new ones

            // Loop through the "users" array and add users to the database
            for (int i = 0; i < usersArray.length(); i++) {
                JSONObject userObject = usersArray.getJSONObject(i);

                // Extract fields from the user object
                int id = userObject.getInt("id");
                String firstName = userObject.getString("first_name");
                String lastName = userObject.getString("last_name");
                String email = userObject.getString("email");

                // Create a new User object
                User user = new User(id, firstName, lastName, email);

                // Add the user to the database
                databaseHelper.addUser(user);
            }

            // Reload users from the database to update the ListView
            userList.clear(); // Clear the current list
            loadUsers(0); // Reload users from SQLite

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Method to check internet availability
    private boolean isInternetAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
            return capabilities != null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        }
        return false;
    }
}
