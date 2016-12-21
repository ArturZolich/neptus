/*
 * Copyright (c) 2004-2017 Universidade do Porto - Faculdade de Engenharia
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
 * http://ec.europa.eu/idabc/eupl.html.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: hfq
 * Apr 16, 2013
 */
package pt.lsts.neptus.vtk.visualization;

import vtk.vtkBoxRepresentation;
import vtk.vtkBoxWidget;
import vtk.vtkBoxWidget2;
import vtk.vtkDataSet;
import vtk.vtkRenderWindowInteractor;
import vtk.vtkRenderer;

/**
 * @author hfq
 * 
 */
public class BoxWidget {

    /**
     * 
     * @param ren
     * @param interactor
     * @param dataSet
     */
    public static void addBoxWidget1ToVisualizer(vtkRenderer ren, vtkRenderWindowInteractor interactor,
            vtkDataSet dataSet) {
        vtkBoxWidget widget = new vtkBoxWidget();

        widget.SetCurrentRenderer(ren);
        widget.SetInteractor(interactor);
        widget.SetPlaceFactor(1.25);
        widget.PlaceWidget();
        widget.SetInput(dataSet.GetData(null)); // não está bem penso eu

        widget.EnabledOn();
    }

    /**
     * 
     * @param ren
     * @param interactor
     */
    public static void addBoxWidget2Tovisualizer(vtkRenderer ren, vtkRenderWindowInteractor interactor) {
        vtkBoxWidget2 widget = new vtkBoxWidget2();

        vtkBoxRepresentation boxrep = new vtkBoxRepresentation();
        boxrep.SetPlaceFactor(1.25);
        widget.SetRepresentation(boxrep);
        widget.SetCurrentRenderer(ren);
        widget.SetInteractor(interactor);

        widget.EnabledOn();
    }
}
