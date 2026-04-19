package com.bsbarron.midschoolapp

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bsbarron.midschoolapp.ui.setup.SetupViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SetupActivity : AppCompatActivity() {
    private val viewModel: SetupViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_setup)

        // 초기 설정 화면은 간단한 입력 폼이라 필요한 위젯만 직접 찾는다.
        val gradeInput = findViewById<android.widget.EditText>(R.id.gradeInput)
        val classInput = findViewById<android.widget.EditText>(R.id.classInput)
        val saveButton = findViewById<android.widget.Button>(R.id.saveStudentInfoButton)

        saveButton.setOnClickListener {
            viewModel.updateGrade(gradeInput.text.toString().trim())
            viewModel.updateClassroom(classInput.text.toString().trim())
            lifecycleScope.launch {
                viewModel.saveStudentInfo()
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    // 유효성 검증 실패 같은 일회성 메시지는 토스트로 바로 소비한다.
                    viewModel.messageEvent.collect { messageRes ->
                        Toast.makeText(this@SetupActivity, messageRes, Toast.LENGTH_SHORT).show()
                    }
                }
                launch {
                    // 저장이 끝나면 메인 화면으로 이동하고 현재 화면은 스택에서 제거한다.
                    viewModel.navigationEvent.collect {
                        startActivity(Intent(this@SetupActivity, MainActivity::class.java))
                        finish()
                    }
                }
            }
        }
    }
}
