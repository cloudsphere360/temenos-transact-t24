package com.temenos.fusion;

import java.util.ArrayList;
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

public class VerDeathDateUpdate extends RecordLifecycle {
    private static final String L3API = "L3API";
    private static final Logger verLogger = LoggerFactory.getLogger(L3API);
//private static final FusionFileLogger verLogger = //FusionFileLogger.getLogger(VerDeathDateUpdate.class);

    DataAccess daObj = null;
    Session ssObj = null;
    @Override
    public void postUpdateRequest(String application, String currentRecordId, TStructure currentRecord,
            List<TransactionData> transactionData, List<TStructure> currentRecords,
            TransactionContext transactionContext) {
verLogger.info("rtn triggered ");

         daObj = new DataAccess(this);
         ssObj = new Session(this);

        try {
            String currStatus = "";
            String remarks = "";
            CustomerRecord cusRec = new CustomerRecord(currentRecord);
            String notDeathDate = cusRec.getDeathDate().getValue();
            List<TField> remarksList = cusRec.getText();
            if (!remarksList.isEmpty()) {
                remarks = remarksList.get(remarksList.size() - 1).getValue();
            }
            currStatus = cusRec.getLocalRefField("FF.CUR.DOD.STS").getValue();
verLogger.info("notDeathDate "+ notDeathDate);
verLogger.info("currStatus "+ currStatus);

            if (!notDeathDate.isEmpty()) {

                List<String> accountRecList = getAccountList(daObj, currentRecordId);
                for (String accId : accountRecList) {
                    processArrangement(accId,notDeathDate, remarks,currStatus,transactionData,currentRecords);
                  

                }                

                
            }
        } catch (Exception e) {
            verLogger.info(e.getMessage());
        }

    }

    /**
     * @param accId
     * @param notDeathDate
     * @param remarks
     * @param currStatus
     * @param transactionData
     * @param currentRecords
     */
    private void processArrangement(String accId, String notDeathDate, String remarks, String currStatus,
            List<TransactionData> transactionData, List<TStructure> currentRecords) {
        String compMne = ssObj.getCompanyRecord().getFinancialMne().getValue();
        try {
           AccountRecord accRec = new AccountRecord(daObj.getRecord(compMne, "ACCOUNT", "", accId));
           String arrId = accRec.getArrangementId().getValue();
verLogger.info("arid  "+ arrId);

           if (!arrId.isEmpty()) {
               AaArrangementRecord aaObj = new AaArrangementRecord(
                       daObj.getRecord(compMne, "AA.ARRANGEMENT", "", arrId));
               String coCode = aaObj.getCoCodeRec().getValue();

               if (aaObj.getArrStatus().getValue().equals("CURRENT")) {
                   if(currStatus.equals("DECEASED") || currStatus.equals("CONFIRMATION")) {
                   EbFflDeceasedInfoRecord ebFflDeceasedObj = readEbFfRecord(compMne, daObj, arrId);

                   if (ebFflDeceasedObj != null) {

                       ebFflDeceasedObj = updateRejectionLoanAccount(notDeathDate, ebFflDeceasedObj, remarks,
                               currStatus);

                       updateOfs(transactionData, ebFflDeceasedObj, currentRecords, arrId, coCode);
                   } else {

                       ebFflDeceasedObj = new EbFflDeceasedInfoRecord(this);
                       ebFflDeceasedObj = updateRejectionLoanAccount(notDeathDate, ebFflDeceasedObj, remarks,
                               currStatus);
                       updateOfs(transactionData, ebFflDeceasedObj, currentRecords, arrId, coCode);
                   }
verLogger.info("update ffllogs  ");

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
verLogger.info("inside ffllogs ");

          EbFflDeceasedLogsTable fflLogTableObj=new EbFflDeceasedLogsTable(this);
          EbFflDeceasedLogsRecord fflLogRecObj=readEbFfLogRecord(compMne, daObj, arrId);
          int ffDodSize = 0;
          com.temenos.t24.api.records.ebffldeceasedlogs.FfCurDodStsClass ffCurDodStsClassObj = new com.temenos.t24.api.records.ebffldeceasedlogs.FfCurDodStsClass();
          ffCurDodStsClassObj.setFfCurDodSts(currStatus);
          ffCurDodStsClassObj.setFfCurDodDate(notDeathDate);
          ffCurDodStsClassObj.setFfDodStsAct(remarks);
verLogger.info("ffCurDodStsClassObj "+ ffCurDodStsClassObj);

     if(fflLogRecObj!=null) {      
             ffDodSize = fflLogRecObj.getFfCurDodSts().size();
             fflLogRecObj.setFfCurDodSts(ffCurDodStsClassObj, ffDodSize);  
             fflLogTableObj.write(arrId, fflLogRecObj);
verLogger.info("fflLogRecObj "+ fflLogRecObj);
     }else {
          fflLogRecObj=new EbFflDeceasedLogsRecord(this);
          fflLogRecObj.setFfCurDodSts(ffCurDodStsClassObj, 0);
          fflLogTableObj.write(arrId, fflLogRecObj);
verLogger.info("fflLogRecObj "+ fflLogRecObj);

     }
          
      }catch (Exception e) {
        verLogger.info(e.getMessage());
    }
    }
    

    /**
     * @param daObj
     * @param currentRecordId
     * @return
     */
    public List<String> getAccountList(DataAccess daObj, String currentRecordId) {
        List<String> accList = new ArrayList<>();
        try {
            accList = daObj.getConcatValues("CUSTOMER.ACCOUNT", currentRecordId);
verLogger.info("accList "+ accList);

        } catch (Exception e) {
            verLogger.info(e.getMessage());
        }
        return accList;
    }

    /**
     * @param transactionData
     * @param ebFfLoanDetailsObj
     * @param currentRecords
     * @param arrId
     * @param compMne
     */
    public void updateOfs(List<TransactionData> transactionData, EbFflDeceasedInfoRecord ebFflDeceasedObj,
            List<TStructure> currentRecords, String arrId, String coCode) {
        try {
            verLogger.info("Inside ofs method");
            TransactionData syncTransactionData = new TransactionData();
            syncTransactionData.setVersionId("EB.FFL.DECEASED.INFO,FF.INSURANCE");
            syncTransactionData.setTransactionId(arrId);
            syncTransactionData.setSourceId("OFS.INSURANCE.UPD");
            syncTransactionData.setCompanyId(coCode);
            syncTransactionData.setFunction("INPUT");
            syncTransactionData.setNumberOfAuthoriser("0");
            transactionData.add(syncTransactionData);
            currentRecords.add(ebFflDeceasedObj.toStructure());
verLogger.info("ebFflDeceasedObj "+ ebFflDeceasedObj);

        } catch (Exception e) {
            verLogger.info(e.getMessage());
        }

    }

    /**
     * @param compMne
     * @param daObj
     * @param arrId
     * @return
     */
    public EbFflDeceasedInfoRecord readEbFfRecord(String compMne, DataAccess daObj, String arrId) {
        EbFflDeceasedInfoRecord ebFflDeceasedObj = null;
        try {
            ebFflDeceasedObj = new EbFflDeceasedInfoRecord(daObj.getRecord(compMne, "EB.FFL.DECEASED.INFO", "", arrId));
        } catch (Exception e) {
            verLogger.info(e.getMessage());
        }
        return ebFflDeceasedObj;
    }
    
    /**
     * @param compMne
     * @param daObj
     * @param arrId
     * @return
     */
    public EbFflDeceasedLogsRecord readEbFfLogRecord(String compMne, DataAccess daObj, String arrId) {
        EbFflDeceasedLogsRecord ebFflDeceasedLogsObj = null;
        try {
            ebFflDeceasedLogsObj = new EbFflDeceasedLogsRecord(daObj.getRecord(compMne, "EB.FFL.DECEASED.LOGS", "", arrId));
        } catch (Exception e) {
            verLogger.info(e.getMessage());
        }
        return ebFflDeceasedLogsObj;
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
     * @param currStatus
     * @param daObj
     * @param remarks
     * @param currStatus
     * @param ebFfLoanDetailsObj2
     * @return
     */
    private EbFflDeceasedInfoRecord updateRejectionLoanAccount(String notDeathDate,
            EbFflDeceasedInfoRecord ebFflDeceasedObj, String remarks, String currStatus) {

        try {
            int ffDodSize = 0;
            FfCurDodStsClass ffCurDodStsClassObj = new FfCurDodStsClass();
            ffCurDodStsClassObj.setFfCurDodSts(currStatus);
            ffCurDodStsClassObj.setFfCurDodDate(notDeathDate);
            ffCurDodStsClassObj.setFfDodStsAct(remarks);
            if (ebFflDeceasedObj != null) {
                ffDodSize = ebFflDeceasedObj.getFfCurDodSts().size();
                if (ffDodSize == 0) {
                    ebFflDeceasedObj.setFfCurDodSts(ffCurDodStsClassObj, 0);
                } else if (ffDodSize > 0) {
                    ebFflDeceasedObj.setFfCurDodSts(ffCurDodStsClassObj, ffDodSize);
                }
            } else {
                ebFflDeceasedObj.setFfCurDodSts(ffCurDodStsClassObj, 0);
            }
            verLogger.info("ebFflDeceasedObj " + ebFflDeceasedObj);
        } catch (Exception e) {
            verLogger.info(e.getMessage());
        }
        return ebFflDeceasedObj;

    }

}
