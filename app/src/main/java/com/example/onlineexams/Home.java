package com.example.onlineexams;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class Home extends AppCompatActivity {

    private String userUID;
    private String firstName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


        // ---------------------------------------------------------------------------------
        DatabaseReference database = FirebaseDatabase.getInstance().getReference();
        ProgressDialog progressDialog = new ProgressDialog(Home.this);
        progressDialog.setMessage("Loading...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        Bundle b = getIntent().getExtras();
        userUID = b.getString("User UID");

        TextView name = findViewById(R.id.name);
        TextView total_questions = findViewById(R.id.total_questions);
        TextView total_points = findViewById(R.id.total_points);
        Button startQuiz = findViewById(R.id.startQuiz);
        Button createQuiz = findViewById(R.id.createQuiz);
        RelativeLayout solvedQuizzes = findViewById(R.id.solvedQuizzes);
        RelativeLayout your_quizzes = findViewById(R.id.your_quizzes);
        EditText quiz_title = findViewById(R.id.quiz_title);
        EditText start_quiz_id = findViewById(R.id.start_quiz_id);
        ImageView signout = findViewById(R.id.signout);

        // ValueEventListener là một listener dùng để lắng nghe mọi thay đổi của dữ liệu trong node được theo dõi.
        ValueEventListener listener = new ValueEventListener() {
            // onDataChange(...) được gọi mỗi khi dữ liệu tại node thay đổi (hoặc lần đầu khi đọc).
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                DataSnapshot usersRef = snapshot.child("Users").child(userUID);
                firstName = usersRef.child("First Name").getValue().toString();

                if (usersRef.hasChild("Total Points")) {
                    String totalPoints = usersRef.child("Total Points").getValue().toString();
                    int points = Integer.parseInt(totalPoints);
                    total_points.setText(String.format("%03d", points));
                }

                if (usersRef.hasChild("Total Questions")) {
                    String totalQuestions = usersRef.child("Total Questions").getValue().toString();
                    int questions = Integer.parseInt(totalQuestions);
                    total_questions.setText(String.format("%03d", questions));
                }

                name.setText("Welcome " + firstName + "!");
                progressDialog.dismiss();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(Home.this, "Can't connect", Toast.LENGTH_SHORT).show();
            }
        };

        // Lệnh này nói với Firebase: “Tôi muốn lắng nghe mọi thay đổi trong toàn bộ cơ sở dữ liệu.”
        //Khi bất kỳ phần nào của Realtime Database thay đổi, hàm onDataChange(...) sẽ được gọi lại.
        database.addValueEventListener(listener);

        signout.setOnClickListener(view -> {
            FirebaseAuth.getInstance().signOut();
            Intent i = new Intent(Home.this, MainActivity.class);
            startActivity(i);
            finish();
        });

        createQuiz.setOnClickListener(view -> {
            if (quiz_title.getText().toString().equals("")) {
                quiz_title.setError("Quiz title cannot be empty");
                return;
            }

            Intent i = new Intent(Home.this, ExamEditor.class);
            i.putExtra("Quiz Title", quiz_title.getText().toString());
            quiz_title.setText("");
            startActivity(i);
        });

        startQuiz.setOnClickListener(view -> {
            if (start_quiz_id.getText().toString().equals("")) {
                start_quiz_id.setError("Quiz title cannot be empty");
            }
            Intent i = new Intent(Home.this, Exam.class);
            i.putExtra("Quiz ID", start_quiz_id.getText().toString());
            start_quiz_id.setText("");
            startActivity(i);
        });

        solvedQuizzes.setOnClickListener(view -> {
            Intent i = new Intent(Home.this, ListQuizzes.class);
            i.putExtra("Operation", "List Solved Quizzes");
            startActivity(i);
        });

        your_quizzes.setOnClickListener(view -> {
            Intent i = new Intent(Home.this, ListQuizzes.class);
            i.putExtra("Operation", "List Created Quizzes");
            startActivity(i);
        });



    }
}