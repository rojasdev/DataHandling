package com.rhix.datahandling;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ListView;
import android.widget.Toast;

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
import java.util.Collections;
import java.util.List;

public class FragmentLazyLoadingLive extends Fragment {

    private ListView listViewData;
    private UserAdapter userAdapter;
    private List<User> userList;
    private static final String API_URL = "https://devlab.helioho.st/api/listing.php";
    private static final String API_KEY = "7999b0bd43fe96b083f8430a0de1cc65ecf3902993d15ffb6d3a287f9e939000"; // Replace with your API key

    private int currentOffset = 0;
    private final int PAGE_SIZE = 20; // Number of users to load per page
    private boolean isLoading = false; // Prevent multiple API calls simultaneously
    private boolean allDataLoaded = false; // Flag to check if all data has been loaded
    private SwipeRefreshLayout swipeRefreshLayout; // SwipeRefreshLayout instance

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_swipe, container, false);

        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        listViewData = view.findViewById(R.id.userListViewData);
        userList = new ArrayList<>();

        // Initialize adapter and set it to the ListView
        userAdapter = new UserAdapter(getContext(), userList);
        listViewData.setAdapter(userAdapter);

        // Set up the swipe refresh layout
        swipeRefreshLayout.setOnRefreshListener(() -> {
            // Refresh data when swiped down
            currentOffset = 0; // Reset offset for fresh data
            fetchDataFromApi(currentOffset);
        });

        // Fetch the initial data from the API
        fetchDataFromApi(currentOffset);

        // Set a scroll listener for lazy loading
        listViewData.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                // No need to handle this
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                if (!isLoading && !allDataLoaded && (firstVisibleItem + visibleItemCount >= totalItemCount) && totalItemCount > 0) {
                    // Load more data when the user scrolls near the end of the list
                    currentOffset += PAGE_SIZE;
                    fetchDataFromApi(currentOffset);
                }
            }
        });

        return view;
    }

    private void fetchDataFromApi(int offset) {
        if (isLoading || allDataLoaded) return; // Prevent multiple requests or when all data is loaded

        isLoading = true;
        swipeRefreshLayout.setRefreshing(true); // Show loading spinner

        new Thread(() -> {
            try {
                URL url = new URL(API_URL); // Adjust the URL for your API

                // Open connection
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

                // Create the POST parameters with pagination
                String postData = "api_key=" + API_KEY + "&limit=" + PAGE_SIZE + "&offset=" + offset;

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
                    parseJsonAndPopulateListView(jsonResponse);
                } else {
                    throw new Exception("API returned error response: " + responseCode);
                }

            } catch (Exception e) {
                e.printStackTrace();
                // Show error message on main thread
                new Handler(Looper.getMainLooper()).post(() ->
                        Toast.makeText(getActivity(), "Failed to retrieve data!", Toast.LENGTH_SHORT).show());
            }
            isLoading = false; // Reset loading state
            swipeRefreshLayout.setRefreshing(false); // Hide loading spinner
        }).start();
    }

    private void parseJsonAndPopulateListView(String jsonResponse) {
        try {
            // Parse the main JSON object
            JSONObject jsonObject = new JSONObject(jsonResponse);
            // Extract the "users" array from the response
            JSONArray usersArray = jsonObject.getJSONArray("users");

            if (usersArray.length() == 0) {
                allDataLoaded = true; // Mark that all data has been loaded
                return;
            }

            // Initialize the database helper for native
            DatabaseHelper db = new DatabaseHelper(getContext());

            // List to hold newly fetched users
            List<User> newUsers = new ArrayList<>();

            // Loop through the "users" array
            for (int i = 0; i < usersArray.length(); i++) {
                JSONObject userObject = usersArray.getJSONObject(i);

                // Extract fields from the user object
                int id = userObject.getInt("id");
                String firstName = userObject.getString("first_name");
                String lastName = userObject.getString("last_name");
                String email = userObject.getString("email");

                // Check if the record already exists in the list
                boolean userExists = false;
                for (User user : userList) {
                    if (user.getId() == id) {
                        userExists = true;
                        break;
                    }
                }

                if (!userExists) {
                    // Create a new User object
                    User user = new User(id, firstName, lastName, email);

                    // Add the new user to the temporary list of new users
                    newUsers.add(user);

                    // Add the user to the database
                    db.addUser(user);
                }
            }

            // Add new users to the list
            userList.addAll(newUsers);

            // Sort the userList in descending order by ID (or created_at if needed)
            Collections.sort(userList, (user1, user2) -> Integer.compare(user2.getId(), user1.getId()));

            // Notify the adapter on the main thread
            getActivity().runOnUiThread(() -> {
                userAdapter.notifyDataSetChanged();
                // Optionally, scroll the list to the top
                if (!newUsers.isEmpty()) {
                    listViewData.setSelectionAfterHeaderView();
                }
            });

            Toast.makeText(getActivity(), "Data loaded from API", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
