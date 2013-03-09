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
 * 2010/06/27
 */
package pt.up.fe.dceg.neptus.mp.actions;

import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;

import pt.up.fe.dceg.neptus.imc.IMCMessage;
import pt.up.fe.dceg.neptus.types.miscsystems.MiscSystems;
import pt.up.fe.dceg.neptus.types.miscsystems.MiscSystemsHolder;

/**
 * @author pdias
 *
 */
public class PlanActions {

	protected LinkedList<PayloadConfig> payloadConfigs = new LinkedList<PayloadConfig>();
	protected LinkedList<PlanActionElementConfig> actionMsgs = new LinkedList<PlanActionElementConfig>();
	
	/**
	 * 
	 */
	public PlanActions() {
	}
	
	/**
	 * @return the payloadConfigs
	 */
	public LinkedList<PayloadConfig> getPayloadConfigs() {
		return payloadConfigs;
	}
	
	/**
	 * @return the actionMsgs
	 */
	public LinkedList<PlanActionElementConfig> getActionMsgs() {
		return actionMsgs;
	}


	/**
	 * @param nd
	 */
	public boolean load(Element nd) {
		payloadConfigs.clear();
		List<?> lst = nd.selectNodes("./payload-config/payload");
		for (Object obj : lst) {
			Element pl = (Element) obj;
			PayloadConfig plcfg = new PayloadConfig();
            Node idNd = pl.selectSingleNode("./@id");
            if (idNd != null) {
                MiscSystems ms = MiscSystemsHolder.getMiscSystemsList().get(idNd.getText());
                plcfg.setBaseSystem(ms);
                plcfg.setXmlImcNode((Element) pl.selectSingleNode("child::*"));
                payloadConfigs.add(plcfg);
            }
            else { // FIXME Plan Actions: If no config use as message for now
                PlanActionElementConfig msgcfg = new PlanActionElementConfig();
                msgcfg.setXmlImcNode((Element) pl.selectSingleNode("child::*"));
                if (plcfg.message != null)
                    actionMsgs.add(msgcfg);
            }
		}

		lst = nd.selectNodes("./messages/child::*");
		for (Object obj : lst) {
			Element pl = (Element) obj;
			PlanActionElementConfig plcfg = new PlanActionElementConfig();
			plcfg.setXmlImcNode(pl);
			if (plcfg.message != null)
				actionMsgs.add(plcfg);
		}

		return true;
	}

	

    public Element asElement(String rootElementName) {
        return (Element) asDocument(rootElementName).getRootElement().detach();
    }
	
    public Document asDocument(String rootElementName) {
        Document document = DocumentHelper.createDocument();
		Element root = document.addElement(rootElementName);
        
		if (payloadConfigs.size() > 0) {
			Element pldcfgElement = root.addElement("payload-config");
			for (PayloadConfig plcfg : payloadConfigs) {
				Element ndcf = plcfg.getXmlNode();
//				System.out.println("PayloadConfig _________________\n"+ndcf.asXML());
				if (ndcf != null) {
					//ndcf.setName("payload");
					//pldcfgElement.add(ndcf.detach());
					Element payloadHolderElement = pldcfgElement.addElement("payload");
					payloadHolderElement.addAttribute("id", plcfg.getBaseSystem().getId());
					payloadHolderElement.add(((Node) ndcf.clone()).detach());
				}
			}
		}
		if (actionMsgs.size() > 0) {
			Element plActionsElement = root.addElement("messages");
			for (PlanActionElementConfig plcfg : actionMsgs) {
				Element ndcf = plcfg.getXmlNode();
//				System.out.println("ActionsConfig _________________\n"+ndcf.asXML());
				if (ndcf != null) {
					plActionsElement.add(((Node) ndcf.clone()).detach());
				}
			}
		}
//		System.out.println(document.asXML());
        
        return document;
    }


	/**
	 * @return
	 */
	public IMCMessage[] getAllMessages() {
		LinkedList<IMCMessage> msgs = new LinkedList<IMCMessage>();
		for (PayloadConfig plCfg : payloadConfigs) {
			msgs.add(plCfg.message);
		}
		for (PlanActionElementConfig msgConfig : actionMsgs) {
			msgs.add(msgConfig.message);
		}
		return msgs.toArray(new IMCMessage[msgs.size()]);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		String actStr =  "Plan Actions: ";
		for (PayloadConfig pc : payloadConfigs) {
			actStr += " P[" + pc.message.getAbbrev() +
					"]";
		}
		for (PlanActionElementConfig nm : actionMsgs) {
			actStr += " M[" + nm.message.getAbbrev() +
					"]";
		}
		return actStr;
	}


	/**
	 * @return
	 */
	public boolean isEmpty() {
		long count = getPayloadConfigs().size() + getActionMsgs().size(); 
		return (count == 0);
	}
    
//    public static  PlanActions showEditionDialog() {
//    	LocationPanel.showLocationDialog(parent, title, previousLocation, mt, editable)
//    	IMCMessage
//    	return this;
//    }
	
	/* (non-Javadoc)
	 * @see java.lang.Object#clone()
	 */
	@Override
	public Object clone() {
	    PlanActions clone = new PlanActions();
	    for (PayloadConfig pc : payloadConfigs)
            clone.payloadConfigs.add((PayloadConfig) pc.clone());
        for (PlanActionElementConfig am : actionMsgs)
            clone.actionMsgs.add((PlanActionElementConfig) am.clone());
	    return clone;
	}

    /**
     * @param actionsMessages
     */
    public void parseMessages(Vector<IMCMessage> actionsMessages) {
        // For now all are actionMsgs
        payloadConfigs.clear();
        actionMsgs.clear();
        for (IMCMessage msg : actionsMessages) {
            PlanActionElementConfig paec = new PlanActionElementConfig();
            paec.setMessage(msg);
            actionMsgs.add(paec);
        }
    }
}
