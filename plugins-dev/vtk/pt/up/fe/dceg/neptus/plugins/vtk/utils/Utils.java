/*
 * Copyright (c) 2004-2013 Universidade do Porto - Faculdade de Engenharia
 * Laboratório de Sistemas e Tecnologia Subaquática (LSTS)
 * All rights reserved.
 * Rua Dr. Roberto Frias s/n, sala I203, 4200-465 Porto, Portugal
 *
 * This file is part of Neptus, Command and Control Framework.
 *
 * Commercial Licence Usage
 * Licencees holding valid commercial Neptus licences may use this file
 * in accordance with the commercial licence agreement provided with the
 * Software or, alternatively, in accordance with the terms contained in a
 * written agreement between you and Universidade do Porto. For licensing
 * terms, conditions, and further information contact lsts@fe.up.pt.
 *
 * European Union Public Licence - EUPL v.1.1 Usage
 * Alternatively, this file may be used under the terms of the EUPL,
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
 * included in the packaging of this file. You may not use this work
 * except in compliance with the Licence. Unless required by applicable
 * law or agreed to in writing, software distributed under the Licence is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific
 * language governing permissions and limitations at
 * https://www.lsts.pt/neptus/licence.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: hfq
 * Jun 4, 2013
 */
package pt.up.fe.dceg.neptus.plugins.vtk.utils;

import java.awt.Component;
import java.awt.event.ComponentEvent;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.SwingUtilities;

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.mra.NeptusMRA;
import vtk.vtkCanvas;
import vtk.vtkNativeLibrary;
import vtk.vtkObjectBase;
import vtk.vtkVersion;

/**
 * @author hfq
 * 
 */
public class Utils {

    private final static Logger LOGGER = Logger.getLogger(Utils.class.getName());

    public static void loadVTKLibraries() {

        try {
            System.loadLibrary("jawt");
        }
        catch (Throwable e) {
            NeptusLog.pub().info(I18n.text("<###> cannot load jawt lib!"));
        }
        // for simple visualizations
        try {
            vtkNativeLibrary.COMMON.LoadLibrary();
        }
        catch (Throwable e) {
            NeptusMRA.vtkEnabled = false;
            NeptusLog.pub().info(I18n.text("<###> cannot load vtkCommon, skipping..."));
        }
        try {
            vtkNativeLibrary.FILTERING.LoadLibrary();
        }
        catch (Throwable e) {
            NeptusMRA.vtkEnabled = false;
            NeptusLog.pub().info(I18n.text("<###> cannot load vtkFiltering, skipping..."));
        }
        try {
            vtkNativeLibrary.IO.LoadLibrary();
        }
        catch (Throwable e) {
            NeptusMRA.vtkEnabled = false;
            NeptusLog.pub().info(I18n.text("<###> cannot load vtkImaging, skipping..."));
        }
        try {
            vtkNativeLibrary.GRAPHICS.LoadLibrary();
        }
        catch (Throwable e) {
            NeptusMRA.vtkEnabled = false;
            NeptusLog.pub().info(I18n.text("<###> cannot load vtkGrahics, skipping..."));
        }
        try {
            vtkNativeLibrary.RENDERING.LoadLibrary();
        }
        catch (Throwable e) {
            NeptusMRA.vtkEnabled = false;
            NeptusLog.pub().info(I18n.text("<###> cannot load vtkRendering, skipping..."));
        }

        // Other
        try {
            vtkNativeLibrary.INFOVIS.LoadLibrary();
        }
        catch (Throwable e) {
            NeptusMRA.vtkEnabled = false;
            NeptusLog.pub().info(I18n.text("<###> cannot load vtkInfoVis, skipping..."));
        }
        try {
            vtkNativeLibrary.VIEWS.LoadLibrary();
        }
        catch (Throwable e) {
            NeptusMRA.vtkEnabled = false;
            NeptusLog.pub().info(I18n.text("<###> cannot load vtkViews, skipping..."));
        }
        try {
            vtkNativeLibrary.WIDGETS.LoadLibrary();
        }
        catch (Throwable e) {
            NeptusMRA.vtkEnabled = false;
            NeptusLog.pub().info(I18n.text("<###> cannot load vtkWidgets, skipping..."));
        }
        try {
            vtkNativeLibrary.GEOVIS.LoadLibrary();
        }
        catch (Throwable e) {
            NeptusMRA.vtkEnabled = false;
            NeptusLog.pub().info(I18n.text("<###> cannot load vtkGeoVis, skipping..."));
        }
        try {
            vtkNativeLibrary.CHARTS.LoadLibrary();
        }
        catch (Throwable e) {
            NeptusMRA.vtkEnabled = false;
            NeptusLog.pub().info(I18n.text("<###> cannot load vtkCharts, skipping..."));
        }
        // FIXME not loading vtkHybrid ?!
        try {
            vtkNativeLibrary.HYBRID.LoadLibrary();
        }
        catch (Throwable e) {
            // NeptusMRA.vtkEnabled = false;
            NeptusLog.pub().warn(I18n.text("cannot load vtkHybrid, skipping..."));
        }
        try {
            vtkNativeLibrary.VOLUME_RENDERING.LoadLibrary();
        }
        catch (Throwable e) {
            NeptusMRA.vtkEnabled = false;
            NeptusLog.pub().info(I18n.text("<###> cannot load vtkVolumeRendering, skipping..."));
        }

        NeptusLog.pub().info("Vtk source version: " + new vtkVersion().GetVTKSourceVersion());
    }

    public static void goToAWTThread(Runnable runnable) {
        if (!SwingUtilities.isEventDispatchThread()) {
            // Thread.dumpStack();
            // NeptusLog.pub().info("you try to render on a different thread than the thread" +
            // "that creates the renderView. Making an invokeLater to render on the " +
            // "thread that creates the renderView");
            try {
                SwingUtilities.invokeAndWait(runnable);
            }
            catch (InterruptedException ex) {
                ex.printStackTrace();
                LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
            }
            catch (InvocationTargetException ex) {
                ex.printStackTrace();
                LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
            }
        }
        else
            runnable.run();
    }
    
    public static void delete(vtkObjectBase o) {
        VTKMemoryManager.delete(o);
    }

    public static vtkCanvas retrieveCanvas(ComponentEvent e) {
        Component c = e.getComponent();
        if (c instanceof vtkCanvas)
            return (vtkCanvas) c;
        else
            throw new NoSuchElementException("Found " + c.getClass() + " when " + vtkCanvas.class + " expected.");
    }

    public static boolean isMeshCoherent(float[] points, int[] indices) {
        boolean[] flags = new boolean[points.length / 3];

        // Init
        Arrays.fill(flags, false);

        for (int i = 0; i < indices.length;) {
            // The number of points of the polygon
            int nb = indices[i];

            // Check out of bound
            if (i + nb >= indices.length)
                return false;

            // Explore de polygon
            for (int j = i + 1; j < i + nb + 1; ++j)
                // Check out of bound
                if (indices[j] >= points.length)
                    return false;
                else
                    flags[indices[j]] = true;
            i = i + nb + 1;
        }

        for (boolean flag : flags)
            if (!flag)
                return false;
        return true;
    }

    public static boolean intToBoolean(int value) {
        return value != 0;
    }

    public static int booleanToInt(boolean value) {
        return (value) ? 1 : 0;
    }
}
