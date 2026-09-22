package com.temenos.fusion;

import com.temenos.t24.api.records.aaactivityhistory.AaActivityHistoryRecord;
import com.temenos.t24.api.records.aaactivityhistory.ActivityRefClass;
import com.temenos.t24.api.records.aaactivityhistory.EffectiveDateClass;
import com.temenos.t24.api.records.ebffgenericparameter.EbFfGenericParameterRecord;
import com.temenos.t24.api.records.ebffgenericparameter.KeyNameClass;
import com.temenos.t24.api.records.fundstransfer.FundsTransferRecord;
import com.temenos.t24.api.system.DataAccess;

public class VerAaPosWrtOffUpdate {
    private static final FusionFileLogger verLogger = FusionFileLogger.getLogger(VerAaPosWrtOffUpdate.class);
    
          public String fetchStatus( DataAccess da, String activity, String arrangement, String compMne) {
            AaActivityHistoryRecord aaActivityHistoryRecord = new AaActivityHistoryRecord(
                    da.getRecord("AA.ACTIVITY.HISTORY", arrangement));

            verLogger.info("activity "+activity);

            if(activity.equals("LENDING-WRITE.OFF-BAL.MAINTAIN")) {

                FundsTransferRecord  ftObj = getFtRecordDets(compMne,da, aaActivityHistoryRecord,"LENDING-SETTLE-FORECLOSURE");

                if (ftObj != null) {

                    String foreClsRs =ftObj.getLocalRefField("FF.FORECLS.RS").getValue();
                    verLogger.info("foreClsRs "+foreClsRs);

                    if ("POST WRITE OFF CLOSED".equals(foreClsRs)|| ("POSTWRITEOFFCLOSED".equals(foreClsRs))) {

                        activity = "WRITE.CLOSE";
                    }else if(Boolean.TRUE.equals(checkSettleForeClosureActvty(aaActivityHistoryRecord,"LENDING-CHARGEOFF-ARRANGEMENT"))&&foreClsRs.equals("")) {
                        verLogger.info("BLANK Check true");
                        activity = "BLANK";
                    }
                }else if(Boolean.TRUE.equals(checkSettleForeClosureActvty(aaActivityHistoryRecord,"LENDING-CHARGEOFF-ARRANGEMENT"))){
                    activity = "WRITE.WAIVE";
                    
                }
            }

           
if(activity.equals("WRITE.CLOSE")||activity.equals("WRITE.WAIVE")) {
           return activity;
}else if(activity.equals("BLANK")){
    return "BLANK";
}
            return activity;
        }

      
        /**
         * @param string
         * @param aaActivityHistoryRecord
         */
        public Boolean checkSettleForeClosureActvty( AaActivityHistoryRecord aaActivityHistoryRecord,String activity) {
           
            try {
                verLogger.info("Inside methid of chkforclosure");
                for (EffectiveDateClass effectiveDateClass : aaActivityHistoryRecord.getEffectiveDate()) {
                    for (ActivityRefClass activityRefClass : effectiveDateClass.getActivityRef()) {
                        verLogger.info("activityRefClass.getActivity().getValue() "+activityRefClass.getActivity().getValue());
                        if (activityRefClass.getActivity().getValue().equals(activity)) {
                                                  verLogger.info("true");
                            return true;
                        }

                    }
                }
            }catch (Exception e) {
               verLogger.info(e.getMessage());
            }
            return false;
            
        }
        
        
        
        private FundsTransferRecord getFtRecordDets(String compMne, DataAccess da, AaActivityHistoryRecord aaActivityHistoryRecord, String activity) {
            FundsTransferRecord  ftObj=null;
            try {
verLogger.info("inside get ft record");
                for (EffectiveDateClass effectiveDateClass : aaActivityHistoryRecord.getEffectiveDate()) {
                    for (ActivityRefClass activityRefClass : effectiveDateClass.getActivityRef()) {

                        if (activityRefClass.getActivity().getValue().equals(activity)) {
                            verLogger.info("activity check true");
                            String contractId=activityRefClass.getContractId().getValue();
                            verLogger.info("contractId "+contractId);
                                String ftId = contractId.substring(0, contractId.indexOf('\\'));
                                verLogger.info("ftId "+ftId+" compMne "+compMne);
                                 ftObj = readFtRecord(compMne,da, ftId);
                           break;
                        }

                    }
                }
            }catch (Exception e) {
verLogger.info(e.getMessage());
            }
            return ftObj;
        }

        public FundsTransferRecord readFtRecord(String compMne, DataAccess da, String ftId) {
            FundsTransferRecord ftObj = null;
            try {
                ftObj = new FundsTransferRecord(
                        da.getRecord(compMne, "FUNDS.TRANSFER", "", ftId));

            } catch (Exception e) {
                verLogger.info(e.getMessage());
            }
            return ftObj;
        }



    }