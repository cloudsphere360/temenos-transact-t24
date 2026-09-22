package com.temenos.fusion;


import java.io.File;

import java.util.ArrayList;
import java.util.List;

import com.temenos.t24.api.complex.eb.servicehook.ServiceData;
import com.temenos.t24.api.hook.system.ServiceLifecycle;
import com.temenos.t24.api.records.ebfileuploadparam.EbFileUploadParamRecord;
import com.temenos.t24.api.system.DataAccess;

/**
 *
 * @author jh116454
 *
 */
public class FfAutoCorrPostSrtn extends ServiceLifecycle {

    private static final FusionFileLogger yAutoCorrPostLog = FusionFileLogger.getLogger(FfAutoCorrPostSrtn.class);

    DataAccess yDatAccs = new DataAccess(this);
    List<String> yRecIds = new ArrayList<>();

    String yMultiPath = "";
    
    @Override
    public void processSingleThreaded(ServiceData serviceData) {
        getTransEbparam();
        File folder = new File(yMultiPath);
        File[] listOfFiles = folder.listFiles();
        
        for (File file : listOfFiles) {
            if (file.getName().endsWith(".csv")) {
                boolean yBooFile = file.delete();
                yAutoCorrPostLog.info(" File Deleted -> "+ yBooFile);
            }

        }
    }

    private void getTransEbparam() {

        try {
            EbFileUploadParamRecord yParamRec = new EbFileUploadParamRecord(
                    yDatAccs.getRecord("EB.FILE.UPLOAD.PARAM", "SYSTEM"));
            String yTcUploadPath = yParamRec.getTcUploadPath().getValue();
            yMultiPath = yTcUploadPath + "/TELLRAUTOCORR";
            yAutoCorrPostLog.info(" path -> " + yMultiPath);
        } catch (Exception e) {
            yAutoCorrPostLog.info("Param Rec Missing");
        }
    }
}