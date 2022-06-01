import com.yahoo.behaviorgraph.Graph;

import javax.swing.*;

public class CodeWalkthrough {
    JButton loginButton = new JButton();

    public static void main(String[] args) {
        Graph graph = new Graph();
        LoginForm loginForm = new LoginForm(graph);
        loginForm.addToGraphWithAction();
    }

    public void configureUI() {
        Graph graph = new Graph();
        LoginForm loginForm = new LoginForm(graph);
        loginButton.addActionListener(e -> {
            graph.action(() -> {
                loginForm.loginClick.updateWithAction();
            });
        });
    }
}
