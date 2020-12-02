import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.Messages;

public class CustomInputSwing {

    private JPanel north = new JPanel();

    private JPanel center = new JPanel();

    private JPanel south = new JPanel();

    //为了让位于底部的按钮可以拿到组件内容，这里把表单组件做成类属性
    private JLabel prefixTitle = new JLabel("请输入多语前缀，如json、this.state.language等");
    private JTextField prefixContent = new JTextField("json");
    private JLabel fileTitle;
    private ComboBox<String> fileNameContent;

    public JPanel initNorth() {

        //定义表单的标题部分，放置到IDEA会话框的顶部位置

        JLabel title = new JLabel("表单标题");
        title.setFont(new Font("微软雅黑", Font.PLAIN, 26)); //字体样式
        title.setHorizontalAlignment(SwingConstants.CENTER); //水平居中
        title.setVerticalAlignment(SwingConstants.CENTER); //垂直居中
        north.add(title);

        return north;
    }

    public JPanel initCenter(String moduleUrl) {

        String langDir = moduleUrl + "/public/lang/standard/simpchn";
        String[] listData = getDirFile(langDir);
        fileTitle = new JLabel("请选择多语文件：");
        fileNameContent = new ComboBox<String>(listData);

        center.setLayout(new GridLayout(4, 1));

        center.add(prefixTitle);
        center.add(prefixContent);

        center.add(fileTitle);
        center.add(fileNameContent);

        return center;
    }

    public JPanel initSouth(Function func) {

        //定义表单的提交按钮，放置到IDEA会话框的底部位置

        JButton submit = new JButton("提交");
        submit.setHorizontalAlignment(SwingConstants.CENTER); //水平居中
        submit.setVerticalAlignment(SwingConstants.CENTER); //垂直居中
        south.add(submit);

        //按钮事件绑定
        submit.addActionListener(e -> {
            String prefix = prefixContent.getText();
            String fileName = fileNameContent.getSelectedItem().toString();
            String[] fileInfo = new String[]{prefix, fileName};
            func.apply(fileInfo);
        });

        return south;
    }

    public String[] getDirFile(String path) {
        File file = new File(path);
        File[] fileList = file.listFiles();
        String[] result = new String[fileList.length];
        for (int i = 0; i < fileList.length; i++) {
            if (fileList[i].isFile()) {
                result[i] = fileList[i].getName();
            }
        }
        return result;
    }
}