package com.linkedin.drelephant.exceptions;

import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class HadoopException {
    private final Logger logger = Logger.getLogger(HadoopException.class);
//    private String message;
//    private String stackTrace;
//    private String type;
//
//    public String getMessage(){
//        return this.message;
//    }
//
//    public String getStackTrace(){
//        return this.stackTrace;
//    }
//
//    public String getType(){
//        return this.type;
//    }
//
//    public void setMessage(String message){
//        this.message = message;
//    }
//
//    public void setStackTrace(String stackTrace){
//        this.stackTrace = stackTrace;
//    }
//
//    public void setType(String type){
//        this.type = type;
//    }
    /*private List<List<String>> exceptionChain;
    private String type; //azkaban, script, hadoopjobid, hadooptaskid, other(joboverview)
    private String id;
    public List<List<String>> getExceptionChain() {
        return this.exceptionChain;
    }

    public void setExceptionChain(List<List<String>> s) {
        this.exceptionChain = s;
    }

    public void addException(List<String> s) {         // add exception to exception chain
        this.exceptionChain.add(s);
    }

    public void setType(String s){
        this.type=s;
    }
    public String getType(){
        return this.type;
    }

    public void setId(String s){
        this.id=s;
    }
    public String getId(){
        return this.id;
    }

    public ExceptionLoggingEvent(){
        this.exceptionChain = new ArrayList<List<String>>();
        this.type = "";
        this.id="";
    }*/
    List<ExceptionLoggingEvent> exceptions;

    public HadoopException(){
        this.exceptions = new ArrayList<ExceptionLoggingEvent>();
    }

    public void addException(ExceptionLoggingEvent e){
        exceptions.add(e);
    }

    public List<ExceptionLoggingEvent> getExceptions(){
        return exceptions;
    }

}
