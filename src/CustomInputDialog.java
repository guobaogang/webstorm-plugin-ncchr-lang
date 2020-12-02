import com.intellij.openapi.ui.DialogWrapper;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.function.Function;

public class CustomInputDialog extends DialogWrapper {

    private String moduleUrl;
    private Function func;

    //swing样式类，定义在4.3.2
    private CustomInputSwing customInputSwing = new CustomInputSwing();

    public CustomInputDialog(@NotNull String moduleUrl, Function func) {
        super(true);
        setTitle("请选择"); //设置会话框标题
        this.moduleUrl = moduleUrl; //获取到当前项目的路径
        this.func = func;
        init(); //触发一下init方法，否则swing样式将无法展示在会话框
    }

    @Override
    protected JComponent createNorthPanel() {
        //return customInputSwing.initNorth(); //返回位于会话框north位置的swing样式
        return null;
    }

    // 特别说明：不需要展示SouthPanel要重写返回null，否则IDEA将展示默认的"Cancel"和"OK"按钮
    @Override
    protected JComponent createSouthPanel() {
        return customInputSwing.initSouth(this.func);
    }

    @Override
    protected JComponent createCenterPanel() {
        //定义表单的主题，放置到IDEA会话框的中央位置
        return customInputSwing.initCenter(this.moduleUrl);
    }
}