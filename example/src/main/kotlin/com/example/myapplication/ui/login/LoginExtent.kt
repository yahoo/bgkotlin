//
// Copyright Yahoo 2021
//
package com.example.myapplication.ui.login

import android.text.TextUtils
import android.util.Patterns
import com.yahoo.behaviorgraph.Extent
import com.yahoo.behaviorgraph.Graph
import com.yahoo.behaviorgraph.Moment
import com.yahoo.behaviorgraph.State

class LoginExtent(var loginActivityBG: LoginActivityBG, graph: Graph) : Extent<LoginExtent>(graph) {
    val email = State(this, "")
    val password = State(this, "")
    val emailValid = State(this, false)
    val passwordValid = State(this, false)
    val loginEnabled = State(this, false)
    val loggingIn = State(this, false)
    val loginClick = Moment<Unit>(this)
    val loginComplete = Moment<Boolean>(this)

    private fun validateEmail(target: CharSequence?): Boolean {
        return !TextUtils.isEmpty(target) && Patterns.EMAIL_ADDRESS.matcher(target).matches()
    }

    init {
        makeBehavior(listOf(email), listOf(emailValid)) { extent ->
            val email = extent.email.value
            val emailValid: Boolean = validateEmail(email)
            extent.emailValid.update(emailValid, true)
            extent.sideEffect(null) { extent ->
                extent.loginActivityBG.emailFeedbackTextView.text =
                    if (extent.emailValid.value) {
                        "✅"
                    } else {
                        "❌"
                    }
            }
        }

        makeBehavior(listOf(password), listOf(passwordValid)) { extent ->
            val password = extent.password.value
            val passwordValid = password.isNotEmpty()
            extent.passwordValid.update(passwordValid, true)
            extent.sideEffect("passwordFeedback") {
                extent.loginActivityBG.passwordFeedbackTextView.text =
                    if (extent.passwordValid.value) {
                        "✅"
                    } else {
                        "❌"
                    }

            }
        }

        makeBehavior(listOf(emailValid, passwordValid, loggingIn), listOf(loginEnabled)) { extent ->
            val enabled =
                extent.emailValid.value && extent.passwordValid.value && !extent.loggingIn.value
            extent.loginEnabled.update(enabled, true)
            extent.sideEffect("enable login button") { extent ->
                loginActivityBG.loginButton.isEnabled = extent.loginEnabled.value
            }
        }

        makeBehavior(listOf(loginClick, loginComplete), listOf(loggingIn)) { extent ->
            if (extent.loginClick.justUpdated && extent.loginEnabled.traceValue) {
                extent.loggingIn.update(true, true)
            } else if (extent.loginComplete.justUpdated && extent.loggingIn.value) {
                extent.loggingIn.update(false, true)
            }

            if (extent.loggingIn.justUpdatedTo(true)) {
                extent.sideEffect("login api call") { extent ->
                }
            }
        }

        makeBehavior(listOf(loggingIn, loginComplete), null) { extent ->
            extent.sideEffect("login status") { extent ->
                var status = ""
                if (extent.loggingIn.value) {
                    status = "Logging in...";
                } else if (extent.loggingIn.justUpdatedTo(false)) {
                    if (extent.loginComplete.justUpdated && extent.loginComplete.value) {
                        status = "Login Success";
                    } else if (extent.loginComplete.justUpdated && !extent.loginComplete.value) {
                        status = "Login Failed";
                    }
                }
                extent.loginActivityBG.loginStatusTextView.text = status

            }
        }
    }
}
