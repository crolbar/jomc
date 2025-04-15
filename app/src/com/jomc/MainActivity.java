package com.jomc;

// import androidx.appcompat.app.AppCompatActivity;

// public class MainActivity extends AppCompatActivity
// {
//     @Override
//     protected void onCreate(Bundle savedintanceState) {
//         super.onCreate(savedIntanceState);
//         setContentView(R.layout.activity_main);
//     }
// }


import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Button button = findViewById(R.id.my_button);
        // button.setOnClickListener(view -> 
        //     Toast.makeText(this, "Button clicked!", Toast.LENGTH_SHORT).show()
        // );
    }
}
