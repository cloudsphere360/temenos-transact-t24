package com.tem.msg.fusion;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.LinkedHashMap;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "arg5", "arg4", "arg3", "arg2", "arg1" })
public class Params {

    @JsonProperty("arg5")
    private String arg5;

    @JsonProperty("arg4")
    private String arg4;

    @JsonProperty("arg3")
    private String arg3;

    @JsonProperty("arg2")
    private String arg2;

    @JsonProperty("arg1")
    private String arg1;

    @JsonIgnore
    private Map<String, Object> additionalProperties = new LinkedHashMap<>();

    // --- arg5 ---
    @JsonProperty("arg5")
    public String getArg5() {
        return arg5;
    }

    @JsonProperty("arg5")
    public void setArg5(String arg5) {
        this.arg5 = arg5;
    }

    // --- arg4 ---
    @JsonProperty("arg4")
    public String getArg4() {
        return arg4;
    }

    @JsonProperty("arg4")
    public void setArg4(String arg4) {
        this.arg4 = arg4;
    }

    // --- arg3 ---
    @JsonProperty("arg3")
    public String getArg3() {
        return arg3;
    }

    @JsonProperty("arg3")
    public void setArg3(String arg3) {
        this.arg3 = arg3;
    }

    // --- arg2 ---
    @JsonProperty("arg2")
    public String getArg2() {
        return arg2;
    }

    @JsonProperty("arg2")
    public void setArg2(String arg2) {
        this.arg2 = arg2;
    }

    // --- arg1 ---
    @JsonProperty("arg1")
    public String getArg1() {
        return arg1;
    }

    @JsonProperty("arg1")
    public void setArg1(String arg1) {
        this.arg1 = arg1;
    }

    // --- Additional dynamic properties ---
    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }
}