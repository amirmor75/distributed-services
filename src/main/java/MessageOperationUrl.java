public class MessageOperationUrl {
    private String action;
    private String url;
    public MessageOperationUrl(String action , String url) {
        this.action = action;
        this.url = url;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public String toString() {
        return "MessageOperationUrl{" +
                "action='" + action + '\'' +
                ", url='" + url + '\'' +
                '}';
    }
}
