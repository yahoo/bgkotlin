import behaviorgraph.Graph;

public class Tutorial {
    public static void main(String[] args) {
        //Schedule a job for the event-dispatching thread:
        //creating and showing this application's GUI.
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            TutorialUI ui;
            Thermostat tm;
            Graph graph;
            public void run() {
                ui = new TutorialUI();
                graph = new Graph();
                tm = new Thermostat(graph, ui);
                tm.addToGraphWithAction();
                ui.createAndShowGUI();
            }
        });
    }
}
