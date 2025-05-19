package com.example.onlineexams;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class Exam extends AppCompatActivity {

    private Question[] data;
    private String quizID;
    private String uid;
    private int oldTotalPoints = 0;
    private int oldTotalQuestions = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_exam);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // -------------------------------------------------------------------------------------

        quizID = getIntent().getStringExtra("Quiz ID");
        ListView listview = findViewById(R.id.listview);
        Button submit = findViewById(R.id.submit);
        TextView title = findViewById(R.id.title);

        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        DatabaseReference database = FirebaseDatabase.getInstance().getReference();
        ValueEventListener listener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.child("Quizzes").hasChild(quizID)) {
                    DataSnapshot ref = snapshot.child("Quizzes").child(quizID);
                    int num = Integer.parseInt(ref.child("Total Questions").getValue().toString());
                    data = new Question[num];

                    for (int i = 0; i < num; i++) {
                        DataSnapshot qRef = ref.child("Questions").child(String.valueOf(i));
                        Question question = new Question();
                        question.setQuestion(qRef.child("Question").getValue().toString());
                        question.setOption1(qRef.child("Option 1").getValue().toString());
                        question.setOption2(qRef.child("Option 2").getValue().toString());
                        question.setOption3(qRef.child("Option 3").getValue().toString());
                        question.setOption4(qRef.child("Option 4").getValue().toString());

                        int ans = Integer.parseInt(qRef.child("Ans").getValue().toString());
                        question.setCorrectAnswer(ans);
                        data[i] = question;
                    }

                    ListAdapter listAdapter = new ListAdapter(data);
                    listview.setAdapter(listAdapter);
                    DataSnapshot ref2 = snapshot.child("Users").child(uid);

                    if (ref2.hasChild("Total Points")) {
                        oldTotalPoints = Integer.parseInt(ref2.child("Total Points").getValue().toString());
                    }
                    if (ref2.hasChild("Total Questions")) {
                        oldTotalQuestions= Integer.parseInt(ref2.child("Total Questions").getValue().toString());
                    }
                } else {
                    finish();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(Exam.this, "Can't connect", Toast.LENGTH_SHORT).show();
            }
        };

        database.addValueEventListener(listener);
    }

    public class ListAdapter extends BaseAdapter {
        Question[] arr;

        ListAdapter(Question[] arr2) {
            arr = arr2;
        }

        @Override
        public int getCount() {
            return arr.length;
        }

        @Override
        public Object getItem(int i) {
            return arr[i];
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            LayoutInflater inflater = getLayoutInflater();
            View v = inflater.inflate(R.layout.question, null);

            TextView question = v.findViewById(R.id.question);
            RadioButton option1 = v.findViewById(R.id.option1);
            RadioButton option2 = v.findViewById(R.id.option2);
            RadioButton option3 = v.findViewById(R.id.option3);
            RadioButton option4 = v.findViewById(R.id.option4);

            question.setText(data[i].getQuestion());
            option1.setText(data[i].getOption1());
            option2.setText(data[i].getOption2());
            option3.setText(data[i].getOption3());
            option4.setText(data[i].getOption4());

            option1.setOnCheckedChangeListener((CompoundButton, b) -> {
                if (b) data[i].setSelectedAnswer(1);
            });
            option2.setOnCheckedChangeListener((CompoundButton, b) -> {
                if (b) data[i].setSelectedAnswer(2);
            });
            option3.setOnCheckedChangeListener((CompoundButton, b) -> {
                if (b) data[i].setSelectedAnswer(3);
            });
            option4.setOnCheckedChangeListener((CompoundButton, b) -> {
                if (b) data[i].setSelectedAnswer(4);
            });

            switch ( data[i].getSelectedAnswerAnswer() ) {
                case 1:
                    option1.setChecked(true);
                    break;
                case 2:
                    option1.setChecked(true);
                    break;
                case 3:
                    option1.setChecked(true);
                    break;
                case 4:
                    option1.setChecked(true);
                    break;
            }
            return v;
        }
    }
}