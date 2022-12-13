//import com.yahoo.behaviorgraph.*;

import java.util.ArrayList;
import java.util.Collections;
import behaviorgraph.*;
class RootExtent extends Extent<RootExtent> {
  Moment root = moment();
  Moment addExtents = moment();
  Moment extentAdder = moment();
  Moment linkUpdater = moment();
  static int width = 300;
  static int depth = 300;
  Moment[][] bulkResources = new Moment[depth][width];
  ArrayList<Sub1Extent> subextents = new ArrayList<>();

  RootExtent(Graph g) {
    super(g);
    root = moment();
    extentAdder = moment();

    behavior()
      .supplies(extentAdder)
      .demands(addExtents)
      .runs(ctx -> {

        for (var i = 0; i < 3; i++) {
          var e = new Sub1Extent(g, root);
          e.addToGraph();
          subextents.add(e);
        }
        extentAdder.update();

      });


    behavior()
      .supplies(linkUpdater)
      .demands(extentAdder)
      .runs(ctx -> {

        // make all the behaviors in the first row of this extent
        // demand the resources in the first row of each subextent
        ArrayList<Moment> newDemands = new ArrayList<>();
        for (var ext: subextents) {
          Collections.addAll(newDemands, ext.bulkResources[0]);
        }
        for (var j = 0; j < width; j++) {
          bulkResources[0][j].getSuppliedBy().setDynamicDemands(newDemands);
        }

        // make each behavior in the last row of each subextent
        // demand each resource in the last row of this extent
        ArrayList<Moment> allSupplied = new ArrayList();
        Collections.addAll(allSupplied, bulkResources[depth - 1]);
        for (var ext: subextents) {
          for (var j = 0; j < ext.width; j++) {
            ext.bulkResources[ext.depth - 1][j].getSuppliedBy().setDynamicDemands(allSupplied);
          }
        }
      });

    ArrayList<Moment> nextDemands = new ArrayList();
    for (int i = 0; i < depth; i++) {
      nextDemands.add(root);
      nextDemands.add(linkUpdater);
      for (int j = 0; j < width; j++) {
        var newMoment = moment();
        bulkResources[i][j] = newMoment;
        behavior()
          .supplies(bulkResources[i][j])
          .demands(nextDemands)
          .runs(ctx -> {
            newMoment.update();
          });
      }
      nextDemands = new ArrayList();
      Collections.addAll(nextDemands, bulkResources[i]);
    }
  }

}

class Sub1Extent extends Extent<Sub1Extent> {
  Moment[][] bulkResources = new Moment[depth][width];
  static int width = 10;
  static int depth = 100;

  Sub1Extent(Graph g, Moment root) {
    super(g);

    ArrayList<Moment> nextDemands = new ArrayList();
    for (var i = 0; i < depth; i++) {
      for (var j = 0; j < width; j++) {
        var newMoment = moment();
        bulkResources[i][j] = newMoment;
        behavior()
          .supplies(bulkResources[i][j])
          .demands(nextDemands)
          .runs(ctx -> {
            newMoment.update();
          });
      }
      nextDemands = new ArrayList();
      Collections.addAll(nextDemands, bulkResources[i]);
    }
  }

}

public class PerfTest {
  public static void main(String[] args) {
    System.out.println("starting....");

    for (var i = 0; i < 1; i++) {
      var g = new Graph();
      var e = new RootExtent(g);
      long start = 0;
      long end = 0;

      // Add to graph
      start = System.nanoTime();
      e.addToGraphWithAction();
      end = System.nanoTime();
      System.out.printf("Add to graph: %1$d\n", (end - start) / 1000000);


      // Simple update
      start = System.nanoTime();
      e.root.updateWithAction();
      end = System.nanoTime();
      System.out.printf("Update root: %1$d\n", (end - start) / 1000000);

      // Add subextents
      start = System.nanoTime();
      g.action(() -> {
        e.root.update();
        e.addExtents.update();
      });
      end = System.nanoTime();
      System.out.printf("Add subextents: %1$d\n", (end - start) / 1000000);

      // Remove
      start = System.nanoTime();
      g.action(() -> {
        for (var ext: e.subextents) {
          ext.removeFromGraph();
        }
        e.removeFromGraph();
      });
      end = System.nanoTime();
      System.out.printf("Remove subextents: %1$d\n", (end - start) / 1000000);
    }
  }
}