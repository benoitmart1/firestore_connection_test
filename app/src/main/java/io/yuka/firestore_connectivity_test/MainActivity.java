package io.yuka.firestore_connectivity_test;

import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.github.pwittchen.reactivenetwork.library.rx2.ReactiveNetwork;
import com.google.android.gms.tasks.OnCanceledListener;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {

    public static final String TAG = "MainActivity";
    private FirebaseFirestore db;
    private TextView product;
    private TextView connection;
    private Disposable connectivityListener;
    private Boolean internetConnected = true;
    private Boolean networkConnected = true;
    private Integer status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d(TAG, "onCreate");

        db = FirebaseFirestore.getInstance();


        // Disable offline data
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(false)
                .build();
        db.setFirestoreSettings(settings);

        Button button = findViewById(R.id.button);
        product = findViewById(R.id.tv_product);
        connection = findViewById(R.id.tv_connection);

        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.d(TAG, "onClick");
                if (!internetConnected) return;
                getData(0);
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onCreate");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");

        if (connectivityListener != null && !connectivityListener.isDisposed())
            connectivityListener.dispose();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");

        connectivityListener();
    }

    public void getData(final int retry) {

        product.setText("...");

        DocumentReference docRef = db.collection("products").document("cCvdY5Sc0aKnhaXWSUML");
        docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {

                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        Log.d(TAG, "Data: " + document.getData());
                        product.setText((String) document.getData().get("name"));

                    } else {
                        Log.d(TAG, "No such document");
                    }
                } else {
                    Log.d(TAG, task.getException().getMessage());

                    if (retry < 20) {
                        Log.d(TAG, "retry: " + retry);

                        final Handler handler = new Handler();
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                getData(retry + 1);
                            }
                        }, 1000);
                    }
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.d(TAG, "Failure: " + e.getMessage());
            }
        }).addOnCanceledListener(new OnCanceledListener() {
            @Override
            public void onCanceled() {
                Log.d(TAG, "Canceled");
            }
        });
    }

    private void connectivityListener() {

        connectivityListener = ReactiveNetwork
                .observeInternetConnectivity()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(isConnectedToInternet -> {
                    internetConnected = isConnectedToInternet;
                    networkConnected = isOnline();
                    setConnectivity();
                });
    }

    public void setConnectivity() {

        if (!networkConnected)
            setOffline();
        else if (!internetConnected)
            setLowConnection();
        else
            setOnLine();
    }


    public boolean isOnline() {
        int newStatus = io.yuka.firestore_connectivity_test.Connectivity.getConnectivityStatusString(getApplicationContext());
        if (status == null || status != newStatus)
        status = newStatus;
        return status > 0;
    }

    private void setOffline() {

        if (connection.getText() == "Aucune connexion") return;
        Log.d(TAG, "Client is offline");
        connection.setText("Client is offline");
    }


    private void setLowConnection() {

        if (connection.getText() == "Connexion insuffisante") return;
        Log.d(TAG, "Client is slow");
        connection.setText("Client is slow");
    }

    private void setOnLine() {

        if (connection.getText() == "" || connection.getText() == "Connecté") return;
        Log.d(TAG, "Client is online");
        connection.setText("Client is online");

        final Handler handlerOn = new Handler();
        handlerOn.postDelayed(() -> {
            // Do something after 5s = 5000ms
            // Check if it's still online
            if (internetConnected) {
                connection.setAlpha(0);
            }
        }, 5000);
    }

}
