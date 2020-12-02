import com.alibaba.fastjson.JSONObject;

public class LanguageData {
    private String langKey;
    private boolean hasLang;
    private JSONObject langJson;

    public String getLangKey() {
        return langKey;
    }

    public void setLangKey(String langKey) {
        this.langKey = langKey;
    }

    public boolean isHasLang() {
        return hasLang;
    }

    public void setHasLang(boolean hasLang) {
        this.hasLang = hasLang;
    }

    public JSONObject getLangJson() {
        return langJson;
    }

    public void setLangJson(JSONObject langJson) {
        this.langJson = langJson;
    }
}
