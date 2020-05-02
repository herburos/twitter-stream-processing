package io.github.herburos.vertx.web;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.annotations.Fluent;
import io.vertx.core.json.JsonObject;

@DataObject(generateConverter = true, publicConverter = false)
public class HelloWorld {
    private String helloWorld;

    public HelloWorld(String helloWorld) {
        this.helloWorld = helloWorld;
    }

    public HelloWorld(JsonObject jsonObject) {
        HelloWorldConverter.fromJson(jsonObject, this);
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        HelloWorldConverter.toJson(this, json);
        return json;
    }

    public String getHelloWorld() {
        return helloWorld;
    }

    @Fluent public HelloWorld setHelloWorld(String helloWorld) {
        this.helloWorld = helloWorld;
        return this;
    }
}
