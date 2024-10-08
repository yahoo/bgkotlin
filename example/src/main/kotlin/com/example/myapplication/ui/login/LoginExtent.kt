//
// Copyright Yahoo 2021
//
package com.example.myapplication.ui.login

import android.text.TextUtils
import android.util.Patterns
import behaviorgraph.Extent
import behaviorgraph.Graph

class LoginExtent(var loginActivityBG: LoginActivityBG, graph: Graph) : Extent<LoginExtent>(graph) {
    val email = state("")
    val password = state("")
    val emailValid = state(false)
    val passwordValid = state(false)
    val loginEnabled = state(false)
    val loggingIn = state(false)
    val loginClick = moment()
    val loginComplete = typedMoment<Boolean>()

    init {
        behavior()
            .supplies(emailValid)
            .demands(email, didAdd)
            .runs {
                emailValid.update(validateEmail(email.value))
                sideEffect(null) {
                    loginActivityBG.emailFeedbackTextView.text =
                        if (emailValid.value) {
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
                        if (loginComplete.value == true) {
                            status = "Login Success";
                        } else if (loginComplete.value == false) {
                            status = "Login Failed";
                        }
                    }
                    loginActivityBG.loginStatusTextView.text = status

                }
            }
    }

    private fun validateEmail(target: CharSequence?): Boolean {
        return !TextUtils.isEmpty(target) && Patterns.EMAIL_ADDRESS.matcher(target).matches()
    }

}
