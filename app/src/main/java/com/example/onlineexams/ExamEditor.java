package com.example.onlineexams;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class ExamEditor extends AppCompatActivity {

    private static ArrayList<Question> data;
    private RecyclerView listview;
    private int quizID;

// 1. ExamEditor khởi tạo (onCreate)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_exam_editor);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // -----------------------------------------------------------------------------------------

        Bundle b = getIntent().getExtras();
        String quizTitle = b.getString("Quiz Title");

        TextView title = findViewById(R.id.title);
        title.setText(quizTitle);

        Button submit = findViewById(R.id.submit);

    // Kết nối Firebase Realtime Database → đọc Quizzes/Last ID để sinh quizID mới.
    // Nếu chưa có, bắt đầu từ 100000
        DatabaseReference database = FirebaseDatabase.getInstance().getReference();
        ValueEventListener listener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // Đoạn này chỉ để tạo ID :)
                if (snapshot.child("Quizzes").hasChild("Last ID")) {
                    String lID = snapshot.child("Quizzes").child("Last ID").getValue().toString();
                    quizID = Integer.parseInt(lID) + 1;
                } else {
                    quizID = 100000;
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ExamEditor.this, "Can't connect", Toast.LENGTH_SHORT).show();
            }
        };

        database.addValueEventListener(listener);

    // Tạo danh sách câu hỏi data (ban đầu 1 câu rỗng) và gắn vào RecyclerView qua CustomAdapter.
        data = new ArrayList<>();
        data.add(new Question());

        listview = findViewById(R.id.listview);
        listview.setLayoutManager(new LinearLayoutManager(this));
        CustomAdapter customAdapter = new CustomAdapter(data);
        listview.setAdapter(customAdapter);

    // Gắn ItemTouchHelper để người dùng kéo-thả đổi thứ tự câu hỏi.
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(callback);
        itemTouchHelper.attachToRecyclerView(listview);


        submit.setOnClickListener(view -> {
            DatabaseReference ref = database.child("Quizzes");
            // 1. Lưu Last ID mới.
            ref.child("Last ID").setValue(quizID);

            // 2. Ghi tiêu đề, số lượng câu hỏi và toàn bộ chi tiết từng câu (Question, Option 1-4, Ans).
            ref.child(String.valueOf(quizID)).child("Title").setValue(quizTitle);
            ref.child(String.valueOf(quizID)).child("Total Questions").setValue(data.size());
            DatabaseReference qRef = ref.child(String.valueOf(quizID)).child("Questions");

            for (int i = 0; i < data.size(); i++) {
                String p = String.valueOf(i);
                qRef.child(p).child("Question").setValue(data.get(i).getQuestion());
                qRef.child(p).child("Option 1").setValue(data.get(i).getOption1());
                qRef.child(p).child("Option 2").setValue(data.get(i).getOption2());
                qRef.child(p).child("Option 3").setValue(data.get(i).getOption3());
                qRef.child(p).child("Option 4").setValue(data.get(i).getOption4());
                qRef.child(p).child("Ans").setValue(data.get(i).getCorrectAnswer());
            }

            // 3. Thêm mã bài thi vào nút Quizzes Created của người tạo (uid hiện tại).
            database.child("Users").child(
                    FirebaseAuth.getInstance().getCurrentUser().getUid())
                    .child("Quizzes Created").child(String.valueOf(quizID))
                    .setValue("");

            // 4. Sao chép quizID vào clipboard và đóng activity.
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Quiz ID", String.valueOf(quizID));
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "Your this id : " + quizID + " copied to clipboard",
                    Toast.LENGTH_SHORT).show();

            finish();
        });
    }


// 3. Kéo-thả sắp xếp
    // Gắn ItemTouchHelper để người dùng kéo-thả đổi thứ tự câu hỏi
    // ItemTouchHelper.SimpleCallback cho phép di chuyển UP/DOWN.
    ItemTouchHelper.SimpleCallback callback = new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder dragged, @NonNull RecyclerView.ViewHolder target) {
            int position_dragged = dragged.getAdapterPosition();
            int position_target = target.getAdapterPosition();

            // Khi kéo hoàn tất: Collections.swap(data, draggedPos, targetPos) + notifyItemMoved
            Collections.swap(data, position_dragged, position_target);
            listview.getAdapter().notifyItemMoved(position_dragged, position_target);

            return true;
        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {}
    };


// 2. CustomAdapter quản lý mỗi item (layout question_edit)
    public static class CustomAdapter extends RecyclerView.Adapter<ExamEditor.CustomAdapter.ViewHolder> {

        public static class ViewHolder extends RecyclerView.ViewHolder {
            private final EditText question;
            private final RadioButton option1rb;
            private final RadioButton option2rb;
            private final RadioButton option3rb;
            private final RadioButton option4rb;

            private final EditText option1et;
            private final EditText option2et;
            private final EditText option3et;
            private final EditText option4et;

            private final LinearLayout new_question;

            private final RadioGroup radio_group;

            public ViewHolder(View view) {
                super(view);
                question = view.findViewById(R.id.question);
                option1rb = view.findViewById(R.id.option1rb);
                option2rb = view.findViewById(R.id.option2rb);
                option3rb = view.findViewById(R.id.option3rb);
                option4rb = view.findViewById(R.id.option4rb);

                option1et = view.findViewById(R.id.option1et);
                option2et = view.findViewById(R.id.option2et);
                option3et = view.findViewById(R.id.option3et);
                option4et = view.findViewById(R.id.option4et);

                new_question = view.findViewById(R.id.new_question);

                radio_group = view.findViewById(R.id.radio_group);
            }

            public RadioGroup getRadio_group() {
                return radio_group;
            }

            public EditText getQuestion() {
                return question;
            }

            public LinearLayout getNew_question() {
                return new_question;
            }

            public EditText getOption4et() {
                return option4et;
            }

            public EditText getOption3et() {
                return option3et;
            }

            public EditText getOption2et() {
                return option2et;
            }

            public EditText getOption1et() {
                return option1et;
            }

            public RadioButton getOption4rb() {
                return option4rb;
            }

            public RadioButton getOption3rb() {
                return option3rb;
            }

            public RadioButton getOption2rb() {
                return option2rb;
            }

            public RadioButton getOption1rb() {
                return option1rb;
            }
        }

        public CustomAdapter (ArrayList<Question> data) {}


        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.question_edit, parent, false);

            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, @SuppressLint("RecyclerView") int position) {
            holder.setIsRecyclable(false);

            // Đổ dữ liệu từ data vào view.
            holder.getOption1et().setText(data.get(position).getOption1());
            holder.getOption2et().setText(data.get(position).getOption2());
            holder.getOption3et().setText(data.get(position).getOption3());
            holder.getOption4et().setText(data.get(position).getOption4());

            switch (data.get(position).getCorrectAnswer()) {
                case 1:
                    holder.getOption1rb().setChecked(true);
                    break;
                case 2:
                    holder.getOption2rb().setChecked(true);
                    break;
                case 3:
                    holder.getOption3rb().setChecked(true);
                    break;
                case 4:
                    holder.getOption4rb().setChecked(true);
                    break;
            }

            // TextWatcher cập nhật ngược vào data mỗi khi người dùng gõ.
            holder.getQuestion().addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(Editable editable) {
                    data.get(position).setQuestion(editable.toString());
                }
            });

            holder.getOption1et().addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(Editable editable) {
                    data.get(position).setOption1(editable.toString());
                }
            });

            holder.getOption2et().addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(Editable editable) {
                    data.get(position).setOption2(editable.toString());
                }
            });

            holder.getOption3et().addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(Editable editable) {
                    data.get(position).setOption3(editable.toString());
                }
            });

            holder.getOption4et().addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(Editable editable) {
                    data.get(position).setOption4(editable.toString());
                }
            });

            // OnCheckedChangeListener ghi đáp án đúng (1-4) vào data.
            holder.getRadio_group().setOnCheckedChangeListener((radioGroup, i) -> {
                if (holder.getOption1rb().isChecked()) data.get(position).setCorrectAnswer(1);
                if (holder.getOption2rb().isChecked()) data.get(position).setCorrectAnswer(2);
                if (holder.getOption3rb().isChecked()) data.get(position).setCorrectAnswer(3);
                if (holder.getOption4rb().isChecked()) data.get(position).setCorrectAnswer(4);
            });

            // Với item cuối cùng, hiển thị nút new_question → click sẽ data.add(new Question()) rồi notifyDataSetChanged().
            if (position == (data.size() - 1)) {
                holder.getNew_question().setVisibility(View.VISIBLE);

                holder.getNew_question().setOnClickListener(view -> {
                    data.add(new Question());
                    notifyDataSetChanged();
                });
            }
        }

        @Override
        public int getItemCount() {
            return 0;
        }
    }
}