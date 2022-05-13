//
// Copyright Yahoo 2021
//
package com.example.myapplication.ui.login

import android.text.TextUtils
import android.util.Patterns
import com.yahoo.behaviorgraph.Extent
import com.yahoo.behaviorgraph.Graph
import com.yahoo.behaviorgraph.behavior
import com.yahoo.behaviorgraph.sideEffect

class LoginExtent(var loginActivityBG: LoginActivityBG, graph: Graph) : Extent(graph) {
    val email = this.state("")
    val password = this.state("")
    val emailValid = this.state(false)
    val passwordValid = this.state(false)
    val loginEnabled = this.state(false)
    val loggingIn = this.state(false)
    val loginClick = this.moment()
    val loginComplete = this.typedMoment<Boolean>()

    private fun validateEmail(target: CharSequence?): Boolean {
        return !TextUtils.isEmpty(target) && Patterns.EMAIL_ADDRESS.matcher(target).matches()
    }

    init {
        behavior()
            .supplies(emailValid)
            .demands(email, didAdd)
            .runs {
                emailValid.update(validateEmail(email.value))
                sideEffect(null) { extent ->
                    extent.loginActivityBG.emailFeedbackTextView.text =
                        if (extent.emailValid.value) {
                            "✅"
                        } else {
                            "❌"
                        }
                }
            }

        behavior()
            .supplies(passwordValid)
            .demands(password, didAdd)
            .runs {
                passwordValid.update(password.value.isNotEmpty())
                sideEffect("passwordFeedback") {
                    loginActivityBG.passwordFeedbackTextView.text =
                        if (passwordValid.value) {
                            "✅"
                        } else {
                            "❌"
                        }

                }
            }

        behavior()
            .supplies(loginEnabled)
            .demands(emailValid, passwordValid, loggingIn, didAdd)
            .runs {
                val enabled =
                    emailValid.value && passwordValid.value && !loggingIn.value
                loginEnabled.update(enabled)
                sideEffect("enable login button") { extent ->
                    loginActivityBG.loginButton.isEnabled = extent.loginEnabled.value
                }
            }

        behavior()
            .supplies(loggingIn)
            .demands(loginClick, loginComplete)
            .runs {
                if (loginClick.justUpdated && loginEnabled.traceValue) {
                    loggingIn.update(true)
                } else if (loginComplete.justUpdated && loggingIn.value) {
                    loggingIn.update(false)
                }

                if (loggingIn.justUpdatedTo(true)) {
                    sideEffect("login api call") { extent ->
                    }
                }
            }

        behavior()
            .demands(loggingIn, loginComplete, didAdd)
            .runs {
                sideEffect("login status") {
                    var status = ""
                    if (loggingIn.value) {
                        status = "Logging in...";
                    } else if (loggingIn.justUpdatedTo(false)) {
                        if (loginComplete.justUpdated && loginComplete.value) {
                            status = "Login Success";
                        } else if (loginComplete.justUpdated && !loginComplete.value) {
                            status = "Login Failed";
                        }
                    }
                    loginActivityBG.loginStatusTextView.text = status

                }
            }
    }
}
