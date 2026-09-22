package com.temenos.fusion;

import java.util.ArrayList;
import java.util.List;

import com.temenos.api.LocalRefGroup;
import com.temenos.api.LocalRefList;
import com.temenos.api.TStructure;
import com.temenos.logging.facade.Logger;
import com.temenos.logging.facade.LoggerFactory;
import com.temenos.t24.api.complex.eb.servicehook.TransactionData;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.aaarrangement.AaArrangementRecord;
import com.temenos.t24.api.records.customer.CustomerRecord;
import com.temenos.t24.api.records.ebffldeceasedinfo.EbFflDeceasedInfoRecord;
import com.temenos.t24.api.records.ebffldeceasedinfo.FfGuarDodStsClass;
import com.temenos.t24.api.records.ebffldeceasedlogs.EbFflDeceasedLogsRecord;
import com.temenos.t24.api.records.ebffloandetails.EbFfLoanDetailsRecord;
import com.temenos.t24.api.records.ebffloandetails.FmEntityNumberClass;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;
import com.temenos.t24.api.tables.ebffldeceasedlogs.EbFflDeceasedLogsTable;

public class VerGuarantorDeathDateUpd extends RecordLifecycle {
    private static final String L3API = "L3API";
    private static final Logger verLogger = LoggerFactory.getLogger(L3API);
    VerDeathInfoUpdateRtn deathInfoObj = new VerDeathInfoUpdateRtn();
    VerDeathDateUpdate deathDateObj=new VerDeathDateUpdate();
    public static final String GUAR_DOD_STS = "FF.GUAR.DOD.STS";
    public static final String REJECTED_STS = "REJECTED";
    DataAccess daObj=null;
    Session ssObj=null;
    @Override
    public void postUpdateRequest(String application, String currentRecordId, TStructure currentRecord,
            List<TransactionData> transactionData, List<TStructure> currentRecords,
            TransactionContext transactionContext) {

         daObj = new DataAccess(this);
         ssObj = new Session(this);

        try {
            String remarks = "";
            CustomerRecord cusRec = new CustomerRecord(currentRecord);
            
            LocalRefList loanNum = cusRec.getLocalRefGroups("FF.LOAN.NUMBER");
            int loanSize = loanNum.size();
            if (loanSize > 0) {

                LocalRefList guarDodList = cusRec.getLocalRefGroups(GUAR_DOD_STS);
                int guarDodSize = cusRec.getLocalRefGroups(GUAR_DOD_STS).size();
                String notDeathDate = "";
                if (guarDodSize > 0) {
                    String status = guarDodList.get(guarDodSize - 1).getLocalRefField(GUAR_DOD_STS).getValue();
                    if (!status.equals(REJECTED_STS)) {
                        notDeathDate = guarDodList.get(guarDodSize - 1).getLocalRefField("FF.GUR.DOD.DATE").getValue();
                    }
                    remarks = guarDodList.get(guarDodSize - 1).getLocalRefField("FF.GUAR.STS.ACT").getValue();

                    verLogger.info("remarks " + remarks);

                    for (LocalRefGroup loanNumber : loanNum) {
                        String arrId = loanNumber.getLocalRefField("FF.LOAN.NUMBER").getValue();
                       
                        if (!arrId.isEmpty()) {
                            processArrangement(arrId,notDeathDate, remarks, status,transactionData,currentRecords);
                    
                            
                        }

                    }
                }

            }
        } catch (Exception e) {
            verLogger.info(e.getMessage());
        }

    }

    /**
     * @param arrId
     * @param notDeathDate
     * @param remarks
     * @param status
     * @param transactionData
     * @param currentRecords
     */
    private void processArrangement(String arrId, String notDeathDate, String remarks, String status,
            List<TransactionData> transactionData, List<TStructure> currentRecords) {
        String compMne = ssObj.getCompanyRecord().getFinancialMne().getValue();
        Boolean isGuarantor = false;
       try {
           
           AaArrangementRecord aaObj = new AaArrangementRecord(
                   daObj.getRecord(compMne, "AA.ARRANGEMENT", "", arrId));
           String coCode = aaObj.getCoCodeRec().getValue();
         
           if (aaObj.getArrStatus().getValue().equals("CURRENT")) {
               EbFfLoanDetailsRecord ebFfLoanDetailsObj = readEbFfRecord(compMne, daObj, arrId);
               EbFflDeceasedInfoRecord ebFflDeceasedObj = readEbFflDeceasedRecord(compMne, daObj,
                       arrId);
               if (ebFfLoanDetailsObj != null) {

                   isGuarantor = getGuarantorDets(ebFfLoanDetailsObj);

                   if (Boolean.TRUE.equals(isGuarantor)) {
                       if (status.equals("DECEASED") || status.equals("CONFIRMATION")) {
                           ebFflDeceasedObj = updateRejectionLoanAccount(notDeathDate,
                                   ebFflDeceasedObj, remarks, status);
                           updateOfs(transactionData, ebFflDeceasedObj, currentRecords, arrId, coCode);
                           updateFFlLog(notDeathDate, status, remarks, arrId, daObj, compMne);
                       } else {

                           ebFflDeceasedObj = updateFinalDeathDetails(notDeathDate, ebFflDeceasedObj,
                                   status, remarks);
                           updateOfs(transactionData, ebFflDeceasedObj, currentRecords, arrId, coCode);
                           updateSecondTimeFFlLog(notDeathDate, status, remarks, arrId, daObj, compMne);
                       }
                   }

               }
           }
           
       }catch (Exception e) {
        verLogger.info(e.getMessage());
    }
        
    }

    /**
     * @param ebFfLoanDetailsObj
     * @return
     */
    public Boolean getGuarantorDets(EbFfLoanDetailsRecord ebFfLoanDetailsObj) {
        Boolean isGuarantor = false;
        try {
            List<FmEntityNumberClass> entityNumberList = ebFfLoanDetailsObj.getFmEntityNumber();
            for (FmEntityNumberClass entityNumber : entityNumberList) {
                String isGuaran = entityNumber.getIsGuarantor().getValue();

                if (isGuaran.equals("YES")) {
                    isGuarantor = true;
                    break;
                }
                
            }
        } catch (Exception e) {
            verLogger.info(e.getMessage());
        }
        return isGuarantor;
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

            TransactionData syncTransactionData = new TransactionData();
            syncTransactionData.setVersionId("EB.FFL.DECEASED.INFO,FF.INSURANCE");
            syncTransactionData.setTransactionId(arrId);
            syncTransactionData.setSourceId("OFS.INSURANCE.UPD");
            syncTransactionData.setCompanyId(coCode);
            syncTransactionData.setFunction("INPUT");
            syncTransactionData.setNumberOfAuthoriser("0");
            verLogger.info("sync " + syncTransactionData.toString());
            transactionData.add(syncTransactionData);
            currentRecords.add(ebFflDeceasedObj.toStructure());
            verLogger.info("record1 " + currentRecords.toString() + ebFflDeceasedObj);
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
    public EbFfLoanDetailsRecord readEbFfRecord(String compMne, DataAccess daObj, String arrId) {
        EbFfLoanDetailsRecord ebFfLoanDetailsObj = null;
        try {
            ebFfLoanDetailsObj = new EbFfLoanDetailsRecord(daObj.getRecord(compMne, "EB.FF.LOAN.DETAILS", "", arrId));
        } catch (Exception e) {
            verLogger.info(e.getMessage());
        }
        return ebFfLoanDetailsObj;
    }

    /**
     * @param compMne
     * @param daObj
     * @param arrId
     * @return
     */
    public EbFflDeceasedInfoRecord readEbFflDeceasedRecord(String compMne, DataAccess daObj, String arrId) {
        EbFflDeceasedInfoRecord ebFflDeceasedObj = null;
        try {
            ebFflDeceasedObj = new EbFflDeceasedInfoRecord(daObj.getRecord(compMne, "EB.FFL.DECEASED.INFO", "", arrId));
        } catch (Exception e) {
            verLogger.info(e.getMessage());
        }
        return ebFflDeceasedObj;
    }
    /**
     * @param notDeathDate
     * @param currStatus
     * @param remarks
     * @param arrId
     * @param daObj
     * @param compMne 
     */
    private void updateFFlLog(String notDeathDate, String status, String remarks, String arrId, DataAccess daObj, String compMne) {
      try {
          EbFflDeceasedLogsTable fflLogTableObj=new EbFflDeceasedLogsTable(this);
          EbFflDeceasedLogsRecord fflLogRecObj=deathDateObj.readEbFfLogRecord(compMne, daObj, arrId);
          int ffDodSize = 0;
          com.temenos.t24.api.records.ebffldeceasedlogs.FfGuarDodStsClass ffGurDodStsClassObj = new com.temenos.t24.api.records.ebffldeceasedlogs.FfGuarDodStsClass();
          ffGurDodStsClassObj.setFfGuarDodSts(status);
          ffGurDodStsClassObj.setFfGurDodDate(notDeathDate);
          ffGurDodStsClassObj.setFfGuarStsAct(remarks);
     if(fflLogRecObj!=null) {      
             ffDodSize = fflLogRecObj.getFfGuarDodSts().size();
             fflLogRecObj.setFfGuarDodSts(ffGurDodStsClassObj, ffDodSize);
             fflLogTableObj.write(arrId, fflLogRecObj);
     }else {
          fflLogRecObj=new EbFflDeceasedLogsRecord(this);
          fflLogRecObj.setFfGuarDodSts(ffGurDodStsClassObj, 0);
          fflLogTableObj.write(arrId, fflLogRecObj);
     }
          
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
     * @param currStatus
     * @param daObj
     * @param remarks
     * @param status
     * @param ebFfLoanDetailsObj2
     * @return
     */
    private EbFflDeceasedInfoRecord updateRejectionLoanAccount(String notDeathDate,
            EbFflDeceasedInfoRecord ebFflDeceasedObj, String remarks, String status) {

        try {
            int ffDodSize = 0;

            if (ebFflDeceasedObj == null) {
                ebFflDeceasedObj = new EbFflDeceasedInfoRecord(this);
            }
            FfGuarDodStsClass ffGurDodStsClassObj = new FfGuarDodStsClass();
            ffGurDodStsClassObj.setFfGuarDodSts(status);
            ffGurDodStsClassObj.setFfGurDodDate(notDeathDate);
            ffGurDodStsClassObj.setFfGuarStsAct(remarks);
            verLogger.info("ffGurDodStsClassObj " + ffGurDodStsClassObj);

            ffDodSize = ebFflDeceasedObj.getFfGuarDodSts().size();
            if (ffDodSize == 0) {
                ebFflDeceasedObj.setFfGuarDodSts(ffGurDodStsClassObj, 0);
            } else {
                ebFflDeceasedObj.setFfGuarDodSts(ffGurDodStsClassObj, ffDodSize);
            }

            verLogger.info("ebFflDeceasedObj " + ebFflDeceasedObj);

        } catch (Exception e) {
            verLogger.info(e.getMessage());
        }
        return ebFflDeceasedObj;

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
    public EbFflDeceasedInfoRecord updateFinalDeathDetails(String notDeathDate,
            EbFflDeceasedInfoRecord ebFflDeceasedObj, String currStatus, String remarks) {
        try {
            verLogger.info("inside confirm");
            if (!currStatus.equals(REJECTED_STS)) {
                ebFflDeceasedObj.getFfVerGurConf().setValue(notDeathDate);
            }
            List<FfGuarDodStsClass> guarDodStsList = ebFflDeceasedObj.getFfGuarDodSts();
            verLogger.info("currDodStsList " + guarDodStsList);
            FfGuarDodStsClass gurDodStsObj = new FfGuarDodStsClass();

            gurDodStsObj.setFfGuarDodSts(currStatus);

            gurDodStsObj.setFfGurDodDate(notDeathDate);
            gurDodStsObj.setFfGuarStsAct(remarks);
            ebFflDeceasedObj.setFfGuarDodSts(gurDodStsObj, guarDodStsList.size());
            verLogger.info("ebFflDeceasedObj " + ebFflDeceasedObj);
        } catch (Exception e) {
            verLogger.info(e.getMessage());
        }
        return ebFflDeceasedObj;
    }
    
    /**
     * @param notDeathDate
     * @param currStatus
     * @param remarks
     * @param arrId
     * @param daObj
     * @param compMne 
     */
    private void updateSecondTimeFFlLog(String notDeathDate, String currStatus, String remarks, String arrId, DataAccess daObj, String compMne) {
      try {
          EbFflDeceasedLogsTable fflLogTableObj=new EbFflDeceasedLogsTable(this);
          EbFflDeceasedLogsRecord fflLogRecObj=deathDateObj.readEbFfLogRecord(compMne, daObj, arrId);
          if(!currStatus.equals(REJECTED_STS)) {
              fflLogRecObj.getFfVerGurConf().setValue(notDeathDate);
  }
          int ffDodSize = 0;
          com.temenos.t24.api.records.ebffldeceasedlogs.FfGuarDodStsClass gurDodStsObj = new com.temenos.t24.api.records.ebffldeceasedlogs.FfGuarDodStsClass();
          gurDodStsObj.setFfGuarDodSts(currStatus);

          gurDodStsObj.setFfGurDodDate(notDeathDate);
          gurDodStsObj.setFfGuarStsAct(remarks);
      
             ffDodSize = fflLogRecObj.getFfGuarDodSts().size();
             fflLogRecObj.setFfGuarDodSts(gurDodStsObj, ffDodSize);  
             fflLogTableObj.write(arrId, fflLogRecObj);
          
      }catch (Exception e) {
        verLogger.info(e.getMessage());
    }
    }

}
