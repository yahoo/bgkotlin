//
// Copyright Yahoo 2021
//
package com.example.myapplication.ui.login

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.R

class LoginActivityBG : AppCompatActivity() {
    lateinit var usernameEditText: EditText
    lateinit var passwordEditText: EditText
    lateinit var loginButton: Button
    lateinit var loginExtent: LoginExtent
    lateinit var emailFeedbackTextView: TextView
    lateinit var passwordFeedbackTextView: TextView
    lateinit var loginStatusTextView: TextView
    lateinit var loginSucceededButton: Button
    lateinit var loginFailedButton: Button


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        usernameEditText = findViewById(R.id.username)
        passwordEditText = findViewById(R.id.password)
        loginButton = findViewById(R.id.login)

        emailFeedbackTextView = findViewById(R.id.emailFeedback)
        passwordFeedbackTextView = findViewById(R.id.passwordFeedback)
        loginStatusTextView = findViewById(R.id.loginStatus)
        loginSucceededButton = findViewById(R.id.loginSucceededButton)
        loginFailedButton = findViewById(R.id.loginFailedButton)

        loginExtent = LoginExtent(this, Globals.graph)

        Globals.graph.action("init") {
            loginExtent.addToGraph()
        }

        usernameEditText.afterTextChanged {
            loginExtent.email.updateWithAction(it, true)
        }

        passwordEditText.afterTextChanged {
            loginExtent.password.updateWithAction(it, true)
        }

        loginButton.setOnClickListener() {
            loginExtent.loginClick.updateWithAction(Unit)
        }

        loginSucceededButton.setOnClickListener() {
            loginExtent.loginComplete.updateWithAction(true)
        }

        loginFailedButton.setOnClickListener() {
            loginExtent.loginComplete.updateWithAction(false)
        }

    }
}

/**
 * Extension function to simplify setting an afterTextChanged action to EditText components.
 */
fun EditText.afterTextChanged(afterTextChanged: (String) -> Unit) {
    this.addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(editable: Editable?) {
            afterTextChanged.invoke(editable.toString())
        }

        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
    })
}
