import com.yahoo.behaviorgraph.Graph;

public class Tutorial {
    public static void main(String[] args) {
        //Schedule a job for the event-dispatching thread:
        //creating and showing this application's GUI.
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            ListUI ui;
            ListExtent list;
            Graph graph;
            public void run() {
                ui = new ListUI();
                graph = new Graph();
                list = new ListExtent(graph, ui);
                list.addToGraphWithAction();
                ui.createAndShowGUI();
            }
        });
    }
}
