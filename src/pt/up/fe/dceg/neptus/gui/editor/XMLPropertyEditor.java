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
 * Author: Paulo Dias
 * 5/10/2010
 */
package pt.up.fe.dceg.neptus.gui.editor;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dialog.ModalityType;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.Vector;

import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.Validator;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import pt.up.fe.dceg.neptus.fileeditor.SyntaxDocument;
import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.plugins.containers.GroupLayoutContainer;
import pt.up.fe.dceg.neptus.util.GuiUtils;
import pt.up.fe.dceg.neptus.util.StreamUtil;
import pt.up.fe.dceg.neptus.util.XMLUtil;

import com.l2fprod.common.beans.editor.AbstractPropertyEditor;

/**
 * @author pdias
 *
 */
public class XMLPropertyEditor extends AbstractPropertyEditor {

	protected String title = "XML";
	protected String rootElement = "";
	
	protected JButton button = new JButton(I18n.text("edit"));
	protected String xmlStr = "";
	
	protected String helpText = I18n.text("Container using GroupLayout\n"
			+ "(see 'http://download.oracle.com/javase/"
			+ "tutorial/uiswing/layout/group.html'\n"
			+ "and 'http://download.oracle.com/javase/"
			+ "tutorial/uiswing/layout/groupExample.html')\n"
			+ "\n\nDefinition:\n===========\n\n");
    protected String contentType = "text/plain";

	protected String smallMsg = I18n.text("This follows the Java Group Layout");
	
	/**
	 * 
	 */
	public XMLPropertyEditor() {
		initialize();
	}
	
	/**
	 * 
	 */
	private void initialize() {
		editor = new JPanel(new BorderLayout(0,0));
		((JPanel)editor).add(button, BorderLayout.CENTER);
		
		button.addActionListener(new ActionListener() {
			//boolean userDone = false;
			boolean userCancel = false;

			public void actionPerformed(ActionEvent e) {
				final String oldXmlStr = xmlStr;
				final JEditorPane editorPane = SyntaxDocument.getXmlEditorPane();
				JScrollPane jScrollPane = new JScrollPane();
				jScrollPane.setViewportView(editorPane);
			    jScrollPane.setVisible(true);
			    try {
			    	if (!"".equalsIgnoreCase(xmlStr)) {
			    		xmlStr = XMLUtil.getAsCompactFormatedXMLString(xmlStr);
			    		xmlStr = XMLUtil.getAsPrettyPrintFormatedXMLString(xmlStr);
			    	}
				} catch (Exception e1) {
					xmlStr = oldXmlStr;
				}
			    editorPane.setText(xmlStr.trim());
				final JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(editor));
				dialog.setTitle(title);
				dialog.setSize(800, 600);
				dialog.setLayout(new BorderLayout());
				dialog.setModalityType(ModalityType.DOCUMENT_MODAL);
				dialog.setAlwaysOnTop(true);
				GuiUtils.centerOnScreen(dialog);
				dialog.setResizable(true);
				dialog.setAlwaysOnTop(true);
				//dialog.add(jScrollPane, BorderLayout.CENTER);
				JScrollPane jScrollPane1 = new JScrollPane();
				JEditorPane help = new JEditorPane();
		        help.setEditable(false);
                help.setContentType(contentType);
		        help.setBackground(new Color(255, 255, 160));
		        help.setText(helpText);
		        help.setCaretPosition(0);
//		        help.addHyperlinkListener(this);

//				JTextArea help = new JTextArea(helpText);
//				help.setEditable(false);
				
				jScrollPane1.setViewportView(help);
			    jScrollPane1.setVisible(true);
			    //dialog.add(jScrollPane1, BorderLayout.WEST);
				JSplitPane splitPane = new JSplitPane(
						JSplitPane.HORIZONTAL_SPLIT, jScrollPane, jScrollPane1);
				splitPane.setOneTouchExpandable(true);
				splitPane.setDividerLocation((int)(dialog.getWidth()*0.9));
				dialog.add(splitPane, BorderLayout.CENTER);
				JPanel toolbar = new JPanel();
				toolbar.setLayout(new BorderLayout());
				JPanel buttons = new JPanel();
				//buttons.setLayout(new FlowLayout());
				GroupLayout layout = new GroupLayout(buttons);
				buttons.setLayout(layout);
				layout.setAutoCreateGaps(true);
				layout.setAutoCreateContainerGaps(true);
				
				JButton okButton = new JButton(I18n.text("OK"));
				okButton.addActionListener(new ActionListener(){
					public void actionPerformed(ActionEvent arg0) {
						String[] vmsgs = validateLayoutXML(editorPane.getText());
						if (vmsgs.length == 0) {
							dialog.setVisible(false);
							dialog.dispose();
							userCancel = false;
							//userDone = true;
						}
						else {
							String strMsg = I18n.text("Invalid XML!") + "\n";
							for (String str : vmsgs)
								strMsg += "\n" + str;
							GuiUtils.infoMessage(dialog, I18n.text("Validation"), strMsg);
						}
					}
				});
				
				JButton cancelButton = new JButton(I18n.text("Cancel"));
				cancelButton.addActionListener(new ActionListener(){
					public void actionPerformed(ActionEvent arg0) {
						dialog.setVisible(false);
						dialog.dispose();
						userCancel = true;
						//userDone = true;
					}
				});
				GuiUtils.reactEscapeKeyPress(cancelButton);
				
				JButton validateButton = new JButton(I18n.text("Validate"));
				validateButton.addActionListener(new ActionListener(){
					public void actionPerformed(ActionEvent arg0) {
						String[] vmsgs = validateLayoutXML(editorPane.getText());
						if (vmsgs.length == 0) {
							GuiUtils.infoMessage(dialog, I18n.text("Validation"), I18n.text("Valid XML."));
						}
						else {
							String strMsg = I18n.text("Invalid XML!") + "\n";
							for (String str : vmsgs)
								strMsg += "\n" + str;
							GuiUtils.infoMessage(dialog, I18n.text("Validation"), strMsg);
						}
					}
				});
				
				JButton extractSchemaButton = new JButton(I18n.text("Extract Schema"));
				extractSchemaButton.addActionListener(new ActionListener(){
					public void actionPerformed(ActionEvent arg0) {
						try {
							InputStream sstream = GroupLayoutContainer.class
									.getResourceAsStream(GroupLayoutContainer.GROUP_LAYOUT_SCHEMA);
							File fx = new File(GroupLayoutContainer.GROUP_LAYOUT_SCHEMA);
							fx = new File(fx.getName());
							fx.createNewFile();
							StreamUtil.copyStreamToFile(sstream, fx);
                            GuiUtils.infoMessage(dialog, I18n.text("Extract Schema"),
                                    I18n.text("Schema extracted to file:") + "\n" + fx.getAbsolutePath());
						} catch (Exception e) {
                            GuiUtils.errorMessage(dialog, I18n.text("Extract Schema"),
                                    I18n.text("Error while extracting schema to file!!") + "\n" + e.getMessage());
						}
					}
				});
                // If schema doesn't exist disable validation and extraction button
                if (getSchema() == null) {
                    validateButton.setEnabled(false);
                    extractSchemaButton.setEnabled(false);
                }
                
				JLabel label = new JLabel(smallMsg);
				layout.setHorizontalGroup(layout.createSequentialGroup()
						.addComponent(extractSchemaButton)
						.addComponent(validateButton)
						.addComponent(label)
						.addComponent(okButton)
						.addComponent(cancelButton));
				layout.setVerticalGroup(layout.createParallelGroup()
						.addComponent(extractSchemaButton)
						.addComponent(validateButton)
						.addComponent(label)
						.addComponent(okButton)
						.addComponent(cancelButton));
				layout.linkSize(SwingConstants.HORIZONTAL, validateButton,
						okButton, cancelButton, extractSchemaButton);
				toolbar.add(buttons,BorderLayout.EAST);
				dialog.add(toolbar,BorderLayout.SOUTH);
				dialog.addWindowListener(new WindowAdapter() {
					public void windowClosing(WindowEvent e) {
						userCancel = true;
						//userDone = true;
					}
				});
				dialog.setVisible(true);
				if (!userCancel) {
					String tmpStr = editorPane.getText(); 
					try {
						if (!"".equalsIgnoreCase(tmpStr)) {
							tmpStr = XMLUtil.getAsCompactFormatedXMLString(tmpStr);
						}
					} catch (Exception e1) {
					}
					setValue(tmpStr);
					firePropertyChange(oldXmlStr, tmpStr);
				}
				else {
					xmlStr = oldXmlStr;
				}
				
//				SwingWorker<String, Void> sworker = new SwingWorker<String, Void>() {
//
//					@Override
//					protected String doInBackground() throws Exception {
//						publish();
//						if (userDone && ! userCancel) {
//							setValue(editorPane.getText());
//							XMLPropertyEditor.this.firePropertyChange(oldXmlStr, editorPane.getText());
//						}
//						return null;
//					}
//					
//					@Override
//					protected void process(List<Void> chunks) {
//						dialog.setVisible(true);
//					}
//				};
//				sworker.execute();
			}
		});
	}

	public Object getValue() {
		return xmlStr;
	}
	
	public void setValue(Object arg0) {
		if (arg0 instanceof String) {
			xmlStr = (String)arg0;
		}
	}
	
	private String[] validateLayoutXML(String strXml) {
		ByteArrayInputStream bais = new ByteArrayInputStream(
				("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
						((rootElement!=null && !"".equalsIgnoreCase(rootElement))?"<"+rootElement+">":"") +
						strXml.trim() + 
						((rootElement!=null && !"".equalsIgnoreCase(rootElement))?"</"+rootElement+">":""))
						.getBytes());
		
		final Vector<String> validationMsgs = new Vector<String>();
        
		if (getSchema() == null)
		    return new String[0];
        
		Validator validator = getSchema().newValidator();
		validator.setErrorHandler(new ErrorHandler() {
			@Override
			public void warning(SAXParseException exception) throws SAXException {
				validationMsgs.add("WARNING: " + exception.getMessage());
			}
			@Override
			public void error(SAXParseException exception) throws SAXException {
				validationMsgs.add("ERROR: " + exception.getMessage());
			}
			@Override
			public void fatalError(SAXParseException exception) throws SAXException {
				validationMsgs.add("FATAL: " + exception.getMessage());
			}
		});
		try {
			validator.validate(new StreamSource(bais));
		} catch (Exception e) {
			//validationMsgs.add("SOURCE: " + e.getMessage());
		}
		return validationMsgs.toArray(new String[validationMsgs.size()]);
	}
	
	public Schema getSchema() {
	    return null;
	}
	
	public static void main(String[] args) {
        XMLPropertyEditor xp = new XMLPropertyEditor();
        
        GuiUtils.testFrame(xp.button);
    }
}
