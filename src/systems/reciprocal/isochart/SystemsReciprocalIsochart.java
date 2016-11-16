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

/**
 * Plot observed isotope mass data versus RS calculated.
 *
 * @author Bruce Peret
 */
public class SystemsReciprocalIsochart extends Application {

    int magnetic_ionization_level = 1;

    /**
     * Add actual NIST isotope data.
     *
     * This is formatted by chart.css to remove lines and act like a scatter
     * plot. This is the "0" series in chart.css.
     *
     * @return
     * @throws java.sql.SQLException
     */
    XYChart.Series nist_data() throws SQLException {
        PreparedStatement ps = Database.db.prepareStatement(
            "SELECT z,isotope FROM " + Isotope.TABLE
            + " ORDER BY z,isotope"
        );
        XYChart.Series nist = Isotope.xychart(ps);
        nist.setName("NIST Isotope Data");
        return nist;
    }

    /**
     * Reference lines for mass limits.
     *
     * @param ion
     * @return
     */
    XYChart.Series mass_limit(int ion) {
        XYChart.Series ref = new XYChart.Series();
        int z = Rs.unstable_element(ion);
        ref.setName("Stability limit");
        ref.getData().add(new XYChart.Data(1, Rs.MASS_LIMIT));
        ref.getData().add(new XYChart.Data(z, Rs.MASS_LIMIT));
        ref.getData().add(new XYChart.Data(z, 0));
        return ref;
    }

    /**
     * Minimum mass for an element.
     *
     * Calculated by the rotational mass, since you cannot have a fraction of a
     * rotation. min_mass = 2z.
     *
     * @return
     */
    XYChart.Series minimum_mass() {
        XYChart.Series ref = new XYChart.Series();
        ref.setName("Minimum Mass");
        ref.getData().add(new XYChart.Data(1, 2));
        ref.getData().add(new XYChart.Data(118, 2 * 118));
        return ref;
    }

    /**
     * Maximum mass for an element.
     *
     * The maximum mass is reached when the vibrational mass (the gravitational
     * charge) cancels the atomic rotation. Since 2 vibrations = 1 rotation, and
     * 2 amu = 1 rotation, it is at 4z-1 (4z starts disintegration).
     *
     * Note that mass can exceed this, but destroys rotational structure and
     * changes the atomic number in compensation.
     *
     * @return
     */
    XYChart.Series maximum_mass() {
        XYChart.Series ref = new XYChart.Series();
        ref.setName("Maximum Mass");
        ref.getData().add(new XYChart.Data(1, 3));
        ref.getData().add(new XYChart.Data(Rs.Z_LIMIT, 4 * Rs.Z_LIMIT - 1));
        return ref;
    }

    /**
     * Grab the standard atomic weight from the NIST table.
     *
     * @return
     * @throws SQLException
     */
    XYChart.Series standard_weight() throws SQLException {
        PreparedStatement ps = Database.db.prepareStatement(
            "SELECT distinct z,standard_atomic_weight FROM "
            + Isotope.TABLE
            + " ORDER BY z"
        );
        XYChart.Series nist = Isotope.xychart(ps);
        nist.setName("Standard Atomic Weight");
        return nist;
    }

    XYChart.Series zone_of_stability(int ion) {
        XYChart.Series series = new XYChart.Series();
        series.setName("Calculated Zone of Stability");
        for (int z = 1;
            z <= Rs.Z_LIMIT; z++) {
            series.getData().add(
                new XYChart.Data(
                    z,
                    Math.floor(Rs.standard_mass(z, ion)
                    )
                )
            );
        }
        return series;
    }

    @Override
    public void start(Stage stage) throws SQLException {
        // Minor tick marks
        final int xspacing = 4;
        final int yspacing = 10;
        /*
         * Fix the axis sizes so we get a little extra space before
         * and after the atomic number, and don't overshoot the mass
         * because of the calculated zone of stability curve at 118.
         */
        stage.setTitle("RS: Zone of Isotopic Stability Calculation");
        final NumberAxis xAxis = new NumberAxis("Atomic Number (Z)", 0, 120, xspacing);
        final NumberAxis yAxis = new NumberAxis("Mass (u)", 0, 350, yspacing);
        xAxis.setMinorTickCount(xspacing);
        yAxis.setMinorTickCount(yspacing/2);
        /*
         * Create the chart.
         */
        final LineChart<Number, Number> lineChart
            = new LineChart<>(xAxis, yAxis);
        lineChart.setTitle("Observed v. Calculated Isotopic Mass");
        lineChart.getData().add(nist_data());
        lineChart.getData().add(mass_limit(magnetic_ionization_level));
        lineChart.getData().add(minimum_mass());
        lineChart.getData().add(maximum_mass());
        lineChart.getData().add(standard_weight());
        lineChart.getData().add(zone_of_stability(magnetic_ionization_level));
        /*
        * Draw the graph, styled by chart.css.
         */
        Scene scene = new Scene(lineChart, 800, 600);
        scene.getStylesheets()
            .add(getClass().getResource("chart.css").toExternalForm());
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
