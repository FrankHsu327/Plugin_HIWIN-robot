/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org;

import org.micromanager.api.ScriptInterface;
import org.micromanager.api.MMPlugin;
import mmcorej.CMMCore;

/**
 *
 * @author rfv84
 */
public class ControlPanel implements org.micromanager.api.MMPlugin {
    public static final String menuName = "HIWIN_ControlPanel";
    public static final String tooltipDescription = "Displays a simple dialog";
    
    // Provides access to the Micro-Manager Java API (for GUI control and high-
    // level functions).
    private ScriptInterface gui_;

    // Provides access to the Micro-Manager Core API (for direct hardware
    // control)    
    private CMMCore core_;
    
    private HIWIN_frame myFrame_;
    @Override
    public void dispose() {
         //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setApp(ScriptInterface app) {
       gui_ = app;
       core_ = app.getMMCore();
       
       if(myFrame_ == null){
           myFrame_ = new HIWIN_frame(gui_);
       }
       myFrame_.setVisible(true);
       myFrame_.initialize();
    }

    @Override
    public void show() {
//        JOptionPane.showMessageDialog(null, "Hello, world!", "HIWIN_ControlPanel",JOptionPane.PLAIN_MESSAGE); /
    }

    @Override
    public String getDescription() {
        return tooltipDescription; //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String getInfo() {
        return "HIWIN_Control Panel"; //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String getVersion() {
        return "1.0"; //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String getCopyright() {
        return "Cheng-Huan Hsu, 2019"; //To change body of generated methods, choose Tools | Templates.
    }
    
}
