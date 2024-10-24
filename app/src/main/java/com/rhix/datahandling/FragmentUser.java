package com.rhix.datahandling;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class FragmentUser extends Fragment {

    private ListView listViewData;
    private UserAdapter userAdapter;
    private List<User> userList;
    // define API URL here
    private static final String API_URL = "https://devlab.helioho.st/api/listing.php";
    // provide API KEY here
    private static final String API_KEY = "7999b0bd43fe96b083f8430a0de1cc65ecf3902993d15ffb6d3a287f9e939000"; // Replace with your API key

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main, container, false);

        listViewData = view.findViewById(R.id.userListViewData);
        userList = new ArrayList<>();

        // Initialize adapter and set it to the ListView
        userAdapter = new UserAdapter(getContext(), userList);
        listViewData.setAdapter(userAdapter);

        // Fetch data from the API
        fetchDataFromApi();

        return view;
    }

    private void fetchDataFromApi() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // Replace with your API URL
                    URL url = new URL(API_URL); // Adjust the URL for your API

                    // Open connection
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("POST");
                    connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded"); // Form content type

                    // Create the POST parameters
                    String postData = "api_key=" + API_KEY;

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
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getActivity(), "Failed to retrieve data!", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        }).start();
    }

    private void parseJsonAndPopulateListView(String jsonResponse) {
        try {
            // Parse the main JSON object
            JSONObject jsonObject = new JSONObject(jsonResponse);
            // Extract the "users" array from the response
            JSONArray usersArray = jsonObject.getJSONArray("users");
            // Clear the existing list
            userList.clear();

            // Initialize the database helper for native
            DatabaseHelper db = new DatabaseHelper(getContext());
            // Clear existing database entries
            db.deleteAllUsers();

            // Loop through the "users" array
            for (int i = 0; i < usersArray.length(); i++) {
                JSONObject userObject = usersArray.getJSONObject(i);

                // Extract fields from the user object
                int id = userObject.getInt("id");
                String firstName = userObject.getString("first_name");
                String lastName = userObject.getString("last_name");
                String email = userObject.getString("email");

                // Create a new User object and add it to the list
                User user = new User(id, firstName, lastName, email);
                userList.add(user);

                // Add the user to the database native
                db.addUser(user);
            }

            // Notify the adapter on main thread
            getActivity().runOnUiThread(() -> userAdapter.notifyDataSetChanged());
            Toast.makeText(getActivity(), "Data saved to local database", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
