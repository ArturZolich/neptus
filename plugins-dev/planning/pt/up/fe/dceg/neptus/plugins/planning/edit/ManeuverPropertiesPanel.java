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
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
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
 * Author: José Pinto
 * Nov 30, 2011
 */
package pt.up.fe.dceg.neptus.plugins.planning.edit;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.border.TitledBorder;
import javax.swing.undo.UndoManager;

import pt.up.fe.dceg.neptus.gui.PropertiesEditor;
import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.mp.Maneuver;
import pt.up.fe.dceg.neptus.mp.maneuvers.Goto;
import pt.up.fe.dceg.neptus.renderer2d.StateRendererInteraction;
import pt.up.fe.dceg.neptus.types.mission.plan.PlanType;
import pt.up.fe.dceg.neptus.util.GuiUtils;
import pt.up.fe.dceg.neptus.util.ImageUtils;

import com.l2fprod.common.propertysheet.Property;
import com.l2fprod.common.propertysheet.PropertySheetPanel;

/**
 * @author zp
 *
 */
public class ManeuverPropertiesPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    protected PropertySheetPanel propsPanel = new PropertySheetPanel();
    protected JPanel controls = new JPanel();
    protected JToggleButton editBtn = new JToggleButton(I18n.text("Edit"),  ImageUtils.getScaledIcon("pt/up/fe/dceg/neptus/plugins/planning/images/man_edit.png", 16, 16));
    protected JButton deleteBtn = new JButton(I18n.text("Delete"), ImageUtils.getScaledIcon("pt/up/fe/dceg/neptus/plugins/planning/images/man_remove.png", 16, 16));
    protected Maneuver maneuver;
    protected String beforeXml = null;
    protected boolean changed = false;
    protected UndoManager manager = new UndoManager();
    protected PlanType plan;
    
    public ManeuverPropertiesPanel() {
        setLayout(new BorderLayout());     
        setBorder(new TitledBorder(I18n.text("No maneuver selected")));
        add(propsPanel, BorderLayout.CENTER);
        controls.setLayout(new GridLayout(1, 0));        
        controls.add(deleteBtn);
        controls.add(editBtn);
        editBtn.setEnabled(false);
        deleteBtn.setEnabled(false);
        
        add(controls, BorderLayout.SOUTH);
        
        propsPanel.setDescriptionVisible(true);
        propsPanel.addPropertySheetChangeListener(new PropertyChangeListener() {            
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                setProps();
            }
        });
       
        propsPanel.setEditorFactory(PropertiesEditor.getPropertyEditorRegistry());    
        propsPanel.setRendererFactory(PropertiesEditor.getPropertyRendererRegistry());    
        propsPanel.setToolBarVisible(false);        
    }


    /**
     * @return the maneuver
     */
    public Maneuver getManeuver() {
        return maneuver;
    }

    public void setProps() {
        
        String before = maneuver.getManeuverXml();        
        maneuver.setProperties(propsPanel.getProperties());
        
        if (manager != null)
            manager.addEdit(new ManeuverChanged(maneuver, plan, before));
        changed = true;
    }

    public void setManeuver(Maneuver man) {
        
        if (this.maneuver != null) {
            
            if (propsPanel.getTable().getEditorComponent() != null) {
                propsPanel.getTable().commitEditing();
                setProps();
            }
            
        }
        this.maneuver = man;
        editBtn.setSelected(false);
        changed = false;
        if (man == null) {
            setBorder(new TitledBorder(I18n.text("No maneuver selected")));
            propsPanel.setProperties(new Property[0]);
            editBtn.setEnabled(false);
            deleteBtn.setEnabled(false);
            return;
        }
        
        this.beforeXml = man.getManeuverXml();
        propsPanel.setProperties(man.getProperties());
        setBorder(new TitledBorder(man.getId()));
        
        deleteBtn.setEnabled(true);
        editBtn.setSelected(false);
        
        if (maneuver instanceof StateRendererInteraction)               
            editBtn.setEnabled(true);
        else
            editBtn.setEnabled(false);
    }
    
    @Override
    public Dimension getPreferredSize() {
        return new Dimension(200, 200);
    }
    
    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }
    
    /**
     * @return the changed
     */
    public boolean isChanged() {
        return changed;
    }
    
    /**
     * @return the beforeXml
     */
    public String getBeforeXml() {
        return beforeXml;
    }
    
    /**
     * @return the editBtn
     */
    public JToggleButton getEditBtn() {
        return editBtn;
    }

    /**
     * @return the deleteBtn
     */
    public JButton getDeleteBtn() {
        return deleteBtn;
    }

    /**
     * @return the manager
     */
    public UndoManager getManager() {
        return manager;
    }

    /**
     * @param manager the manager to set
     */
    public void setManager(UndoManager manager) {
        this.manager = manager;
    }

    /**
     * @return the plan
     */
    public PlanType getPlan() {
        return plan;
    }

    /**
     * @param plan the plan to set
     */
    public void setPlan(PlanType plan) {
        this.plan = plan;
    }

    public static void main(String[] args) {
        Goto gotoM = new Goto();
        ManeuverPropertiesPanel props = new ManeuverPropertiesPanel();
        GuiUtils.testFrame(props);
        props.setManeuver(gotoM);
    }
}
