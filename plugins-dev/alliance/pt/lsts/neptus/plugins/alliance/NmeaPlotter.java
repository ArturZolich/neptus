/*
 * Copyright (c) 2004-2016 Universidade do Porto - Faculdade de Engenharia
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
 * Author: zp
 * Jul 24, 2014
 */
package pt.lsts.neptus.plugins.alliance;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;

import javax.swing.JMenuItem;

import com.google.common.eventbus.Subscribe;

import de.baderjene.aistoolkit.aisparser.AISParser;
import de.baderjene.aistoolkit.aisparser.message.Message05;
import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;
import pt.lsts.imc.DevDataText;
import pt.lsts.imc.lsf.LsfMessageLogger;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.manager.imc.ImcMsgManager;
import pt.lsts.neptus.comm.manager.imc.ImcSystem;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.console.ConsoleLayer;
import pt.lsts.neptus.console.notifications.Notification;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.NeptusProperty.LEVEL;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.PluginUtils;
import pt.lsts.neptus.plugins.alliance.ais.CmreAisCsvParser;
import pt.lsts.neptus.plugins.update.Periodic;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.map.ScatterPointsElement;
import pt.lsts.neptus.types.vehicle.VehicleType.SystemTypeEnum;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.NMEAUtils;

/**
 * @author zp
 * @author pdias
 */
@PluginDescription(name = "NMEA Plotter", icon = "pt/lsts/neptus/plugins/alliance/nmea-ais.png")
public class NmeaPlotter extends ConsoleLayer {

    @NeptusProperty(name = "Connect to the serial port")
    public boolean serialListen = false;

    @NeptusProperty(name = "Serial Port Device")
    public String uartDevice = "/dev/ttyUSB0";

    @NeptusProperty(name = "Serial Port Baud Rate")
    public int uartBaudRate = 38400;

    @NeptusProperty(name = "Serial Port Data Bits")
    public int dataBits = 8;

    @NeptusProperty(name = "Serial Port Stop Bits")
    public int stopBits = 1;

    @NeptusProperty(name = "Serial Port Parity Bits")
    public int parity = 0;

    @NeptusProperty(name = "UDP port to bind")
    public int udpPort = 7878;

    @NeptusProperty(name = "Listen for incoming UDP packets")
    public boolean udpListen = true;

    @NeptusProperty(name = "Connect via TCP")
    public boolean tcpConnect = false;

    @NeptusProperty(name = "TCP Host")
    public String tcpHost = "127.0.0.1";

    @NeptusProperty(name = "TCP Port")
    public int tcpPort = 13000;

    @NeptusProperty(name = "Maximum age in for AIS contacts (seconds)")
    public int maximumAisAge = 600;

    @NeptusProperty(name = "Retransmit to other Neptus consoles", userLevel = LEVEL.ADVANCED)
    public boolean retransmitToNeptus = true;

    @NeptusProperty(name = "Log received data", userLevel = LEVEL.ADVANCED)
    public boolean logReceivedData = true;

    @NeptusProperty(name = "Number of track points", userLevel = LEVEL.ADVANCED)
    public int trackPoints = 100;

    private JMenuItem connectItem = null;
    private boolean connected = false;

    GeneralPath ship = new GeneralPath();
    {
        ship.moveTo(0, 1.0);
        ship.lineTo(1.0, 0.6);
        ship.lineTo(1.0, -1.0);
        ship.lineTo(-1.0, -1.0);
        ship.lineTo(-1.0, 0.6);
        ship.lineTo(0, 1.0);
    }

    private SerialPort serialPort = null;
    private DatagramSocket udpSocket = null;
    private Socket tcpSocket = null;
    
    private boolean isSerialConnected = false;
    private boolean isUdpConnected = false;
    private boolean isTcpConnected = false;

    private HashSet<NmeaListener> listeners = new HashSet<>();
    private AisContactDb contactDb = new AisContactDb();
    private AISParser parser = new AISParser();

    private LinkedHashMap<String, LocationType> lastLocs = new LinkedHashMap<>();
    private LinkedHashMap<String, ScatterPointsElement> tracks = new LinkedHashMap<>();

    @Periodic(millisBetweenUpdates = 5000)
    public void updateTracks() {
        for (AisContact c : contactDb.getContacts()) {
            LocationType l = c.getLocation();
            String name = c.getLabel();

            if (lastLocs.get(name) == null || !lastLocs.get(name).equals(l)) {
                if (!tracks.containsKey(name)) {
                    ScatterPointsElement sc = new ScatterPointsElement();
                    sc.setCenterLocation(l);
                    sc.setColor(Color.black, Color.gray.brighter());
                    sc.setNumberOfPoints(trackPoints);
                    tracks.put(name, sc);
                }
                tracks.get(name).addPoint(l);
            }
        }
    }

    private void connectToSerial() throws Exception {
        serialPort = new SerialPort(uartDevice);
        isSerialConnected = true;

        boolean opened = serialPort.openPort();
        if (!opened) {
            isSerialConnected = false;
            serialPort = null;
            throw new Exception("Unable to open port " + uartDevice);
        }

        serialPort.setParams(uartBaudRate, dataBits, stopBits, parity);
        serialPort.addEventListener(new SerialPortEventListener() {
            private String currentString = "";

            @Override
            public void serialEvent(SerialPortEvent serEvt) {
                try {
                    String s = serialPort.readString();
                    if (s == null|| s.isEmpty())
                        return; // If null nothing to do!
                    
                    if (s.contains("\n")) {
                        currentString += s.substring(0, s.indexOf('\n'));
                        processSentence();
                        currentString = s.substring(s.indexOf('\n') + 1);
                    }
                    else if (s.contains("$") || s.contains("!")) {
                        // For cases where the stream is not canonical (there is without new line at the end
                        if (s.contains("$"))
                            currentString += s.substring(0, s.indexOf('$'));
                        else
                            currentString += s.substring(0, s.indexOf('!'));
                        processSentence();
                        if (s.contains("$"))
                            currentString = s.substring(s.indexOf('$'));
                        else
                            currentString = s.substring(s.indexOf('!'));
                        // System.out.println(">>" + currentString);
                    }
                    else {
                        currentString += s;
                    }
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }

            private void processSentence() {
                if (!currentString.trim().isEmpty()) {
                    // System.out.println(">" + currentString);
                    if (hasNMEASentencePrefix(currentString)) {
                        for (NmeaListener l : listeners)
                            l.nmeaSentence(currentString.trim());
                    }
                    parseSentence(currentString);
                    if (retransmitToNeptus)
                        retransmit(currentString);
                    if (logReceivedData)
                        LsfMessageLogger.log(new DevDataText(currentString));
                }
            }
        });
        NeptusLog.pub().info("Listening to NMEA messages over serial \"" + serialPort + "\".");
        getConsole().post(Notification.success("NMEA Plotter", "Connected via serial to \"" + uartDevice + "\"."));
    }

    private boolean hasNMEASentencePrefix(String sentence) {
        return sentence.startsWith("$") || sentence.startsWith("!");
    }
    
    private void retransmit(String sentence) {
        DevDataText ddt = new DevDataText(sentence);
        for (ImcSystem s : ImcSystemsHolder.lookupSystemByType(SystemTypeEnum.CCU)) {
            ImcMsgManager.getManager().sendMessageToSystem(ddt, s.getName());
        }
    }

    @Subscribe
    public void on(DevDataText ddt) {
        parseSentence(ddt.getValue());
    }

    private void parseSentence(String s) {
        s = s.trim();
        if (hasNMEASentencePrefix(s)) {
            String nmeaType = NMEAUtils.nmeaType(s);
            if (nmeaType.equals("$B-TLL") || nmeaType.equals("$A-TLL"))
                contactDb.processBtll(s);
            else if (nmeaType.equals("$GPGGA"))
                contactDb.processGGA(s);
            else if (nmeaType.equals("$RATTM"))
                contactDb.processRattm(s);
            else if (nmeaType.equals("$GPHDT"))
                contactDb.processGPHDT(s);
            else {
                synchronized (parser) {
                    parser.process(s);
                }
            }
        }
        else {
            CmreAisCsvParser.process(s, contactDb);
        }
    }

    private void connect() throws Exception {
        connected = true;
        if (serialListen) {
            connectToSerial();
        }
        if (udpListen) {
            connectToUDP();
        }
        if (tcpConnect) {
            connectToTCP();
        }
        connected = isSerialConnected || isUdpConnected || isTcpConnected;
    }

    private void connectToTCP() {
        final Socket socket = new Socket();
        this.tcpSocket = socket;
        Thread listener = new Thread("NMEA TCP Listener") {
            public void run() {
//                    connected = false;
//                isTcpConnected = false;
                BufferedReader reader = null;
                try {
                    socket.connect(new InetSocketAddress(tcpHost, tcpPort));
                    reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
//                    connected = true;
                    isTcpConnected = true;
                }
                catch (Exception e) {
                    NeptusLog.pub().error(e);
                    getConsole().post(Notification.error("NMEA Plotter",
                            "Error connecting via TCP to " + tcpHost + ":" + tcpPort));
                    
                    isTcpConnected = false;

                    // if still connected, we need to reconnect
                    reconnect(socket);
                    
                    return;
                }
                NeptusLog.pub().info("Listening to NMEA messages over TCP.");
                getConsole().post(
                        Notification.success("NMEA Plotter", "Connected via TCP to " + tcpHost + ":" + tcpPort));
                while (connected && isTcpConnected) {
                    try {
                        String sentence = reader.readLine();
                        try {
                            parseSentence(sentence);
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                        }
                        if (retransmitToNeptus)
                            retransmit(sentence);
                        if (logReceivedData)
                            LsfMessageLogger.log(new DevDataText(sentence));
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                        break;
                    }
                }
                NeptusLog.pub().info("TCP Socket closed.");
                getConsole().post(Notification.info("NMEA Plotter", "Disconnected via TCP."));
                try {
                    socket.close();
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
                finally {
                    isTcpConnected = false;
                }
                
                // if still connected, we need to reconnect
                reconnect(socket);
            }

            private void reconnect(final Socket socket) {
                if (connected) {
                    try {
                        Thread.sleep(5000);
                    }
                    catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if (connected && !isTcpConnected && socket == NmeaPlotter.this.tcpSocket) {
                        connectToTCP();
                    }
                }
            };
        };
        listener.setDaemon(true);
        isTcpConnected = true;
        listener.start();
    }

    private void connectToUDP() throws SocketException {
        final DatagramSocket socket = new DatagramSocket(udpPort);
        Thread udpListenerThread = new Thread("NMEA UDP Listener") {
            public void run() {
//                    connected = true;
                isUdpConnected = true;
                try {
                    socket.setSoTimeout(1000);
                }
                catch (SocketException e1) {
                    e1.printStackTrace();
                }
                NeptusLog.pub().info("Listening to NMEA messages over UDP.");
                getConsole().post(Notification.success("NMEA Plotter", "Listening via UDP to port " + udpPort + "."));
                while (connected && isUdpConnected) {
                    try {
                        DatagramPacket dp = new DatagramPacket(new byte[65507], 65507);
                        socket.receive(dp);
                        String sentence = new String(dp.getData());
                        sentence = sentence.substring(0, sentence.indexOf(0));
                        try {
                            parseSentence(sentence);
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                        }
                        if (retransmitToNeptus)
                            retransmit(sentence);
                        if (logReceivedData)
                            LsfMessageLogger.log(new DevDataText(sentence));
                    }
                    catch (SocketTimeoutException e) {
                        continue;
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                        break;
                    }
                }
                NeptusLog.pub().info("UDP Socket closed.");
                getConsole().post(Notification.info("NMEA Plotter", "Stop listening via UDP."));
                socket.close();
                isUdpConnected = false;
            };
        };
        udpListenerThread.setDaemon(true);
        udpListenerThread.start();
    }

    public void disconnect() throws Exception {
        connected = false;
        if (isSerialConnected && serialPort != null) {
            boolean res;
            try {
                res = serialPort.closePort();
            }
            catch (Exception e) {
                e.printStackTrace();
                res = true;
            }
            if (res)
                isSerialConnected = false;
        }
        if (udpSocket != null) {
            udpSocket.close();
//            udpListenerThread.interrupt();
            isUdpConnected = false;
            udpSocket = null;
        }
        if (udpSocket == null)
            isUdpConnected = false;
        if (tcpSocket != null) {
            tcpSocket.close();
            isTcpConnected = false;
            tcpSocket = null;
        }
        if (tcpSocket == null)
            isTcpConnected = false;

        connected = isSerialConnected || isUdpConnected || isTcpConnected;
    }

    public void addListener(NmeaListener listener) {
        listeners.add(listener);
    }

    public void removeListener(NmeaListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void cleanLayer() {
        connected = false;
        try {
            disconnect();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        getConsole().removeMenuItem(I18n.text("Tools") + ">" + I18n.text("NMEA Plotter") + ">" + I18n.text("Connect"));
        getConsole().removeMenuItem(I18n.text("Tools") + ">" + I18n.text("NMEA Plotter") + ">" + I18n.text("Settings"));
    }

    public boolean userControlsOpacity() {
        return false;
    }

    @Periodic(millisBetweenUpdates = 60000)
    public void purgeOldContacts() {
        contactDb.purge(maximumAisAge * 1000);
    }

    @Periodic(millisBetweenUpdates = 120000)
    public void saveCache() {
        contactDb.saveCache();
    }

    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {
        super.paint(g, renderer);

        ArrayList<ScatterPointsElement> els = new ArrayList<>();
        els.addAll(tracks.values());
        for (ScatterPointsElement el : els)
            el.paint((Graphics2D) g.create(), renderer, renderer.getRotation());

        Graphics2D g1 = (Graphics2D) g.create();
        for (AisContact c : contactDb.getContacts()) {
            LocationType l = c.getLocation();
            if (l.getLatitudeDegs() == 0 && l.getLongitudeDegs() == 0)
                continue;

            Point2D pt = renderer.getScreenPosition(l);
            g1.setColor(new Color(64, 124, 192));
            g1.drawString(c.getLabel(), (int) pt.getX() + 17, (int) pt.getY() + 2);

            if (c.getAdditionalProperties() != null) {
                g1.setColor(new Color(64, 124, 192, 128));
                Message05 m = c.getAdditionalProperties();
                Graphics2D copy = (Graphics2D) g1.create();
                double width = m.getDimensionToPort() + m.getDimensionToStarboard();
                double length = m.getDimensionToStern() + m.getDimensionToBow();
                double centerX = pt.getX();
                double centerY = pt.getY();

                double widthOffsetFromCenter = m.getDimensionToPort() - m.getDimensionToStarboard();
                double lenghtOffsetFromCenter = m.getDimensionToStern() - m.getDimensionToBow();

                copy.translate(centerX, centerY);
                double hdg = c.getHdg() > 360 ? c.getCog() : c.getHdg();
                copy.rotate(Math.PI + Math.toRadians(hdg) - renderer.getRotation());
                copy.scale(renderer.getZoom(), renderer.getZoom());
                copy.translate(widthOffsetFromCenter / 2., -lenghtOffsetFromCenter / 2.);
                copy.scale(width / 2, length / 2);
                copy.fill(ship);
                copy.dispose();
            }
            g1.setColor(Color.black);
            g1.fill(new Ellipse2D.Double((int) pt.getX() - 3, (int) pt.getY() - 3, 6, 6));
        }
        g1.dispose();
    }

    @Override
    public void initLayer() {
        connectItem = getConsole().addMenuItem(
                I18n.text("Tools") + ">" + I18n.text("NMEA Plotter") + ">" + I18n.text("Connect"), 
                getIcon(), new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        try {
                            if (!connected)
                                connect();
                            else
                                disconnect();
                        }
                        catch (Exception ex) {
                            GuiUtils.errorMessage(getConsole(), ex);
                        }
                        updateConnectMenuText();
                    }
                });

        getConsole().addMenuItem(I18n.text("Tools") + ">" + I18n.text("NMEA Plotter") + ">" + I18n.text("Settings"),
                null, new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        PluginUtils.editPluginProperties(NmeaPlotter.this, true);
                    }
                });
        parser.register(contactDb);
    }

    private void updateConnectMenuText() {
        if (connected) {
            String comms = isSerialConnected ? "serial" : "";
            comms += isUdpConnected ? (comms.isEmpty() ? "" : ", ") + "UDP" : "";
            comms += isTcpConnected ? (comms.isEmpty() ? "" : ", ") + "TCP" : "";
            if (!comms.isEmpty())
                comms = " (" + comms + ")";
            connectItem.setText(I18n.text("Disconnect") + comms);
        }
        else {
            connectItem.setText(I18n.text("Connect"));
        }
    }

    public static void main(String[] args) throws Exception {
        @SuppressWarnings("resource")
        ServerSocket tcp = new ServerSocket(13000);
        BufferedReader br = null;
        while (true) {
            try {
                Socket con = tcp.accept();
                FileReader fr = new FileReader(new File("CMRE-AIS_example.txt"));
                br = new BufferedReader(fr);
                while (con.isConnected()) {
                    OutputStream os = con.getOutputStream();
                    String line = br.readLine();
                    if (line == null)
                        break;
                    System.out.println(line);
                    os.write(line.getBytes("UTF-8"));
                    os.write("\n\r".getBytes());
                    Thread.sleep(5000);
                }
                System.out.println("END");
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            finally {
                if (br != null)
                    br.close();
            }
        }
    }
}
