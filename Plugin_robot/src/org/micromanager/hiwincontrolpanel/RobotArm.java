/*
 * Written by Cheng-Huan Hsu, Institute of Phisics, Academia Sinica, 2021
 */
package org.micromanager.hiwincontrolpanel;

import org.micromanager.api.ScriptInterface;
import org.micromanager.api.MMPlugin;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author rfv84
 */
public class RobotArm implements MMPlugin{
    public static final String menuName = "RobotArm_Hiwin";
    
    // Provides access to the Micro-Manager Java API (for GUI control and high-
    // level functions).
    private ScriptInterface gui_;

    // Provides access to the Micro-Manager Core API (for direct hardware
    // control)    
    
    private HiwinControlFrame myFrame_;
    @Override
    public void dispose() {
        
    }

    @Override
    public void setApp(ScriptInterface app) {
        gui_ = app;
       
        if(myFrame_ == null){
            myFrame_ = new HiwinControlFrame(gui_);
        }
        myFrame_.setVisible(true);
        
    }

    @Override
    public void show() {
        
    }

    @Override
    public String getDescription() {
        return "It is a plugin to control hiwin robot";
    }

    @Override
    public String getInfo() {
        return "RobotArm_control";
    }   

    @Override
    public String getVersion() {
       return "1.1";
    }

    @Override
    public String getCopyright() {
        return "Cheng-Huan Hsu, 2019";
    }

    private static class HiwinControlFrameImpl extends HiwinControlFrame {

        public HiwinControlFrameImpl(ScriptInterface gui_) {
            super(gui_);
        }
    }
    
}
