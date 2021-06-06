/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.micromanager.hiwincontrolpanel;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import java.util.*;

import mmcorej.CMMCore;
import mmcorej.CharVector;

import org.micromanager.api.ScriptInterface;

/**
 *
 * @author rfv84
 */
public class HiwinControlFrame extends javax.swing.JFrame {
    private final ScriptInterface gui_;
    private final CMMCore core_;
    String port_kohzu;
    String port_hiwin;
    boolean port_enable = false;
    boolean isKohzuTimer = false;
    boolean isHiwinTimer = false;
   
    
    // command
    String hiwinTerminator = "}";
    String kohzuTerminator = "\r\n";
    String hiwinCommand = "";
    String kohzuCommand = "";
    CharVector HIWIN_Read;
    char ch;
    int comma_count = 0;
    CharVector kohzu_Read_Buffer;
    
    int STOP = 1;//0 run, 1 stop
    int STEP = 0;
    int STEP_FINISH = 0;//0 not finish, 1 finish
    
    Timer kohzu_timer;
    Timer hiwin_timer;
    
    String z_axis = "2";
    String x_axis = "1"; //02A limit 291900 
    
    String Target; 
    //kohzu parameter
    int kohzu_posZ = 0;
    int kohzu_posX = 0;
    int kohzu_speedTable = 8;
    int X_target_pos = 291900;
    int Z_target_pos = -251000;
    
    
    //Hiwin parameter
    int NowStatus = 1;
    int n = 0;
    
    /**
     * Creates new form HiwinControlFrame
     */
    private void clearBuffer(String port) {
        try {
            CharVector answer;
            do {
                answer = core_.readFromSerialPort(port);
            } while(answer.capacity() > 0);
        } catch (Exception ex) {
            Logger.getLogger(HiwinControlFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
// read kohzu stage position from axis of port
    private int readKohzu(String port, String axis) {
        String answer;
        try {          
            clearBuffer(port);
            core_.setSerialPortCommand(port, "\002RDP" + axis, kohzuTerminator);
            Thread.sleep(10);
            answer = core_.getSerialPortAnswer(port, "\r\n");
            StringTokenizer st = new StringTokenizer(answer);
            if(st.nextToken().equals("C")) {
                    st.nextToken();
                    return Integer.parseInt(st.nextToken());
            }
        } catch (Exception ex) {
            Logger.getLogger(HiwinControlFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
        return Integer.MIN_VALUE;
    }   
    public class task_kohzu extends TimerTask {
        /*
         * Command has been sent when start button is clicked. 
         * Timer just for checking position and turn on hiwin timer when target positions arrive.
         */
        @Override
        public void run() {
            try{ 
                kohzu_posZ = readKohzu(port_kohzu,z_axis);
                kohzu_posX = readKohzu(port_kohzu,x_axis);
                jLabel_x.setText(String.valueOf(kohzu_posX));
                jLabel_z.setText(String.valueOf(kohzu_posZ));
                if(kohzu_posZ == Z_target_pos && kohzu_posX == X_target_pos){
                    hiwin_timer = new Timer();
                    hiwin_timer.schedule(new task_hiwin(), 50, 1000);
                }             
            }catch (Exception ex) {
                    Logger.getLogger(HiwinControlFrame.class.getName()).log(Level.SEVERE, null, ex);
                                  }              
        }
    }
    public class task_hiwin extends TimerTask {
        @Override
        public void run() {          
                TM_HIWIN = true;
                if(NowStatus == 3){
                    STOP = 0;
                    STEP = 1;
                    STEP_FINISH = 0;
                    hiwinCommand = "{" + Target + "," + STOP + "," + STEP + "," + STEP_FINISH + "," + "0" + "," + "0" + "," + "0";                   
                    try {                        
                        core_.setSerialPortCommand(port_hiwin, hiwinCommand, hiwinTerminator);  
                    } catch (Exception ex) {
                        Logger.getLogger(HiwinControlFrame.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    NowStatus = 4;
                }
                else if(NowStatus == 4){//4: HIWIN programming                    
                    try {                                                  
                        HIWIN_Read = core_.readFromSerialPort(port_hiwin);
                        if(HIWIN_Read.capacity()!=0){
                            for(int i=0;i<HIWIN_Read.capacity();i++){
                                ch = HIWIN_Read.get(i);
                                if(ch == ','){
                                    comma_count++;
                                }
                                if(comma_count == 3){
                                    STEP_FINISH = Character.getNumericValue(HIWIN_Read.get(i+1));
                                    comma_count = 0;
                                    ch ='\0';
                                    break;
                                }                                
                            }
                        }                        

                        } catch (Exception ex) {
                            Logger.getLogger(HiwinControlFrame.class.getName()).log(Level.SEVERE, null, ex);
                        }                                                   
                    switch(STEP){//Step value
                        //<editor-fold defaultstate="collapsed" desc=" STEP Case ">
                        case 1:
                            if(STEP_FINISH == 1){//==1 means step finished
                                if(EMG == true){
                                    STOP = 1;
                                }
                                else{
                                    STOP = 0;
                                }
                                STEP = 2;
                                if(Message_target == 1){//grab target beside placed one
                                    Message_target = 8;
                                }
                                else{
                                    Message_target--;
                                }                                    
                            }
                            break;
                        case 2:
                             if(STEP_FINISH == 1){
                                if(EMG == true){
                                    STOP = 1;
                                }
                                else{
                                    STOP = 0;
                                }
                                STEP = 3;
                                if(Message_target == 8){//grab target beside placed one
                                    Message_target = 1;
                                }
                                else{
                                    Message_target++;
                                }                                   
                            }
                            break;
                        case 3:
                             if(STEP_FINISH == 1){
                                if(EMG == true){
                                    STOP = 1;
                                }
                                else{
                                    STOP = 0;
                                }
                                STEP = 4;                           
                            }
                            break;
                        case 4:
                             if(STEP_FINISH == 1){
                                STOP = 1;
                                STEP = 5;                          
                                TM_HIWIN = false;              
                                Timer_HIWIN.cancel();        
                                Timer_BackPos.schedule(new kohzu_pos(), 50, 1000);  
                                NowStatus = 5;
                                latch_kohzu = true;
                            }                           
                            break;                                   
                        default:
                            break;
                        //</editor-fold>  
                    };
                    readySig.setBackground(Color.YELLOW);
                    jLabel_status.setText("Operating" + ": STEP " + STEP);                   
                    if(STEP_FINISH == 1){
                        STEP_FINISH = 0;  
                        hiwinCommand = "{" + Target + "," + STOP + "," + STEP + "," + STEP_FINISH + "," + "0" + "," + "0" + "," + "0";
                        try {                             
                            core_.setSerialPortCommand(port_hiwin, hiwinCommand, hiwinTerminator);                                           
                        } catch (Exception ex) {
                            Logger.getLogger(HiwinControlFrame.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }

                }               
        }
    }
    public HiwinControlFrame(ScriptInterface gui) {
        gui_ = gui;
        core_ = gui_.getMMCore();
        initComponents();
        
        
    }

    

   

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jButton_stop = new javax.swing.JButton();
        jPanel1 = new javax.swing.JPanel();
        sample_text = new javax.swing.JLabel();
        jComboBox_sample = new javax.swing.JComboBox();
        jButton_select = new javax.swing.JButton();
        jButton_cancel = new javax.swing.JButton();
        jPanel3 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jComboBox_kohzu = new javax.swing.JComboBox();
        jLabel2 = new javax.swing.JLabel();
        jComboBox_hiwin = new javax.swing.JComboBox();
        jRadioButton_enable = new javax.swing.JRadioButton();
        jRadioButton_disable = new javax.swing.JRadioButton();
        jLabel14 = new javax.swing.JLabel();
        jLabel15 = new javax.swing.JLabel();
        jComboBox_Xaxis = new javax.swing.JComboBox();
        jComboBox_Zaxis = new javax.swing.JComboBox();
        jButton_start = new javax.swing.JButton();
        jScrollPane2 = new javax.swing.JScrollPane();
        jTextArea = new javax.swing.JTextArea();
        jPanel2 = new javax.swing.JPanel();
        jLabel4 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel_x = new javax.swing.JLabel();
        jLabel_z = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTextPane_SP1 = new javax.swing.JTextPane();
        jScrollPane3 = new javax.swing.JScrollPane();
        jTextPane_SP2 = new javax.swing.JTextPane();
        jScrollPane4 = new javax.swing.JScrollPane();
        jTextPane_SP7 = new javax.swing.JTextPane();
        jScrollPane5 = new javax.swing.JScrollPane();
        jTextPane_SP3 = new javax.swing.JTextPane();
        jScrollPane6 = new javax.swing.JScrollPane();
        jTextPane_SP6 = new javax.swing.JTextPane();
        jScrollPane7 = new javax.swing.JScrollPane();
        jTextPane_SP5 = new javax.swing.JTextPane();
        jScrollPane8 = new javax.swing.JScrollPane();
        jTextPane_SP4 = new javax.swing.JTextPane();
        jScrollPane9 = new javax.swing.JScrollPane();
        jTextPane_SP8 = new javax.swing.JTextPane();
        jLabel5 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        jLabel10 = new javax.swing.JLabel();
        jLabel11 = new javax.swing.JLabel();
        jLabel12 = new javax.swing.JLabel();
        jLabel13 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jButton_stop.setText("Stop");
        jButton_stop.setEnabled(false);
        jButton_stop.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton_stopActionPerformed(evt);
            }
        });

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("Sample"));

        sample_text.setText("Target");

        jComboBox_sample.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "1", "2", "3", "4", "5", "6", "7", "8" }));

        jButton_select.setText("Select");
        jButton_select.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton_selectActionPerformed(evt);
            }
        });

        jButton_cancel.setText("Cancel");
        jButton_cancel.setEnabled(false);
        jButton_cancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton_cancelActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(sample_text)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jComboBox_sample, javax.swing.GroupLayout.PREFERRED_SIZE, 79, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 51, Short.MAX_VALUE)
                .addComponent(jButton_select, javax.swing.GroupLayout.PREFERRED_SIZE, 81, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jButton_cancel, javax.swing.GroupLayout.PREFERRED_SIZE, 81, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(22, 22, 22))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(0, 0, Short.MAX_VALUE)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jComboBox_sample, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(sample_text)
                    .addComponent(jButton_select)
                    .addComponent(jButton_cancel))
                .addGap(64, 64, 64))
        );

        sample_text.getAccessibleContext().setAccessibleName("Sample number");

        jPanel3.setBorder(javax.swing.BorderFactory.createTitledBorder("COM Port"));

        jLabel1.setText("Kohzu");

        jComboBox_kohzu.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "COM 1", "COM 2", "COM 3", "COM 4", "COM 5", "COM 6", "COM 7", "COM 8", "COM 9", "COM 10" }));

        jLabel2.setText("RobotArm");

        jComboBox_hiwin.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "COM 1", "COM 2", "COM 3", "COM 4", "COM 5", "COM 6", "COM 7", "COM 8", "COM 9", "COM 10" }));

        jRadioButton_enable.setText("Enable");
        jRadioButton_enable.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jRadioButton_enableActionPerformed(evt);
            }
        });

        jRadioButton_disable.setSelected(true);
        jRadioButton_disable.setText("Disable");
        jRadioButton_disable.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jRadioButton_disableActionPerformed(evt);
            }
        });

        jLabel14.setText("Z :");

        jLabel15.setText("X :");

        jComboBox_Xaxis.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "1", "2", "3", "4", "5", "6", "7", "8", "9" }));

        jComboBox_Zaxis.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "1", "2", "3", "4", "5", "6", "7", "8", "9" }));

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                            .addComponent(jLabel1)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED))
                        .addGroup(jPanel3Layout.createSequentialGroup()
                            .addComponent(jLabel15)
                            .addGap(28, 28, 28)))
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(jLabel14)
                        .addGap(30, 30, 30)))
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(jComboBox_Xaxis, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jComboBox_kohzu, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jComboBox_Zaxis, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(18, 18, 18)
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jComboBox_hiwin, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 136, Short.MAX_VALUE)
                .addComponent(jRadioButton_enable)
                .addGap(58, 58, 58)
                .addComponent(jRadioButton_disable)
                .addGap(67, 67, 67))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(jComboBox_kohzu, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel2)
                    .addComponent(jComboBox_hiwin, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jRadioButton_enable)
                    .addComponent(jRadioButton_disable))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 9, Short.MAX_VALUE)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel15)
                    .addComponent(jComboBox_Xaxis, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel14)
                    .addComponent(jComboBox_Zaxis, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18))
        );

        jButton_start.setText("Start");
        jButton_start.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton_startActionPerformed(evt);
            }
        });

        jTextArea.setColumns(20);
        jTextArea.setFont(new java.awt.Font("Monospaced", 0, 18)); // NOI18N
        jTextArea.setRows(5);
        jScrollPane2.setViewportView(jTextArea);

        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder("Information"));

        jLabel4.setText("Kohzu_Z :");

        jLabel3.setText("Kohzu_X :");

        jLabel_x.setText("0");

        jLabel_z.setText("0");

        jScrollPane1.setPreferredSize(new java.awt.Dimension(31, 31));
        jScrollPane1.setViewportView(jTextPane_SP1);

        jScrollPane3.setPreferredSize(new java.awt.Dimension(31, 31));
        jScrollPane3.setViewportView(jTextPane_SP2);

        jScrollPane4.setPreferredSize(new java.awt.Dimension(31, 31));
        jScrollPane4.setViewportView(jTextPane_SP7);

        jScrollPane5.setPreferredSize(new java.awt.Dimension(31, 31));
        jScrollPane5.setViewportView(jTextPane_SP3);

        jScrollPane6.setPreferredSize(new java.awt.Dimension(31, 31));
        jScrollPane6.setViewportView(jTextPane_SP6);

        jScrollPane7.setPreferredSize(new java.awt.Dimension(31, 31));
        jScrollPane7.setViewportView(jTextPane_SP5);

        jScrollPane8.setPreferredSize(new java.awt.Dimension(31, 31));
        jScrollPane8.setViewportView(jTextPane_SP4);

        jScrollPane9.setPreferredSize(new java.awt.Dimension(31, 31));
        jScrollPane9.setViewportView(jTextPane_SP8);

        jLabel5.setText("1");

        jLabel6.setText("2");

        jLabel7.setText("3");

        jLabel8.setText("4");

        jLabel9.setText("5");

        jLabel10.setText("6");

        jLabel11.setText("7");

        jLabel12.setText("8");

        jLabel13.setText("Sample");

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel4)
                    .addComponent(jLabel3)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGap(223, 223, 223)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jLabel_z)
                            .addComponent(jLabel_x)))
                    .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel2Layout.createSequentialGroup()
                            .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGroup(jPanel2Layout.createSequentialGroup()
                                    .addGap(11, 11, 11)
                                    .addComponent(jLabel5)))
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                                    .addComponent(jLabel6)
                                    .addGap(11, 11, 11)))
                            .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addGroup(jPanel2Layout.createSequentialGroup()
                                    .addGap(9, 9, 9)
                                    .addComponent(jScrollPane5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGroup(jPanel2Layout.createSequentialGroup()
                                    .addGap(20, 20, 20)
                                    .addComponent(jLabel7)))
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(jScrollPane8, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGroup(jPanel2Layout.createSequentialGroup()
                                    .addGap(11, 11, 11)
                                    .addComponent(jLabel8)))
                            .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                .addGroup(jPanel2Layout.createSequentialGroup()
                                    .addGap(20, 20, 20)
                                    .addComponent(jLabel9)
                                    .addGap(32, 32, 32)
                                    .addComponent(jLabel10)
                                    .addGap(31, 31, 31)
                                    .addComponent(jLabel11)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(jLabel12)
                                    .addGap(11, 11, 11))
                                .addGroup(jPanel2Layout.createSequentialGroup()
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(jScrollPane7, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(jScrollPane6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(jScrollPane4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(jScrollPane9, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                            .addComponent(jLabel13)
                            .addGap(132, 132, 132))))
                .addContainerGap(18, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(jLabel_x))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(jLabel_z))
                .addGap(31, 31, 31)
                .addComponent(jLabel13)
                .addGap(1, 1, 1)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jLabel5)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(jLabel6)
                                .addComponent(jLabel7))
                            .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(jLabel10)
                                .addComponent(jLabel11)
                                .addComponent(jLabel12))
                            .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(jLabel8)
                                .addComponent(jLabel9)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jScrollPane5, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jScrollPane8, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jScrollPane7, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jScrollPane6, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jScrollPane4, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jScrollPane9, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addContainerGap(44, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel3, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jScrollPane2))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(62, 62, 62)
                        .addComponent(jButton_start, javax.swing.GroupLayout.PREFERRED_SIZE, 83, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jButton_stop, javax.swing.GroupLayout.PREFERRED_SIZE, 83, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(56, 56, 56)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jButton_start, javax.swing.GroupLayout.PREFERRED_SIZE, 72, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jButton_stop, javax.swing.GroupLayout.PREFERRED_SIZE, 72, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, 75, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jScrollPane2))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jRadioButton_enableActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jRadioButton_enableActionPerformed
        jRadioButton_disable.setSelected(false);
        if(!port_enable){
            port_enable = true;
            jRadioButton_enable.setSelected(true);
            port_kohzu = jComboBox_kohzu.getSelectedItem().toString();
            port_hiwin = jComboBox_hiwin.getSelectedItem().toString();
            x_axis = jComboBox_Xaxis.getSelectedItem().toString();
            z_axis = jComboBox_Zaxis.getSelectedItem().toString();
            if ((port_kohzu == port_hiwin) | (x_axis == z_axis)) {
                port_enable = false;
                JOptionPane.showMessageDialog(null, "COM ports or axes should be different.\n Please select again.");
                jRadioButton_enable.setSelected(false);
                jRadioButton_disable.setSelected(true);
            }
            else{//show kohzu information on the screen
                jTextArea.append("X axis: " + x_axis + "\n");
                jTextArea.append("Z axis: " + z_axis + "\n");
                jTextArea.append("Kohzu port: " + port_kohzu + "\n");
                jTextArea.append("Hiwin port: " + port_hiwin + "\n");
                jTextArea.append("\n");
                kohzu_posZ = readKohzu(port_kohzu, z_axis);
                kohzu_posX = readKohzu(port_kohzu, x_axis);
                jLabel_x.setText(String.valueOf(kohzu_posX));
                jLabel_z.setText(String.valueOf(kohzu_posZ));
            }
        }
        else{
             jRadioButton_enable.setSelected(true);
             jRadioButton_disable.setSelected(false);
        }
    }//GEN-LAST:event_jRadioButton_enableActionPerformed

    private void jRadioButton_disableActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jRadioButton_disableActionPerformed
        jRadioButton_enable.setSelected(false);
        if (port_enable){
            port_enable = false;
            jTextArea.append("Ports are disable.\n");
            jTextArea.append("\n");
            //stop timer
        }
        else{
            jRadioButton_disable.setSelected(true);
            jRadioButton_enable.setSelected(false);
        }
        
        // TODO add your handling code here:
    }//GEN-LAST:event_jRadioButton_disableActionPerformed

    private void jButton_selectActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton_selectActionPerformed
        //Select sample number
        if(jButton_select.isEnabled()){
            Target = jComboBox_sample.getSelectedItem().toString();
            jComboBox_sample.setEnabled(false);
            jButton_select.setEnabled(false);
            jButton_cancel.setEnabled(true);
            jTextArea.append("Target " + Target + " is selected.\n");
            switch(Integer. parseInt(Target)){
                case 1:
                    jTextPane_SP1.setBackground(Color.yellow);
                    break;
                case 2:
                    jTextPane_SP2.setBackground(Color.yellow);
                    break;
                case 3:
                    jTextPane_SP3.setBackground(Color.yellow);
                    break;
                case 4:
                    jTextPane_SP4.setBackground(Color.yellow);
                    break;
                case 5:
                    jTextPane_SP5.setBackground(Color.yellow);
                    break;
                case 6:
                    jTextPane_SP6.setBackground(Color.yellow);
                    break;
                case 7:
                    jTextPane_SP7.setBackground(Color.yellow);
                    break;
                case 8:
                    jTextPane_SP8.setBackground(Color.yellow);
                    break;                           
            }
        }
    }//GEN-LAST:event_jButton_selectActionPerformed

    private void jButton_cancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton_cancelActionPerformed
        // Cancel selected sample number
        if(jButton_cancel.isEnabled()){
            Color color = new Color(255,255,255);
            switch(Integer. parseInt(Target)){
                case 1:
                    jTextPane_SP1.setBackground(color);
                    break;
                case 2:
                    jTextPane_SP2.setBackground(color);
                    break;
                case 3:
                    jTextPane_SP3.setBackground(color);
                    break;
                case 4:
                    jTextPane_SP4.setBackground(color);
                    break;
                case 5:
                    jTextPane_SP5.setBackground(color);
                    break;
                case 6:
                    jTextPane_SP6.setBackground(color);
                    break;
                case 7:
                    jTextPane_SP7.setBackground(color);
                    break;
                case 8:
                    jTextPane_SP8.setBackground(color);
                    break;                           
            }
            jTextArea.append("Selected target is canceled.\n");
            Target = "";
            jButton_cancel.setEnabled(false);
            jButton_select.setEnabled(true);
            jComboBox_sample.setEnabled(true);
        }
    }//GEN-LAST:event_jButton_cancelActionPerformed

    private void jButton_startActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton_startActionPerformed
        // 3 situation kohzu timer: off hiwin timer: off
        //             kohzu timer: on hiwin timer: off
        //             kohzu timer: off hiwin timer: on
        //start first by moving kohzu stage
        if(isKohzuTimer == false & isHiwinTimer == false){
            isKohzuTimer = true;
            kohzu_timer = new Timer();
            jButton_stop.setEnabled(true);
            jButton_start.setEnabled(false);

            //send kohzu target command 
            kohzuCommand = "\002APS" + z_axis + "/" + kohzu_speedTable + "/" + Z_target_pos + "/" + "1";//APSa/b/c/d  a:axis b:speed table number c:movement amount d:response method
            jTextArea.append(kohzuCommand + "\n");
            try{
                core_.setSerialPortCommand(port_kohzu, kohzuCommand, kohzuTerminator);
            }catch (Exception ex) {
                        Logger.getLogger(HiwinControlFrame.class.getName()).log(Level.SEVERE, null, ex);
                                  }  
            //show message on panel
            jTextArea.append("Send kohzu : " + kohzuCommand + "\n");
            jTextArea.append("\n");

            //Then start kohzu timer
            kohzu_timer.schedule(new task_kohzu(), 50, 1000);
        }
        else if(isKohzuTimer == true & isHiwinTimer == false){
            kohzu_timer = new Timer();
            jButton_stop.setEnabled(true);
            jButton_start.setEnabled(false);

            //send kohzu target command 
            kohzuCommand = "\002APS" + z_axis + "/" + kohzu_speedTable + "/" + Z_target_pos + "/" + "1";//APSa/b/c/d  a:axis b:speed table number c:movement amount d:response method
            try{
                core_.setSerialPortCommand(port_kohzu, kohzuCommand, kohzuTerminator);
            }catch (Exception ex) {
                        Logger.getLogger(HiwinControlFrame.class.getName()).log(Level.SEVERE, null, ex);
                                  }  
            //show message on panel
            jTextArea.append("Send kohzu : " + kohzuCommand + "\n");
            jTextArea.append("\n");

            //Then start kohzu timer
            kohzu_timer.schedule(new task_kohzu(), 50, 1000);
        }
        else if(isKohzuTimer == false & isHiwinTimer == true){
            hiwin_timer = new Timer();
            hiwin_timer.schedule(new task_hiwin(), 50, 1000);
        }
        
    }//GEN-LAST:event_jButton_startActionPerformed

    private void jButton_stopActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton_stopActionPerformed
        //Stop kohzu timer of hiwin timer, depend on which timer is on.
        jButton_start.setEnabled(true);
        jButton_stop.setEnabled(false);
        if(isKohzuTimer){
            kohzu_timer.cancel();
        }
        else if(isHiwinTimer){
            hiwin_timer.cancel();
        }
        jTextArea.append("Process stop.\n");
        jTextArea.append("\n");
    }//GEN-LAST:event_jButton_stopActionPerformed

    /**
     * @param args the command line arguments
     */
//    public static void main(String args[]) {
//        /* Set the Nimbus look and feel */
//        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
//        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
//         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
//         */
//        try {
//            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
//                if ("Nimbus".equals(info.getName())) {
//                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
//                    break;
//                }
//            }
//        } catch (ClassNotFoundException ex) {
//            java.util.logging.Logger.getLogger(HiwinControlFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
//        } catch (InstantiationException ex) {
//            java.util.logging.Logger.getLogger(HiwinControlFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
//        } catch (IllegalAccessException ex) {
//            java.util.logging.Logger.getLogger(HiwinControlFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
//        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
//            java.util.logging.Logger.getLogger(HiwinControlFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
//        }
//        //</editor-fold>
//
//        /* Create and display the form */
//        java.awt.EventQueue.invokeLater(new Runnable() {
//            public void run() {
//                new HiwinControlFrame().setVisible(true);
//            }
//        });
//    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton_cancel;
    private javax.swing.JButton jButton_select;
    private javax.swing.JButton jButton_start;
    private javax.swing.JButton jButton_stop;
    private javax.swing.JComboBox jComboBox_Xaxis;
    private javax.swing.JComboBox jComboBox_Zaxis;
    private javax.swing.JComboBox jComboBox_hiwin;
    private javax.swing.JComboBox jComboBox_kohzu;
    private javax.swing.JComboBox jComboBox_sample;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JLabel jLabel_x;
    private javax.swing.JLabel jLabel_z;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JRadioButton jRadioButton_disable;
    private javax.swing.JRadioButton jRadioButton_enable;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JScrollPane jScrollPane5;
    private javax.swing.JScrollPane jScrollPane6;
    private javax.swing.JScrollPane jScrollPane7;
    private javax.swing.JScrollPane jScrollPane8;
    private javax.swing.JScrollPane jScrollPane9;
    private javax.swing.JTextArea jTextArea;
    private javax.swing.JTextPane jTextPane_SP1;
    private javax.swing.JTextPane jTextPane_SP2;
    private javax.swing.JTextPane jTextPane_SP3;
    private javax.swing.JTextPane jTextPane_SP4;
    private javax.swing.JTextPane jTextPane_SP5;
    private javax.swing.JTextPane jTextPane_SP6;
    private javax.swing.JTextPane jTextPane_SP7;
    private javax.swing.JTextPane jTextPane_SP8;
    private javax.swing.JLabel sample_text;
    // End of variables declaration//GEN-END:variables
    

}
