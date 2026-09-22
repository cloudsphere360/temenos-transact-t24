package com.temenos.fusion;


import java.util.List;

import com.temenos.api.TField;
import com.temenos.api.TStructure;
import com.temenos.logging.facade.Logger;
import com.temenos.logging.facade.LoggerFactory;
import com.temenos.t24.api.complex.eb.servicehook.TransactionData;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.aaarrangement.AaArrangementRecord;
import com.temenos.t24.api.records.account.AccountRecord;
import com.temenos.t24.api.records.customer.CustomerRecord;
import com.temenos.t24.api.records.ebffldeceasedinfo.EbFflDeceasedInfoRecord;
import com.temenos.t24.api.records.ebffldeceasedinfo.FfCurDodStsClass;
import com.temenos.t24.api.records.ebffldeceasedlogs.EbFflDeceasedLogsRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;
import com.temenos.t24.api.tables.ebffldeceasedlogs.EbFflDeceasedLogsTable;


public class VerDeathInfoUpdateRtn extends RecordLifecycle{
    private static final String L3API = "L3API";
    private static final Logger verLogger = LoggerFactory.getLogger(L3API);
    public static final String REJECTED_STS="REJECTED";
    VerDeathDateUpdate verDeathUpdObj=new VerDeathDateUpdate();
    DataAccess daObj =null;
    Session ssObj =null;
    @Override
    public void postUpdateRequest(String application, String currentRecordId, TStructure currentRecord,
            List<TransactionData> transactionData, List<TStructure> currentRecords,
            TransactionContext transactionContext) {
        
        daObj = new DataAccess(this);
        ssObj = new Session(this);
        try {
            String remarks="";
            CustomerRecord cusRec = new CustomerRecord(currentRecord);
            String notDeathDate = cusRec.getNotificationOfDeath().getValue();
            String currStatus = cusRec.getLocalRefField("FF.CUR.DOD.STS").getValue();
            List<TField> remarksList=cusRec.getText();
            if(!remarksList.isEmpty()) {
             remarks = remarksList.get(remarksList.size()-1).getValue();
            }
            verLogger.info("deathDate55 " + notDeathDate);
            if (currStatus.equals(REJECTED_STS)||!(currStatus.isEmpty() && notDeathDate.isEmpty())) {
                List<String> accountRecList = verDeathUpdObj.getAccountList(daObj,currentRecordId);
                for (String accId : accountRecList) {
                    processArrangement(accId,notDeathDate,currStatus,remarks,transactionData,currentRecords);
                    

                }

            }
        } catch (Exception e) {
            verLogger.info(e.getMessage());
        }

}
    
    /**
     * @param notDeathDate
     * @param currStatus
     * @param remarks
     * @param currentRecords 
     * @param transactionData 
     * @param remarks2 
     * @param daObj
     * @param compMne
     */
    private void processArrangement(String accId, String notDeathDate, String currStatus, String remarks, List<TransactionData> transactionData, List<TStructure> currentRecords) {
        String compMne = ssObj.getCompanyRecord().getFinancialMne().getValue();
        try {
            AccountRecord accRec = new AccountRecord(daObj.getRecord(compMne, "ACCOUNT", "", accId));
            
            String arrId = accRec.getArrangementId().getValue();
            if (!arrId.isEmpty()) {
                AaArrangementRecord aaObj = new AaArrangementRecord(
                        daObj.getRecord(compMne, "AA.ARRANGEMENT", "", arrId));
                String coCode = aaObj.getCoCodeRec().getValue();
            
                if (aaObj.getArrStatus().getValue().equals("CURRENT")) {
              
                    EbFflDeceasedInfoRecord ebFflDeceasedObj=new EbFflDeceasedInfoRecord(daObj.getRecord(compMne, "EB.FFL.DECEASED.INFO", "", arrId));
                    //Change status based confirmation
                    verLogger.info("Before update "+ebFflDeceasedObj);
                    if (currStatus.equals ("CONFIRMED")||currStatus.equals("WRITEOFF")||currStatus.equals(REJECTED_STS)||currStatus.equals("PAYOFF")) {
                        verLogger.info("inside confirm");
                        ebFflDeceasedObj= updateFinalDeathDetails( notDeathDate,ebFflDeceasedObj,currStatus,remarks);
                        verLogger.info("calling updateOfs");
                        verDeathUpdObj.updateOfs(transactionData, ebFflDeceasedObj, currentRecords, arrId, coCode);
                        updateFFlLog(notDeathDate,currStatus,remarks,arrId,daObj,compMne);
                    }
                }
            }
        }catch (Exception e) {
            verLogger.info(e.getMessage());
        }
        
    }

    /**
     * @param notDeathDate
     * @param currStatus
     * @param remarks
     * @param arrId
     * @param daObj
     * @param compMne 
     */
    private void updateFFlLog(String notDeathDate, String currStatus, String remarks, String arrId, DataAccess daObj, String compMne) {
      try {
          EbFflDeceasedLogsTable fflLogTableObj=new EbFflDeceasedLogsTable(this);
          EbFflDeceasedLogsRecord fflLogRecObj=verDeathUpdObj.readEbFfLogRecord(compMne, daObj, arrId);
          if(!currStatus.equals(REJECTED_STS)) {
              fflLogRecObj.getFfVerDodConf().setValue(notDeathDate);
  }
          int ffDodSize = 0;
          com.temenos.t24.api.records.ebffldeceasedlogs.FfCurDodStsClass ffCurDodStsClassObj = new com.temenos.t24.api.records.ebffldeceasedlogs.FfCurDodStsClass();
          ffCurDodStsClassObj.setFfCurDodSts(currStatus);
          ffCurDodStsClassObj.setFfCurDodDate(notDeathDate);
          ffCurDodStsClassObj.setFfDodStsAct(remarks);
      
             ffDodSize = fflLogRecObj.getFfCurDodSts().size();
             fflLogRecObj.setFfCurDodSts(ffCurDodStsClassObj, ffDodSize);  
             fflLogTableObj.write(arrId, fflLogRecObj);
          
             
      }catch (Exception e) {
        verLogger.info(e.getMessage());
    }
    }
    

    /**
     * @param currentRecords
     * @param transactionData
     * @param activityId
     * @param aaObj
     * @param arrId
     * @param contractObj
     * @param notDeathDate
     * @param currStatus 
     * @param remarks 
     * @param deathDate 
     * @param ebFfLoanDetailsObj2 
     * @return 
     */
    public EbFflDeceasedInfoRecord updateFinalDeathDetails(String notDeathDate, EbFflDeceasedInfoRecord ebFflDeceasedObj, String currStatus, String remarks) {
        try {
if(!currStatus.equals(REJECTED_STS)) {
            ebFflDeceasedObj.getFfVerDodConf().setValue(notDeathDate);
}
            List<FfCurDodStsClass> currDodStsList = ebFflDeceasedObj.getFfCurDodSts();
           
            FfCurDodStsClass ffCurDodStsObj=new FfCurDodStsClass();
            ffCurDodStsObj.setFfCurDodSts(currStatus);
            ffCurDodStsObj.setFfCurDodDate(notDeathDate);
            ffCurDodStsObj.setFfDodStsAct(remarks);
            
            ebFflDeceasedObj.setFfCurDodSts(ffCurDodStsObj, currDodStsList.size());
            verLogger.info("ebFflDeceasedObj "+ebFflDeceasedObj);
        }catch (Exception e) {
            verLogger.info(e.getMessage());
        }
        return ebFflDeceasedObj;
    }
}
