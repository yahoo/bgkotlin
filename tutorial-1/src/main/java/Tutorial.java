import com.yahoo.behaviorgraph.*;

import java.util.List;

public class Tutorial {
    public static void main(String[] args) {
        Graph g = new Graph();
        TutorialExtent e = new TutorialExtent(g);

        e.addToGraphWithAction();

        g.action(() -> {
            e.person.update("World");
            e.greeting.update("Hello");
        });
        g.action(() -> {
            e.greeting.update("Goodbye");
        });
        g.action(() -> {
            e.button.update();
            e.greeting.update("Nevermind");
            e.loggingEnabled.update(false);
        });
    }
}

class TutorialExtent extends Extent<TutorialExtent> {

    State<String> person = state("Nobody");
    State<String> greeting = state("Greetings");
    Moment button = moment();
    State<String> message = state(null);
    Moment sentMessage = moment();
    State<Boolean> loggingEnabled = state(true);

    public TutorialExtent(Graph g) {
        super(g);

        behavior()
                .supplies(message, sentMessage)
                .demands(person, greeting, button)
                .runs(ctx -> {
                    message.update(greeting.value() + ", " + person.value() + "!");
                    if (button.justUpdated()) {
                        System.out.println(message.value());
                        sentMessage.update();
                    }
                });

        behavior()
                .demands(message, sentMessage, loggingEnabled)
                .runs(ctx -> {
                    if (loggingEnabled.value()) {
                        if (message.justUpdated()) {
                            System.out.println("Message changed to: " + message.value() + " : " + message.event().getTimestamp());
                        }
                        if (sentMessage.justUpdated()) {
                            System.out.println("Message sent: " + message.value() + " : " + message.event().getTimestamp());
                        }
                    }
                });
    }

}