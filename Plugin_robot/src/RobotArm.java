
import org.micromanager.api.ScriptInterface;
import org.micromanager.api.MMPlugin;
import mmcorej.CMMCore;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author rfv84
 */
public class RobotArm implements org.micromanager.api.MMPlugin{
    public static final String menuName = "HIWIN_ControlPanel";
    public static final String tooltipDescription = "Displays a simple dialog";
    
    // Provides access to the Micro-Manager Java API (for GUI control and high-
    // level functions).
    private ScriptInterface gui_;

    // Provides access to the Micro-Manager Core API (for direct hardware
    // control)    
    private CMMCore core_;
    
    private plugin myFrame_;
    @Override
    public void dispose() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setApp(ScriptInterface app) {
        gui_ = app;
        core_ = app.getMMCore();
       
        if(myFrame_ == null){
            myFrame_ = new plugin(gui_);
        }
        myFrame_.setVisible(true);
        myFrame_.initialize();
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void show() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String getDescription() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String getInfo() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String getVersion() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String getCopyright() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
