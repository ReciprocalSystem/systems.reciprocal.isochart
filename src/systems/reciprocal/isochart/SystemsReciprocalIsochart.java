/*
 * Copyright (C) 2016 Bruce Peret
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package systems.reciprocal.isochart;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.stage.Stage;
import systems.reciprocal.Database;
import systems.reciprocal.Rs;
import systems.reciprocal.db.physics.Isotope;
import systems.reciprocal.db.physics.Nubase;

/**
 * Plot observed isotope mass data versus RS calculated.
 *
 * @author Bruce Peret
 */
public class SystemsReciprocalIsochart extends Application {

    public static final double IRR = 128. * (1 + 2. / 9.);
    public static final int MAG_IONIZATION = 1;

    @Override
    public void start(Stage stage) throws SQLException {

        stage.setTitle("RS: Zone of Isotopic Stability Calculation");
        //defining the axes
        final NumberAxis xAxis = new NumberAxis(0, 120, 4);
        final NumberAxis yAxis = new NumberAxis(0, 350, 10);
        xAxis.setLabel("Atomic Number (Z)");
        yAxis.setLabel("Mass (u)");
        
        /*
         * Create the chart.
         */
        final LineChart<Number, Number> lineChart
            = new LineChart<>(xAxis, yAxis);
        lineChart.setTitle("Observed v. Calculated Isotopic Mass");
        
        /*
         * Add actual isotope data.
         * This is formatted by chart.css to remove lines and act like
         * a scatter plot. This is the "0" series in chart.css.
         */
        PreparedStatement ps;

        /*
         * NIST data.
         */
        ps = Database.db.prepareStatement(
            "SELECT z,isotope FROM " + Isotope.TABLE
            + " ORDER BY z,isotope"
        );
        XYChart.Series nist = Isotope.xychart(ps);
        nist.setName("NIST Isotope Data");
        lineChart.getData().add(nist);
        
        /*
         * Reference lines, 1, 2, 3.
        */
        XYChart.Series ref = new XYChart.Series();
        ref.setName("Stability limit");
        ref.getData().add(new XYChart.Data(1,236));
        ref.getData().add(new XYChart.Data(92,236));
        ref.getData().add(new XYChart.Data(92,0));
        lineChart.getData().add(ref);
        ref = new XYChart.Series();
        ref.setName("Minimum Mass");
        ref.getData().add(new XYChart.Data(1,2));
        ref.getData().add(new XYChart.Data(118,2*118));
        lineChart.getData().add(ref);
        ref = new XYChart.Series();
        ref.setName("Maximum Mass");
        ref.getData().add(new XYChart.Data(1,3));
        ref.getData().add(new XYChart.Data(118,4*118-1));
        lineChart.getData().add(ref);
        
        /*
         * NIST data, (z,mass).
         */
        ps = Database.db.prepareStatement(
            "SELECT distinct z,standard_atomic_weight FROM " + Isotope.TABLE
            + " ORDER BY z"
        );
        nist = Isotope.xychart(ps);
        nist.setName("Standard Atomic Weight");
        lineChart.getData().add(nist);
        
        /*
         * Calculated zone of isotopic stability.
         * This is the "4" series in chart.css.
         */
        XYChart.Series series = new XYChart.Series();
        series.setName("Calculated Zone of Stability");
        for (int z = 1; z <= 118; z++) {
            /*
             * Calculated mass: 2Z + G (magnetic ionization = 1).
             * G = magnetic_ionization * z^2 / IRR.
             */
            double mass = 2 * z + MAG_IONIZATION * z * z / IRR;
            /*
             * Discrete units means floor() of mass value.
             * Fractional parts do not count.
             */
            series.getData().add(new XYChart.Data(z, Math.floor(mass)));
        }
        lineChart.getData().add(series);
        
        /*
        * Draw the graph, styled by chart.css.
         */
        Scene scene = new Scene(lineChart, 800, 600);
        scene.getStylesheets().add(getClass().getResource("chart.css").toExternalForm());
        stage.setScene(scene);
        stage.show();
    }

    /**
     * @param args the command line arguments
     * @throws java.io.IOException
     * @throws java.sql.SQLException
     */
    public static void main(String[] args) throws IOException, SQLException {
        Rs.factory();   // RS runtime environment (static)
        launch(args);
    }

}
