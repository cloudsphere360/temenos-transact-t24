package com.temenos.fusion;

import java.util.ArrayList;
import java.util.List;
import com.temenos.api.TStructure;
import com.temenos.t24.api.complex.eb.servicehook.ServiceData;
import com.temenos.t24.api.complex.eb.servicehook.SynchronousTransactionData;
import com.temenos.t24.api.complex.eb.servicehook.TransactionControl;
import com.temenos.t24.api.hook.system.ServiceLifecycle;
import com.temenos.t24.api.records.tsaservice.TsaServiceRecord;
import com.temenos.t24.api.system.DataAccess;

public class FfSerCobRptOfs extends ServiceLifecycle {
    DataAccess dataAccess = new DataAccess(this);
 
    @Override
    public List<String> getIds(ServiceData serviceData, List<String> controlList) {
        List<String> serviceList = new ArrayList<>();
       
       
        try {    
            serviceList.add("MFI/FF.COB.INST.UPI.NPA.RPT.EXT");
           serviceList.add("MFI/FF.COB.CAD.BRN.CLS.RPT.EXT");
           serviceList.add("MFI/FF.COB.OVRDUE.LN.CUS.INACC.RPT.EXT");          
           serviceList.add("MFI/FF.CREDIT.BUREAU.REPORT");
           serviceList.add("MFI/FF.CREDIT.BUREAU.MID.REPORT");
           serviceList.add("BNK/FF.CREDIT.BUREAU.REPORT");
           serviceList.add("BNK/FF.COB.INST.UPI.NPA.RPT.EXT");
           serviceList.add("BNK/FF.COB.CAD.BRN.CLS.RPT.EXT");
           serviceList.add("BNK/FF.COB.OVRDUE.LN.CUS.INACC.RPT.EXT");
           serviceList.add("BNK/FF.CREDIT.BUREAU.MID.REPORT");
         
        } catch (Exception e) {
            e.getMessage();
        }
     
        return serviceList;
    }

    @Override
    public void updateRecord(String id, ServiceData serviceData, String controlItem,
            TransactionControl transactionControl, List<SynchronousTransactionData> transactionData,
            List<TStructure> records) {
        try {
                       
            TsaServiceRecord tsaServiceRecord = new TsaServiceRecord(this.dataAccess.getRecord("TSA.SERVICE", id));
            if (tsaServiceRecord.getServiceControl().getValue().equals("STOP")) {
              tsaServiceRecord.setServiceControl("START");
              
              SynchronousTransactionData synchronousTransactionData = new SynchronousTransactionData();
              synchronousTransactionData.setFunction("INPUTT");
              synchronousTransactionData.setNumberOfAuthoriser("0");
              synchronousTransactionData.setSourceId("FF.BM.RPT");
              synchronousTransactionData.setTransactionId(id.replace("/", "^"));
              synchronousTransactionData.setVersionId("TSA.SERVICE,INPUT");
              transactionData.add(synchronousTransactionData);
              records.add(tsaServiceRecord.toStructure());
  
          
            } 
          } catch (Exception e) {
           e.getMessage();
          } 
        }
        
        
}    
        
        
        
        