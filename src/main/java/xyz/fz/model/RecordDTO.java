package xyz.fz.model;

public class RecordDTO {
    private String id;

    private String host;

    private String method;

    private String url;

    public RecordDTO() {
    }

    public RecordDTO(String id, String host, String method, String url) {
        this.id = id;
        this.host = host;
        this.method = method;
        this.url = url;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public String toString() {
        return "RecordDTO{" +
                "id='" + id + '\'' +
                ", host='" + host + '\'' +
                ", method='" + method + '\'' +
                ", url='" + url + '\'' +
                '}';
    }
}
