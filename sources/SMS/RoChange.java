package com.tem.msg.fusion;


import java.util.List;

import com.temenos.api.TStructure;
import com.temenos.t24.api.arrangement.accounting.Contract;
import com.temenos.t24.api.complex.eb.servicehook.TransactionData;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;

import com.temenos.t24.api.records.ebffcentredetail.EbFfCentreDetailRecord;
import com.temenos.t24.api.records.ebffgenericparamsms.EbFfGenericParamSmsRecord;
import com.temenos.t24.api.records.ebffrochange.EbFfRoChangeRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;
import com.temenos.t24.api.tables.ebffrochange.EbFfRoChangeTable;

/**
 * TODO: Document me!
 *
 * @author lm116299
 *
 */
public class RoChange extends RecordLifecycle {
    
    String currentRo = "";
    String previousRo = "";
    String currNo = "";
    String status = "";
    EbFfGenericParamSmsRecord genParam = null;
    EbFfRoChangeRecord logRec = new EbFfRoChangeRecord(this);
    EbFfRoChangeTable logTab = new EbFfRoChangeTable(this);
    EbFfCentreDetailRecord ecd = null;
    String apiStatus = "";
    String description = "";
    Session sn = new Session(this);
    DataAccess da = new DataAccess(this);
    Contract api = new Contract(this);
    int TempCurrNo = 0;

    @Override
    public void postUpdateRequest(String application, String currentRecordId, TStructure currentRecord,
            List<TransactionData> transactionData, List<TStructure> currentRecords,
            TransactionContext transactionContext) {
        
        EbFfCentreDetailRecord cd = new EbFfCentreDetailRecord(currentRecord);
        currentRo = cd.getCurrentRo().getValue();
        System.out.println("currentRo" + currentRo);
        previousRo = cd.getPreviousRo().getValue();
        System.out.println("previousRo" + previousRo);
        status = transactionContext.getCurrentFunction().toString();
        System.out.println("status" + status);
        
          if(status.equalsIgnoreCase("INPUT")) {
              try {
                      String Mne = sn.getCompanyRecord().getFinancialMne().toString();
                      try {
                      ecd = new EbFfCentreDetailRecord(da.getRecord("EB.FF.CENTRE.DETAIL",currentRecordId));
                      }catch(Exception e10) {
                          System.out.println("e10" + e10);
                      }
                      String hisCurrentRo = ecd.getCurrentRo().getValue();
                      System.out.println("hisCurrentRo" + hisCurrentRo);
                      if(hisCurrentRo.equalsIgnoreCase(previousRo)) {
                          try {
                              
                              logRec.getStatus().setValue("ROCHANGE");
                              logRec.getMne().setValue(Mne);
                              logTab.write(currentRecordId, logRec);
                          } catch (Exception e3) {
                              System.out.println("e3" + e3);
                          }
                      }else {
                     
                  }
              }catch(Exception e2) {
                  System.out.println("e2" + e2);
              }
                  
              
              }
          
        }
}
