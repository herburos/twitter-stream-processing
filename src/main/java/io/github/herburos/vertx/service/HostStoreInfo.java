package io.github.herburos.vertx.service;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@DataObject
public class HostStoreInfo {
    private String host;
    private int port;
    private List<String> storeNames;

    public HostStoreInfo(JsonObject json) {
        this.host = json.getString("host");
        this.port = json.getInteger("port");
        List<String> storeNames = json.getJsonArray("storeNames").stream().map(Object::toString).collect(Collectors.toList());
        this.storeNames = storeNames;
    }

    public HostStoreInfo(String host, int port, List<String> storeNames) {
        this.host = host;
        this.port = port;
        this.storeNames = storeNames;
    }

    public HostStoreInfo(String host, int port) {
        this.host = host;
        this.port = port;
        this.storeNames = new ArrayList<>();
    }

    public List<String> getStoreNames() {
        return storeNames;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setStoreNames(List<String> storeNames) {
        this.storeNames = storeNames;
    }

    public JsonObject toJson(){
        JsonObject json = new JsonObject();
        json.put("host", host);
        json.put("port", port);
        json.put("storeNames", new JsonArray(storeNames));
        return json;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HostStoreInfo that = (HostStoreInfo) o;
        return port == that.port &&
                host.equals(that.host);
    }

    @Override
    public int hashCode() {
        return Objects.hash(host, port);
    }

    @Override
    public String toString() {
        return "HostStoreInfo{" +
                "host='" + host + '\'' +
                ", port=" + port +
                ", storeNames=" + storeNames +
                '}';
    }
}
