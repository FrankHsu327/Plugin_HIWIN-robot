/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org;



import java.io.*;
import java.awt.Color;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.*;
import mmcorej.CMMCore;
import java.util.StringTokenizer;
import mmcorej.CharVector; 
import org.micromanager.api.MMListenerInterface;
import org.micromanager.api.ScriptInterface;

/**
 *
 * @author rfv84
 */
public class HIWIN_frame extends javax.swing.JFrame implements MMListenerInterface {
    private final ScriptInterface gui_;
    private final CMMCore core_;
    int mouseX;
    int mouseY;
    int NowStatus = 0;
      //<editor-fold defaultstate="collapsed" desc=" int NowStatus definition ">
 /*                      * 1: choose target, 
                         * 2: confirm choosen target
                         * 3: programming(kohzu moving)
                         * 4: programming(HIWIN moving)
                         * 5: program finished
                         * 6: program stop and wait for reset or continue command
                         * choose reset will turn to 1 
                         * choose continu will turn to 3
                         * */
//        //</editor-fold>
    int Z_original_pos = 0;
    int Z_target_pos = -251000;
    int Z_now_pos = 0;
    int X_original_pos = 0;
    int X_target_pos = 291900;
    int X_now_pos = 0;
    boolean original_pos = true;
    boolean target_pos = false;
    //////////////////
    //////////////////serial port and command
    InputStream inputstream;
    OutputStream outputstream;
    
    String HIWIN_COM = "COM4";
    String KOHZU_COM = "COM3";
    
    String hiwinTerminator = "}";
    String hiwinCommand = "";
    String kohzuCommand = "";
    String kohzuTerminator = "\r\n";
    CharVector HIWIN_Read;
    char ch;
    int comma_count = 0;
    CharVector kohzu_Read_Buffer;
    
    //////////////////
    //////////////////Timer
    Timer Timer_CheckPos;
    Timer Timer_BackPos;
    TimerTask kohzu_pos;
    Timer Timer_HIWIN;
    TimerTask HIWIN_CMD;
    
    boolean TM_HIWIN = false;
    boolean TM_KOHZU = false;
    boolean EMG =false;
    boolean isAuto = false;
    /***/
    int z_axis = 2;
    int x_axis = 1; //02A limit 291900 
    int kohzu_speedTable = 8;
    String Target;   
    int Message_target_Log = 0;
    int Message_target = 0;
    int STOP = 1;//0 run, 1 stop
    int STEP = 0;
    int STEP_FINISH = 0;//0 not finish, 1 finish
    int ReadLog = 0;
    boolean latch_status5 = false;
    boolean latch_kohzu = false;
    boolean precheck =false;
    
    //////////////////
    //////////////////log
    /**
     * Creates new form MyFrame_
     */
    //<editor-fold defaultstate="collapsed" desc=" TimerTask Class ">
        //////////////////////////////////////////
        ///hiwin timer↓
        //////////////////////////////////////////
    public class HIWIN_CMD extends TimerTask{

        @Override
        public void run() {
            //////////////////////////////////////////
                /////////////Command for HIWIN
                /////////////Excute movement according to NowStatus
                //////////////////////////////////////////
                TM_HIWIN = true;
                if(NowStatus == 3){
                    STOP = 0;
                    STEP = 1;
                    STEP_FINISH = 0;
                    hiwinCommand = "{" + Message_target + "," + STOP + "," + STEP + "," + STEP_FINISH + "," + "0" + "," + "0" + "," + "0";                   
                    try {                        
                        Text_T.append(hiwinCommand + hiwinTerminator + "\n");
                        core_.setSerialPortCommand(HIWIN_COM, hiwinCommand, hiwinTerminator);  
                                //////////setSerialPortCommand(port, command, commandTerminator);
                    } catch (Exception ex) {
                        Logger.getLogger(HIWIN_frame.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    NowStatus = 4;
                }
                else if(NowStatus == 4){//4: HIWIN programming                    
                    try {                                                  
                        HIWIN_Read = core_.readFromSerialPort(HIWIN_COM);
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
                            Logger.getLogger(HIWIN_frame.class.getName()).log(Level.SEVERE, null, ex);
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
                        hiwinCommand = "{" + Message_target + "," + STOP + "," + STEP + "," + STEP_FINISH + "," + "0" + "," + "0" + "," + "0";
                        try {
                            Text_T.append(hiwinCommand + hiwinTerminator + "\n");                                   
                            core_.setSerialPortCommand(HIWIN_COM, hiwinCommand, hiwinTerminator);                                           
                        } catch (Exception ex) {
                            Logger.getLogger(HIWIN_frame.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                    
                }               
        }
        
    }
        //////////////////////////////////////////
        //////////////////////kohzu timer ↓
        //////////////////////////////////////////
    public class kohzu_pos extends TimerTask{

        @Override
        public void run() {
            //check kohzu position
            TM_KOHZU = true;
            if(NowStatus == 3){
                readySig.setBackground(Color.YELLOW);
                jLabel_status.setText("Operating: Kohzu");
                if(latch_kohzu){     // Give command when latch is true, move to lowest point  
                    ////////////////////////
                    ////////////////////////Z pos
                    kohzuCommand = "\002APS" + z_axis + "/" + kohzu_speedTable + "/" + Z_target_pos + "/" + "1";//APSa/b/c/d  a:axis b:speed table number c:movement amount d:response method
                    Text_T.append(kohzuCommand + "\n");
                    try {                                                           
                        core_.setSerialPortCommand(KOHZU_COM, kohzuCommand, kohzuTerminator);
                    } catch (Exception ex) {
                        Logger.getLogger(HIWIN_frame.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    ////////////////////////
                    ////////////////////////X pos
                    kohzuCommand = "\002APS" + x_axis + "/" + kohzu_speedTable + "/" + X_target_pos + "/" + "1";//APSa/b/c/d  a:axis b:speed table number c:movement amount d:response method
                    Text_T.append(kohzuCommand + "\n");
                    try {         
                        Thread.sleep(10);
                        core_.setSerialPortCommand(KOHZU_COM, kohzuCommand, kohzuTerminator);
                    } catch (Exception ex) {
                        Logger.getLogger(HIWIN_frame.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    ////////////////////////
                    ////////////////////////
                    latch_kohzu = false;
                }                
                try {
                     Thread.sleep(10);
                     Z_now_pos = readkohzu(KOHZU_COM,z_axis);   //Continuously read Z_pos    
                     Text_T.append("Z now pos :" + Z_now_pos + "\r\n");
                } catch (Exception ex) {
                     Logger.getLogger(HIWIN_frame.class.getName()).log(Level.SEVERE, null, ex);
                }
                try {
                    Thread.sleep(10);
                    X_now_pos = readkohzu(KOHZU_COM,x_axis);   //Continuously read Z_pos    
                    Text_T.append("X now pos :" + X_now_pos + "\r\n");
                } catch (Exception ex) {
                     Logger.getLogger(HIWIN_frame.class.getName()).log(Level.SEVERE, null, ex);
                }
                if(Z_now_pos == Z_target_pos && X_now_pos == X_target_pos){//Lowest point is -251200, so Z_target_pos is -251200
                    Timer_CheckPos.cancel();
                    TM_KOHZU = false;
                    Timer_HIWIN.schedule(new HIWIN_CMD(), 50, 1000);
                    NowStatus = 3;
                }               
            }
            else if(NowStatus == 5){//Robot finish, get back to original point, shut down timer
                readySig.setBackground(Color.YELLOW);
                jLabel_status.setText("Operating: Kohzu");
                if(latch_kohzu){
                    kohzuCommand = "\002APS" + z_axis + "/" + kohzu_speedTable + "/" + Z_original_pos + "/" + "1";//APSa/b/c/d  a:axis b:speed table number c:movement amount d:response method
                    try {                                   
                        Text_T.append(kohzuCommand + "\n");
                        core_.setSerialPortCommand(KOHZU_COM, kohzuCommand, kohzuTerminator);
                    } catch (Exception ex) {
                        Logger.getLogger(HIWIN_frame.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    kohzuCommand = "\002APS" + x_axis + "/" + kohzu_speedTable + "/" + X_original_pos + "/" + "1";//APSa/b/c/d  a:axis b:speed table number c:movement amount d:response method
                    try {                                   
                        Text_T.append(kohzuCommand + "\n");
                        core_.setSerialPortCommand(KOHZU_COM, kohzuCommand, kohzuTerminator);
                    } catch (Exception ex) {
                        Logger.getLogger(HIWIN_frame.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    latch_kohzu = false;
                }              
                try {
                     Z_now_pos = readkohzu(KOHZU_COM,z_axis);   //Continuously read Z_pos  
                     Text_T.append("Z now pos :" + Z_now_pos + "\n");
                } catch (Exception ex) {
                     Logger.getLogger(HIWIN_frame.class.getName()).log(Level.SEVERE, null, ex);
                }
                try {
                     X_now_pos = readkohzu(KOHZU_COM,x_axis);   //Continuously read Z_pos  
                     Text_T.append("X now pos :" + X_now_pos + "\n");
                } catch (Exception ex) {
                     Logger.getLogger(HIWIN_frame.class.getName()).log(Level.SEVERE, null, ex);
                }
                if(Z_now_pos == Z_original_pos && X_now_pos == X_original_pos){//Lowest point is -251200, so Z_target_pos is -251200
                    //<editor-fold defaultstate="collapsed" desc=" isAuto true or false ">                 
                    if(isAuto){
                        Target = Target + 1;
                        Timer_BackPos.cancel();  
                        Timer_CheckPos = new Timer();
                        Timer_HIWIN = new Timer();
                        Timer_BackPos = new Timer();
                        latch_kohzu = true;
                        NowStatus = 3;
                        try {
                            Z_original_pos =  readkohzu(KOHZU_COM,z_axis);
                            Thread.sleep(50);
                        } catch (Exception ex) {
                            Logger.getLogger(HIWIN_frame.class.getName()).log(Level.SEVERE, null, ex);
                        }
                        try {
                            X_original_pos =  readkohzu(KOHZU_COM,x_axis);
                        } catch (Exception ex) {
                            Logger.getLogger(HIWIN_frame.class.getName()).log(Level.SEVERE, null, ex);
                        }
                        Text_T.append("Z original pos :" + Z_original_pos + "\n");
                        Text_T.append("X original pos :" + X_original_pos + "\n");
                        //////////////////////////////////////////
                        ///////////// Step 3: Check if stage reach lowest point
                        //////////////////////////////////////////
                        Timer_CheckPos.schedule(new kohzu_pos(), 50, 1000);
                        //Timer_HIWIN.schedule(new HIWIN_CMD(), 50, 1000);
                    }
                    else{
                        confirmBtn.setEnabled(true);
                        cancelBtn.setEnabled(false);
                        startBtn.setEnabled(false);
                        ResetBtn.setEnabled(false);
                        ContinueBtn.setEnabled(false);
                        EMGBtn.setEnabled(false);
                        Text_T.setText("");
                        readySig.setBackground(Color.GREEN);
                        jLabel_status.setText("Ready");
                        NowStatus = 1;    
                        ComboBox_kohzuCOM.setEnabled(true);
                        ComboBox_COM.setEnabled(true);
                        jComboBox_Target.setEnabled(true);
                        //</editor-fold>  
                        original_pos = true;                    
                        Timer_BackPos.cancel();  
                        TM_KOHZU = false;
                        target_pos = false;
                    }
                    
                }
            }
            else if(NowStatus == 6){
                if(!latch_kohzu){
                    kohzuCommand = "\002STP" + z_axis + "/" + "1";//0:deceleration and stop 1:emergency stop
                    Text_T.append(kohzuCommand + "\n");
                    try {                                   
                        core_.setSerialPortCommand(KOHZU_COM, kohzuCommand, kohzuTerminator);
                    } catch (Exception ex) {
                        Logger.getLogger(HIWIN_frame.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    kohzuCommand = "\002STP" + x_axis + "/" + "1";//0:deceleration and stop 1:emergency stop
                    Text_T.append(kohzuCommand + "\n");
                    try {                                   
                        core_.setSerialPortCommand(KOHZU_COM, kohzuCommand, kohzuTerminator);
                    } catch (Exception ex) {
                        Logger.getLogger(HIWIN_frame.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    if(!ContinueBtn.isEnabled()){
                        ContinueBtn.setEnabled(true);
                    }
                    if(!ResetBtn.isEnabled()){
                            ResetBtn.setEnabled(true);
                    }
                    latch_kohzu = true;
                }
                
            } 
                                             
        }
        
    }   
    
    //</editor-fold>
    public HIWIN_frame(ScriptInterface gui) {
        
        gui_ = gui;
        core_ = gui_.getMMCore();
        initComponents();
        setDefaultCloseOperation(HIWIN_frame.DISPOSE_ON_CLOSE);
        this.getContentPane().setBackground(new java.awt.Color(240, 240, 240));
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    public final void initialize() {
        ComboBox_COM.setSelectedItem("COM4");
        ComboBox_kohzuCOM.setSelectedItem("COM3");
        confirmBtn.setEnabled(true);
        cancelBtn.setEnabled(false);
        startBtn.setEnabled(false);
        ResetBtn.setEnabled(false);
        ContinueBtn.setEnabled(false);
        EMGBtn.setEnabled(false);
        readySig.setBackground(Color.GREEN);
        jLabel_status.setText("Ready");
        NowStatus = 1;    
        ComboBox_kohzuCOM.setEnabled(true);
        ComboBox_COM.setEnabled(true);
        jComboBox_Target.setEnabled(true);
    }
    void clearbuffer(String port) throws Exception{   
	do{
		kohzu_Read_Buffer = core_.readFromSerialPort(port);
	}while(kohzu_Read_Buffer.capacity() > 0);
    }
    int readkohzu(String port, int axis) throws Exception{
	try{
              String answer;
              readySig.setBackground(Color.YELLOW);
              jLabel_status.setText("Reading kozhu.");
              kohzuCommand = "\002RDP";// \002 = 0x02
              clearbuffer(port);              
	      core_.setSerialPortCommand(port, kohzuCommand + axis, kohzuTerminator);
              Thread.sleep(10);
	      answer = core_.getSerialPortAnswer(port, "\r\n");
              StringTokenizer st = new StringTokenizer(answer);
              if(st.nextToken().equals("C")){		
                    st.nextToken();                   
                    return Integer.parseInt(st.nextToken());
              }
	   }catch (Exception ex) {
                Logger.getLogger(HIWIN_frame.class.getName()).log(Level.SEVERE, null, ex);
           }
        return Integer.MIN_VALUE;
    }
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jDialog_ = new javax.swing.JDialog();
        jButton_ok = new javax.swing.JButton();
        jPanel6 = new javax.swing.JPanel();
        jScrollPane3 = new javax.swing.JScrollPane();
        jTextArea1 = new javax.swing.JTextArea();
        jLabel4 = new javax.swing.JLabel();
        jPanel1 = new javax.swing.JPanel();
        jPanel3 = new javax.swing.JPanel();
        jScrollPane5 = new javax.swing.JScrollPane();
        readySig = new javax.swing.JTextPane();
        jLabel_status = new javax.swing.JLabel();
        startBtn = new javax.swing.JButton();
        ResetBtn = new javax.swing.JButton();
        EMGBtn = new javax.swing.JButton();
        ContinueBtn = new javax.swing.JButton();
        jRadioButton1 = new javax.swing.JRadioButton();
        jRadioButton2 = new javax.swing.JRadioButton();
        jButton_align = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        Text_T = new javax.swing.JTextArea();
        jPanel2 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        ComboBox_COM = new javax.swing.JComboBox();
        ComboBox_kohzuCOM = new javax.swing.JComboBox();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jComboBox_Target = new javax.swing.JComboBox();
        confirmBtn = new javax.swing.JButton();
        cancelBtn = new javax.swing.JButton();

        jDialog_.setTitle("操作說明");
        jDialog_.setMinimumSize(new java.awt.Dimension(425, 275));

        jButton_ok.setText("OK");
        jButton_ok.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton_okActionPerformed(evt);
            }
        });

        jPanel6.setBorder(javax.swing.BorderFactory.createTitledBorder("操作說明"));

        jTextArea1.setEditable(false);
        jTextArea1.setColumns(20);
        jTextArea1.setRows(5);
        jTextArea1.setText("1. 選擇 Target number\n2. 按下 Confirm 確認 Target (按下 Cancel 取消選取)\n3. 選擇 COM Port (連接成功燈泡會亮起)\n4. 完成步驟 2 跟 3 後，Start 會亮起\n5. 按下STOP，Reset、Continue亮起\n");
        jTextArea1.setAutoscrolls(false);
        jScrollPane3.setViewportView(jTextArea1);

        javax.swing.GroupLayout jPanel6Layout = new javax.swing.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane3, javax.swing.GroupLayout.Alignment.TRAILING)
        );
        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel6Layout.createSequentialGroup()
                .addContainerGap(21, Short.MAX_VALUE)
                .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        javax.swing.GroupLayout jDialog_Layout = new javax.swing.GroupLayout(jDialog_.getContentPane());
        jDialog_.getContentPane().setLayout(jDialog_Layout);
        jDialog_Layout.setHorizontalGroup(
            jDialog_Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jDialog_Layout.createSequentialGroup()
                .addContainerGap(188, Short.MAX_VALUE)
                .addComponent(jButton_ok)
                .addGap(188, 188, 188))
            .addGroup(jDialog_Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel6, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        jDialog_Layout.setVerticalGroup(
            jDialog_Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jDialog_Layout.createSequentialGroup()
                .addContainerGap(43, Short.MAX_VALUE)
                .addComponent(jPanel6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButton_ok)
                .addGap(27, 27, 27))
        );

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("HIWIN_Control");
        setBackground(new java.awt.Color(153, 153, 153));
        addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentHidden(java.awt.event.ComponentEvent evt) {
                formComponentHidden(evt);
            }
        });

        jPanel1.setBackground(new java.awt.Color(220, 220, 220));

        jPanel3.setBorder(javax.swing.BorderFactory.createTitledBorder("Control Panel"));

        readySig.setEditable(false);
        readySig.setBackground(new java.awt.Color(0, 255, 0));
        jScrollPane5.setViewportView(readySig);

        jLabel_status.setFont(new java.awt.Font("Franklin Gothic Demi", 0, 12)); // NOI18N
        jLabel_status.setText("Ready");

        startBtn.setFont(new java.awt.Font("Franklin Gothic Demi", 0, 12)); // NOI18N
        startBtn.setText("Run");
        startBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                startBtnActionPerformed(evt);
            }
        });

        ResetBtn.setFont(new java.awt.Font("Franklin Gothic Demi Cond", 0, 12)); // NOI18N
        ResetBtn.setText("Reset");
        ResetBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ResetBtnActionPerformed(evt);
            }
        });

        EMGBtn.setFont(new java.awt.Font("Franklin Gothic Demi Cond", 0, 12)); // NOI18N
        EMGBtn.setText("Stop");
        EMGBtn.setEnabled(false);
        EMGBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                EMGBtnActionPerformed(evt);
            }
        });

        ContinueBtn.setFont(new java.awt.Font("Franklin Gothic Demi Cond", 0, 12)); // NOI18N
        ContinueBtn.setText("Continue");
        ContinueBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ContinueBtnActionPerformed(evt);
            }
        });

        jRadioButton1.setText("One Target");
        jRadioButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jRadioButton1ActionPerformed(evt);
            }
        });

        jRadioButton2.setText("Auto");
        jRadioButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jRadioButton2ActionPerformed(evt);
            }
        });

        jButton_align.setText("Align");

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGap(0, 0, Short.MAX_VALUE)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(ResetBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 77, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 34, Short.MAX_VALUE)
                        .addComponent(ContinueBtn))
                    .addComponent(EMGBtn, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(startBtn, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(jRadioButton2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jRadioButton1)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(jScrollPane5, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(jLabel_status)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jButton_align)))
                .addGap(0, 0, Short.MAX_VALUE))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jRadioButton1)
                    .addComponent(jRadioButton2))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(startBtn)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(EMGBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(ResetBtn)
                    .addComponent(ContinueBtn))
                .addGap(18, 18, 18)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jScrollPane5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel_status, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jButton_align))))
        );

        jRadioButton1.getAccessibleContext().setAccessibleName("jRadioButton_OneTarget");
        jRadioButton2.getAccessibleContext().setAccessibleName("jRadioButton_Auto");

        Text_T.setEditable(false);
        Text_T.setBackground(java.awt.SystemColor.control);
        Text_T.setColumns(20);
        Text_T.setRows(5);
        Text_T.setBorder(null);
        Text_T.setOpaque(false);
        jScrollPane1.setViewportView(Text_T);

        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder("COM & Target"));

        jLabel1.setFont(new java.awt.Font("Franklin Gothic Demi", 0, 12)); // NOI18N
        jLabel1.setText("HIWIN ");

        ComboBox_COM.setBackground(java.awt.Color.lightGray);
        ComboBox_COM.setFont(new java.awt.Font("Franklin Gothic Demi Cond", 0, 12)); // NOI18N
        ComboBox_COM.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "COM1", "COM2", "COM3", "COM4", "COM5", "COM6", "COM7", "COM8", "COM9", "COM10" }));
        ComboBox_COM.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ComboBox_COMActionPerformed(evt);
            }
        });

        ComboBox_kohzuCOM.setFont(new java.awt.Font("Franklin Gothic Demi Cond", 0, 12)); // NOI18N
        ComboBox_kohzuCOM.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "COM1", "COM2", "COM3", "COM4", "COM5", "COM6", "COM7", "COM8", "COM9", "COM10" }));

        jLabel2.setFont(new java.awt.Font("Franklin Gothic Demi", 0, 12)); // NOI18N
        jLabel2.setText("KOHZU");

        jLabel3.setFont(new java.awt.Font("Franklin Gothic Demi", 0, 12)); // NOI18N
        jLabel3.setText("Target number");

        jComboBox_Target.setFont(new java.awt.Font("Franklin Gothic Demi", 0, 12)); // NOI18N
        jComboBox_Target.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "1", "2", "3", "4", "5", "6", "7", "8", "9", "10" }));

        confirmBtn.setFont(new java.awt.Font("Franklin Gothic Demi", 0, 12)); // NOI18N
        confirmBtn.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/image/confirm.png"))); // NOI18N
        confirmBtn.setText("Confirm");
        confirmBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                confirmBtnActionPerformed(evt);
            }
        });

        cancelBtn.setFont(new java.awt.Font("Franklin Gothic Demi", 0, 12)); // NOI18N
        cancelBtn.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/image/delete (1).png"))); // NOI18N
        cancelBtn.setText("Cancel");
        cancelBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelBtnActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel2)
                            .addComponent(jLabel1))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(ComboBox_COM, javax.swing.GroupLayout.PREFERRED_SIZE, 78, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(ComboBox_kohzuCOM, javax.swing.GroupLayout.PREFERRED_SIZE, 78, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jLabel3)
                        .addGap(46, 46, 46)
                        .addComponent(jComboBox_Target, javax.swing.GroupLayout.PREFERRED_SIZE, 78, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE))))
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(57, 57, 57)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(cancelBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 95, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(confirmBtn))
                .addGap(0, 0, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(ComboBox_COM, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(ComboBox_kohzuCOM, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel2))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(jComboBox_Target, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(confirmBtn)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(cancelBtn))
        );

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jScrollPane1)
                        .addContainerGap())
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 10, Short.MAX_VALUE))))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 11, Short.MAX_VALUE)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 101, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jLabel4))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(26, 26, 26)
                .addComponent(jLabel4)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents
//<editor-fold defaultstate="collapsed" desc=" Target and cancel button ">
 

    private void jButton_okActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton_okActionPerformed
        jDialog_.dispose();// TODO add your handling code here:
    }//GEN-LAST:event_jButton_okActionPerformed

    private void formComponentHidden(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_formComponentHidden
        // TODO add your handling code here:
    }//GEN-LAST:event_formComponentHidden

    private void ResetBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ResetBtnActionPerformed
        //tell robot arm to initialize
        Timer_CheckPos.cancel();
        Timer_HIWIN.cancel();
        confirmBtn.setEnabled(true);
        cancelBtn.setEnabled(false);
        startBtn.setEnabled(false);
        ResetBtn.setEnabled(false);
        ContinueBtn.setEnabled(false);
        EMGBtn.setEnabled(false);
        readySig.setBackground(Color.GREEN);
        jLabel_status.setText("Ready");
        NowStatus = 1;
        ComboBox_kohzuCOM.setEnabled(true);
        ComboBox_COM.setEnabled(true);
        jComboBox_Target.setEnabled(true);
        //change status from 6 to 4

    }//GEN-LAST:event_ResetBtnActionPerformed

    private void ContinueBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ContinueBtnActionPerformed
        //change status from 6 to 4
        EMG = false;
        if(STEP!=0){
            NowStatus = 4;
            EMGBtn.setEnabled(true);
            ResetBtn.setEnabled(false);
            ContinueBtn.setEnabled(false);
        }
        else{
            NowStatus = 3;
            EMGBtn.setEnabled(true);
            ResetBtn.setEnabled(false);
            ContinueBtn.setEnabled(false);
        }

    }//GEN-LAST:event_ContinueBtnActionPerformed

    private void EMGBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_EMGBtnActionPerformed
        EMG = true;
        if(TM_HIWIN == true){         //4 means HIWIN programming
            EMGBtn.setEnabled(false);
            jLabel_status.setText("Program stop");
            readySig.setBackground(Color.RED);
            latch_status5 = true;
        }
        else if(NowStatus == 3 && TM_KOHZU == true){
            EMGBtn.setEnabled(false);
            jLabel_status.setText("Program stop");
            readySig.setBackground(Color.RED);
            NowStatus = 6;
        }
    }//GEN-LAST:event_EMGBtnActionPerformed

    private void startBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_startBtnActionPerformed

        /*                      * 1: Record kohzu Z stage position
        * 2: Move stage to the lowest point
        * 3: Check if stage reach lowest point
        * 4: Robot grabs the sample on Air-bearing(controller's job)
        * 5: Put sample to outside holder(controller's job)
        * 6: Grab new sample move to Air-bearing
        * 7: Robot goes back to zero point(controller's job)
        * 8: Read finished signal of controller, then let kohzu go back to original position

        * Use timer to continuously read status signal from hiwin controller
        * */
        startBtn.setEnabled(false);
        cancelBtn.setEnabled(false);
        Timer_CheckPos = new Timer();
        Timer_HIWIN = new Timer();
        Timer_BackPos = new Timer();
        EMGBtn.setEnabled(true);
        //////////////////////////////////////////
        /////////////Step 1: Record kohzu Z stage position  & Step 2: Move stage to the lowest point
        //////////////////////////////////////////
        //////////////////////////////////////////
        //////////enter step
        //////////////////////////////////////////
        latch_kohzu = true;
        NowStatus = 3;
        try {
            Z_original_pos =  readkohzu(KOHZU_COM,z_axis);
            Thread.sleep(50);
        } catch (Exception ex) {
            Logger.getLogger(HIWIN_frame.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            X_original_pos =  readkohzu(KOHZU_COM,x_axis);
        } catch (Exception ex) {
            Logger.getLogger(HIWIN_frame.class.getName()).log(Level.SEVERE, null, ex);
        }
        Text_T.append("Z original pos :" + Z_original_pos + "\n");
        Text_T.append("X original pos :" + X_original_pos + "\n");
        //////////////////////////////////////////
        ///////////// Step 3: Check if stage reach lowest point
        //////////////////////////////////////////
        Timer_CheckPos.schedule(new kohzu_pos(), 50, 1000);
        //Timer_HIWIN.schedule(new HIWIN_CMD(), 50, 1000);
    }//GEN-LAST:event_startBtnActionPerformed

    private void cancelBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelBtnActionPerformed

        Target= "";
        Message_target = 0;
        startBtn.setEnabled(false);
        confirmBtn.setEnabled(true);
        cancelBtn.setEnabled(false);
        NowStatus = 1;
        ComboBox_kohzuCOM.setEnabled(true);
        ComboBox_COM.setEnabled(true);
        jComboBox_Target.setEnabled(true);
    }//GEN-LAST:event_cancelBtnActionPerformed

    private void confirmBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_confirmBtnActionPerformed
        HIWIN_COM = ComboBox_COM.getSelectedItem().toString();
        KOHZU_COM = ComboBox_kohzuCOM.getSelectedItem().toString();
        if(jComboBox_Target.getSelectedItem() != null){
            Target = jComboBox_Target.getSelectedItem().toString();
            Message_target = Integer.parseInt(Target);
            Text_T.append("Target :" + Message_target + "\n");
            NowStatus = 2;
            confirmBtn.setEnabled(false);
            cancelBtn.setEnabled(true);
            startBtn.setEnabled(true);
            ComboBox_kohzuCOM.setEnabled(false);
            ComboBox_COM.setEnabled(false);
            jComboBox_Target.setEnabled(false);
        }

    }//GEN-LAST:event_confirmBtnActionPerformed

    private void jRadioButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jRadioButton2ActionPerformed
        // TODO add your handling code here:
        jRadioButton2.setSelected(true);
        jRadioButton1.setSelected(false);
    }//GEN-LAST:event_jRadioButton2ActionPerformed

    private void jRadioButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jRadioButton1ActionPerformed
        // TODO add your handling code here:
        jRadioButton1.setSelected(true);
        jRadioButton2.setSelected(false);
    }//GEN-LAST:event_jRadioButton1ActionPerformed

    private void ComboBox_COMActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ComboBox_COMActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_ComboBox_COMActionPerformed

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
//            java.util.logging.Logger.getLogger(HIWIN_frame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
//        } catch (InstantiationException ex) {
//            java.util.logging.Logger.getLogger(HIWIN_frame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
//        } catch (IllegalAccessException ex) {
//            java.util.logging.Logger.getLogger(HIWIN_frame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
//        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
//            java.util.logging.Logger.getLogger(HIWIN_frame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
//        }
//        //</editor-fold>
//
//        /* Create and display the form */
//        java.awt.EventQueue.invokeLater(new Runnable() {
//            public void run() {
//                new HIWIN_frame().setVisible(true);
//            }
//        });
//    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JComboBox ComboBox_COM;
    private javax.swing.JComboBox ComboBox_kohzuCOM;
    private javax.swing.JButton ContinueBtn;
    private javax.swing.JButton EMGBtn;
    private javax.swing.JButton ResetBtn;
    private javax.swing.JTextArea Text_T;
    private javax.swing.JButton cancelBtn;
    private javax.swing.JButton confirmBtn;
    private javax.swing.JButton jButton_align;
    private javax.swing.JButton jButton_ok;
    private javax.swing.JComboBox jComboBox_Target;
    private javax.swing.JDialog jDialog_;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel_status;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JRadioButton jRadioButton1;
    private javax.swing.JRadioButton jRadioButton2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane5;
    private javax.swing.JTextArea jTextArea1;
    private javax.swing.JTextPane readySig;
    private javax.swing.JButton startBtn;
    // End of variables declaration//GEN-END:variables

    @Override
    public void propertiesChangedAlert() {
        
    }

    @Override
    public void propertyChangedAlert(String string, String string1, String string2) {
        
    }

    @Override
    public void configGroupChangedAlert(String string, String string1) {
        
    }

    @Override
    public void systemConfigurationLoaded() {
        
    }

    @Override
    public void pixelSizeChangedAlert(double d) {
        
    }

    @Override
    public void stagePositionChangedAlert(String string, double d) {
        
    }

    @Override
    public void xyStagePositionChanged(String string, double d, double d1) {
        
    }

    @Override
    public void exposureChanged(String string, double d) {
        
    }

    @Override
    public void slmExposureChanged(String string, double d) {
        
    }
    
}
    
