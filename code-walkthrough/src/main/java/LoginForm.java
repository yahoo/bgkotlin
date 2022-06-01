import com.yahoo.behaviorgraph.*;

public class LoginForm extends Extent<LoginForm> {
    State<Boolean> loginEnabled = state(false);
    State<String> email = state("");
    State<String> password = state("");
    Moment loginClick = moment();
    State<Boolean> loggingIn = state(false);

    public LoginForm(Graph graph) {
        super(graph);

        behavior()
            .supplies(loggingIn)
            .demands(loginClick)
            .runs(ext -> {
                if (loginClick.justUpdated() && !this.loggingIn.value()) {
                    loggingIn.update(true);
                }
            });

        behavior()
            .supplies(loginEnabled)
            .demands(email, password, loggingIn)
            .runs(ext -> {
                boolean emailValid = validateEmail(email.value());
                boolean passwordValid = password.value().length() > 0;
                boolean enabled = emailValid && passwordValid && !loggingIn.value();
                loginEnabled.update(enabled);

                sideEffect(ext1 -> {
                    loginButton.setEnabled(loginEnabled.value());
                });
            });
    }

    private boolean validateEmail(String email) {
        // ... validate email code goes here
    }
}

