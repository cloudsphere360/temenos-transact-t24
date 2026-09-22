package com.temenos.fusion;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

import com.temenos.api.TStructure;
import com.temenos.t24.api.complex.eb.servicehook.ServiceData;
import com.temenos.t24.api.complex.eb.servicehook.SynchronousTransactionData;
import com.temenos.t24.api.complex.eb.servicehook.TransactionControl;
import com.temenos.t24.api.hook.system.ServiceLifecycle;

import com.temenos.t24.api.records.ebfileuploadparam.EbFileUploadParamRecord;
import com.temenos.t24.api.system.DataAccess;

/**
 *
 * @author jh116454
 *
 */
public class FfAutoRecCorrSrtn extends ServiceLifecycle {

    private static final FusionFileLogger yAutoCorrLog = FusionFileLogger.getLogger(FfAutoRecCorrSrtn.class);

    File[] listOfFiles = null;
    DataAccess yDatAccs = new DataAccess(this);
    List<String> yRecIds = new ArrayList<>();

    String yMultiPath = "";
    String yCollScrnID = "";
    String yCashinBranch = "";

    File yActualFile = null;
    String st;
    String yCashAmt = "";

    String yEmpID = "";
    String yEmpName = "";
    
  
    
    @Override
    public List<String> getIds(ServiceData serviceData, List<String> controlList) {
        yAutoCorrLog.info("  ------------- getid Inside-------------");
        yRecIds.add("DUMMY_ID");
        yAutoCorrLog.info("---Dummy Ids---");
        return yRecIds;
    }

    @Override
    public void updateRecord(String id, ServiceData serviceData, String controlItem,
            TransactionControl transactionControl, List<SynchronousTransactionData> transactionData,
            List<TStructure> records) {

        getTransEbparam();
        File folder = new File(yMultiPath);
        listOfFiles = folder.listFiles();
        getCheckValues(transactionData);

    }

    private void getTransEbparam() {

        try {
            EbFileUploadParamRecord yParamRec = new EbFileUploadParamRecord(
                    yDatAccs.getRecord("EB.FILE.UPLOAD.PARAM", "SYSTEM"));
            String yTcUploadPath = yParamRec.getTcUploadPath().getValue();
            yMultiPath = yTcUploadPath + "/TELLRAUTOCORR";
            yAutoCorrLog.info(" path -> " + yMultiPath);
        } catch (Exception e) {
            yAutoCorrLog.info("Param Rec Missing");
        }
    }

    private void getCheckValues(List<SynchronousTransactionData> transactionData) {

        for (File file : listOfFiles) {
            yActualFile = file;
            yAutoCorrLog.info(" File -> " + yActualFile);

            if (yActualFile.getName().endsWith(".csv")) {
                yAutoCorrLog.info("Processing LegType File -> " + yActualFile.getName());
                try (BufferedReader br = new BufferedReader(new FileReader(yActualFile))) {

                    while ((st = br.readLine()) != null) {
                        String[] strs = st.split(",", -1);
                        
                        yCollScrnID = strs[0];
                        yAutoCorrLog.info(" Coll Scrn ID -> " + yCollScrnID);                       
                        
                        getOfsDetails(transactionData);
                        
                    }

                } catch (Exception e) {
                    yAutoCorrLog.info("Folder doesnt exist");
                }

            }

        }
    }
   
    private void getOfsDetails(List<SynchronousTransactionData> transactionData) {
        yAutoCorrLog.info(" get OFS Details");
                
        try {
            String compId = yCollScrnID.split("-")[0];
            yAutoCorrLog.info(" compId -> " + compId);
               
            SynchronousTransactionData txnData = new SynchronousTransactionData();
            yAutoCorrLog.info("pass 1");
            txnData.setVersionId("EB.FF.COLL.POSTING.SCREEN,FF.TELLER.AUTO.CORR");
            yAutoCorrLog.info("pass 2");
            txnData.setTransactionId(yCollScrnID);
            yAutoCorrLog.info("pass 3");
            txnData.setSourceId("FF.PETY.UPD");
            yAutoCorrLog.info("pass 4");
            txnData.setFunction("INPUT");
            yAutoCorrLog.info("pass 5");
            txnData.setCompanyId(compId);
            yAutoCorrLog.info("pass 6");
            transactionData.add(txnData);
            
            yAutoCorrLog.info("txnData  -> " + txnData);
        } catch (Exception e) {
            yAutoCorrLog.info("Record Not Found ->" +e.getMessage());
        }

    }
}
