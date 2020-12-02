import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.impl.EditorImpl;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.TextRange;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.parser.Feature;
import com.alibaba.fastjson.serializer.SerializerFeature;

import java.io.*;
import java.lang.reflect.Array;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReplaceLangAction extends AnAction {
    private Project project;
    private Document document;
    private MySelection mySelection;
    private Caret primaryCaret;
    private ProjectModule projectModule;
    private CustomInputDialog customInputDialog;

    @Override
    public void actionPerformed(AnActionEvent event) {
        // Get all the required data from data keys
        final Editor editor = event.getRequiredData(CommonDataKeys.EDITOR);
        project = event.getRequiredData(CommonDataKeys.PROJECT);
        document = editor.getDocument();

        if ("json".equals(((EditorImpl) editor).getVirtualFile().getExtension())) {
            Messages.showInfoMessage("不可以在json文件中使用！", "提示");
            return;
        }
        // Work off of the primary caret to get the selection info
        primaryCaret = editor.getCaretModel().getPrimaryCaret();
        mySelection = getMySelection(primaryCaret, document);
        if (mySelection.isEmpty()) {
            Messages.showInfoMessage("未选中任何文字！", "提示");
            return;
        }
        String filePath = ((EditorImpl) editor).getVirtualFile().getPath();
        projectModule = getCurrFileInfo(filePath);
        Function handleLangFile = fileInfo -> {
            customInputDialog.close(0);
            String prefix = (String) Array.get(fileInfo, 0);
            String fileName = (String) Array.get(fileInfo, 1);
            int start = mySelection.getStart();
            int end = mySelection.getEnd();
            String selectText = mySelection.getText();
            String targetFilePath = getTargetFile(projectModule.getModuleDir(), fileName);
            File jsonFile = new File(targetFilePath);
            String jsonDoc = null;
            try {
                jsonDoc = stream2String(new FileInputStream(jsonFile), "UTF-8");
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            JSONObject jsonObj = JSON.parseObject(jsonDoc, Feature.OrderedField);
            LanguageData languageData = handleLangData(jsonObj, selectText, projectModule.getModule());
            Boolean inObjectFlag = isInObject(document, end);
            String langKey = "";
            if (inObjectFlag) {
                langKey = prefix + "[\"" + languageData.getLangKey() + "\"] /*多语: " + selectText + " */";
            } else {
                langKey = "{" + prefix + "[\"" + languageData.getLangKey() + "\"] /*多语: " + selectText + " */}";
            }
            // Replace the selection with a fixed string.
            // Must do this document change in a write action context.
            String finalLangKey = langKey;
            WriteCommandAction.runWriteCommandAction(project, () -> {
                        document.replaceString(start, end, finalLangKey);
                        if (!languageData.isHasLang()) {
                            //格式化json数据
                            String pretty = JSON.toJSONString(languageData.getLangJson(), SerializerFeature.PrettyFormat);
                            try {
                                // get the content in bytes
                                FileOutputStream fos = new FileOutputStream(jsonFile);
                                OutputStreamWriter osw = new OutputStreamWriter(fos, "UTF-8");
                                osw.write(pretty.replaceAll(":", ": "));
                                osw.flush();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
            );

            // De-select the text range that was just replaced
            primaryCaret.removeSelection();

            return fileInfo;
        };

        customInputDialog = new CustomInputDialog(projectModule.getModuleDir(), handleLangFile);
        customInputDialog.setResizable(true); //是否允许用户通过拖拽的方式扩大或缩小你的表单框，我这里定义为true，表示允许
        customInputDialog.show();
    }

    /**
     * 获取当前文件信息
     * 需要解析出module,当前模块路径等信息
     *
     * @return 当前文件所在模块及模块路径
     */
    public ProjectModule getCurrFileInfo(String filePath) {
        String[] pathArr = filePath.split("src");
        String workSpace = pathArr[0];
        String moduleSpace = pathArr[1];
        String module = moduleSpace.split("/")[1];
        String moduleDir = workSpace + "src/" + module;
        ProjectModule projectModule = new ProjectModule();
        projectModule.setModule(module);
        projectModule.setModuleDir(moduleDir);
        return projectModule;
    }

    /**
     * 获取目标多语文件
     *
     * @param moduleDir 当前文件路径
     * @return 多语文件路径
     */
    public String getTargetFile(String moduleDir, String fileName) {
        return moduleDir + "/public/lang/standard/simpchn/" + fileName;
    }

    /**
     * 处理多语，如果当前有改多语，返回key值，如果没有，添加多语后，写文件，返回key值
     *
     * @param langJson 当前多语文件的json
     * @param text     需要替换的文字
     * @param module   当前模块
     * @return 替换后的多语信息
     */
    public LanguageData handleLangData(JSONObject langJson, String text, String module) {
        LanguageData languageData = new LanguageData();
        String curKey = "";
        for (Object key : langJson.keySet()) {
            if (text.equals(langJson.get(key))) {
                curKey = (String) key;
                break;
            }
        }
        if (!curKey.isEmpty()) {
            languageData.setHasLang(true);
            languageData.setLangKey(curKey);
            return languageData;
        }
        String lastKey = (String) langJson.keySet().toArray()[langJson.keySet().toArray().length - 1];
        String nextKey = "";
        if (null == lastKey) {
            nextKey = module + "-" + "000000";
        } else {
            nextKey = getNextKey(lastKey);
        }
        langJson.put(nextKey, text);
        languageData.setHasLang(false);
        languageData.setLangKey(nextKey);
        languageData.setLangJson(langJson);
        return languageData;
    }

    /**
     * 获取下一个key值
     *
     * @param key 当前key值
     * @return 下一个key值
     */
    public String getNextKey(String key) {
        String[] keyArr = key.split("-");
        int nextKeyNum = Integer.parseInt(keyArr[1]) + 1;
        return keyArr[0] + "-" + String.format("%06d", nextKeyNum);
    }

    /**
     * 简单(不一定适用所有情况)判断需要替换的字符串是否在对象中
     * 判断下一个出现的字符是'<'还是'}'
     * 如果下一个出现的是}说明当前在对象中，否则在html标签中
     *
     * @param document 源文档
     * @param end      选中字符串结束位置
     * @return 是否在对象中
     */
    public Boolean isInObject(Document document, int end) {
        String afterText = document.getText(new TextRange(end + 1, document.getTextLength()));
        String pattern = "<|}";
        String strBefore = "";
        Pattern findPattern = Pattern.compile(pattern);
        Matcher matcher = findPattern.matcher(afterText);
        if (matcher.find()) {
            strBefore = matcher.group();
        }
        return "}".equals(strBefore);
    }

    /**
     * 获取选中的文字,如果选中文字前后包含引号，则去掉引号，
     * 如果不包含，则扩展选中前后两个字符，如果是引号，同样去掉引号，开始结束位置扩展
     *
     * @param primaryCaret 选中文字相关
     * @param document     选中文字所在文档
     * @return 选中是否为空，选中文字，选中开始结束位置
     */
    public MySelection getMySelection(Caret primaryCaret, Document document) {
        MySelection mySelection = new MySelection();
        String selectText = primaryCaret.getSelectedText();
        int start = primaryCaret.getSelectionStart();
        int end = primaryCaret.getSelectionEnd();
        if (null == selectText) {
            mySelection.setEmpty(true);
        } else {
            //当前选中是否包含引号
            boolean curHasQuotation = false;
            String reg = "^['|\"](.*)['|\"]$";
            if (selectText.startsWith("\"") || selectText.startsWith("'")) {
                curHasQuotation = true;
                selectText = selectText.replaceAll(reg, "$1");
                mySelection.setEmpty(false);
                mySelection.setText(selectText);
                mySelection.setStart(start);
                mySelection.setEnd(end);
            }
            if (!curHasQuotation) {
                String newText = document.getText(new TextRange(start - 1, end + 1));
                //选中前后字符后是否包含引号
                boolean newHasQuotation = false;
                if (newText.startsWith("'") || newText.startsWith("\"")) {
                    newHasQuotation = true;
                    newText = newText.replaceAll(reg, "$1");
                }
                if (newHasQuotation) {
                    mySelection.setEmpty(false);
                    mySelection.setText(newText);
                    mySelection.setStart(start - 1);
                    mySelection.setEnd(end + 1);
                } else {
                    mySelection.setEmpty(false);
                    mySelection.setText(selectText);
                    mySelection.setStart(start);
                    mySelection.setEnd(end);
                }
            }
        }
        return mySelection;
    }

    /**
     * 文件转换为字符串
     *
     * @param in      字节流
     * @param charset 文件的字符集
     * @return 文件内容
     */
    public static String stream2String(InputStream in, String charset) {
        StringBuffer sb = new StringBuffer();
        try {
            Reader r = new InputStreamReader(in, charset);
            int length = 0;
            for (char[] c = new char[1024]; (length = r.read(c)) != -1; ) {
                sb.append(c, 0, length);
            }
            r.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sb.toString();
    }
}
