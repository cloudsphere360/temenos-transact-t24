package com.temenos.fusion;

import java.math.BigDecimal;
import java.util.List;

import com.temenos.api.TStructure;
import com.temenos.api.TValidationResponse;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.dates.DatesRecord;
import com.temenos.t24.api.records.ebffeodscreen.EbFfEodScreenRecord;
import com.temenos.t24.api.records.ebffsnatchfraudamtupd.EbFfSnatchFraudAmtUpdRecord;
import com.temenos.t24.api.records.ebffsnatchfraudamtupd.DateOfTxnClass;
import com.temenos.t24.api.records.fundstransfer.FundsTransferRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Date;
import com.temenos.t24.api.system.Session;

/**
 *
 * @author jh116454
 * @Attached To: VERSION > FUNDS.TRANSFER,FF.FT.SUSP.AMT VERSION >
 *           FUNDS.TRANSFER,FF.FT.REV.SUSP.AMT
 * @Attached As: INPUT ROUTINE > EB.API > PETTYCASH.TOTAL.OVERRIDE
 * @Description: To throw override if amount is already uploaded for a day
 * 
 * 
 *               01-07-26 - Changed the Logic to : IF the Total Pending
 *               Collection is equal to Sum of all the amount in concat table
 *               EB.FF.SNATCH.FRAUD.AMT.UPD under suspense amount for the day then throw an
 *               error.
 * 
 */

public class PettyCashTotalOverride extends RecordLifecycle {

    private static final FusionFileLogger yPettyCashTotalOverrideLog = FusionFileLogger
            .getLogger(PettyCashTotalOverride.class);

    BigDecimal ySupAmtD = BigDecimal.ZERO;
    BigDecimal yTotSupAmt = BigDecimal.ZERO;
    BigDecimal yFinalSupAmt = BigDecimal.ZERO;
    
    DataAccess yDataAcc = new DataAccess(this);
    
    String yRecordId = "";
    
    @Override
    public TValidationResponse validateRecord(String application, String currentRecordId, TStructure currentRecord,
            TStructure unauthorisEbPettyCashLimitRecordedRecord, TStructure liveRecord,
            TransactionContext transactionContext) {

        FundsTransferRecord yFtRec = new FundsTransferRecord(currentRecord);
        Session sess = new Session(this);
       
        Date date = new Date(this);
        DatesRecord dateRec = date.getDates();

        String ySuspAmt = "";
        String ySuspAmtRev = "";
       
        String yCoCode = sess.getCompanyId();
        String yStrTodayVal = dateRec.getToday().getValue();
        yRecordId = yCoCode + "-" + yStrTodayVal;

        String yVersionID = transactionContext.getCurrentVersionId();

        try {
            EbFfEodScreenRecord yEbFfEODRec = new EbFfEodScreenRecord(
                    yDataAcc.getRecord("EB.FF.EOD.SCREEN", yRecordId));
            ySuspAmt = yEbFfEODRec.getSuspenseAmount().getValue();
            yPettyCashTotalOverrideLog.info("ySuspAmt -> " + ySuspAmt);
            ySuspAmtRev = yEbFfEODRec.getSuspenseReversalAmount().getValue();
            yPettyCashTotalOverrideLog.info("ySuspAmtRev -> " + ySuspAmtRev);
        } catch (Exception ex) {
            yPettyCashTotalOverrideLog.info("Eb Eof Rec Missing");
        }

        if ((yVersionID.equals(",FF.FT.SUSP.AMT")) && (!ySuspAmt.equals(""))) {
           
            ySupAmtD = new BigDecimal((ySuspAmt.isEmpty()) ? "0" : ySuspAmt);
           String ySupAmt = String.valueOf(ySupAmtD);
          
            getSnatchFraudDet();
            
            yPettyCashTotalOverrideLog.info("ySupAmtD ->" + ySupAmtD);
            if ((ySupAmt.equals("0")) || ySupAmt.equals("0.00")){
                yFtRec.getDebitAmount().setError("EB-SNAT.FRD.PEND.ERR");
            }
            
        }

        if ((yVersionID.equals(",FF.FT.REV.SUSP.AMT")) && (!ySuspAmtRev.equals(""))) {
            double yRevAmtD = Double.parseDouble(ySuspAmtRev);
            yPettyCashTotalOverrideLog.info("yRevAmtD ->" + yRevAmtD);
            if (yRevAmtD > 0) {
                yFtRec.getDebitAmount().setError(" - Reverse Amount Already Uploaded in EB.FF.EOD.SCREEN");
            }
        }
        
        currentRecord.set(yFtRec.toStructure());
        return yFtRec.getValidationResponse();
    }
    private void getSnatchFraudDet() {
        try {
            EbFfSnatchFraudAmtUpdRecord yEbSnaFraRec = new EbFfSnatchFraudAmtUpdRecord(
                    yDataAcc.getRecord("EB.FF.SNATCH.FRAUD.AMT.UPD", yRecordId));
            List<DateOfTxnClass> yDateList = yEbSnaFraRec.getDateOfTxn();

            for (int j = 0; j < yDateList.size(); j++) {
                String yIncType = yDateList.get(j).getIncidentTyp().getValue();
                yPettyCashTotalOverrideLog.info(" yIncType ->" + yIncType);

                if (yIncType.equals("SUSPENSE")) {
                    String ySuspAmt = yDateList.get(j).getFfSnaFrdAmt().getValue();
                    yPettyCashTotalOverrideLog.info(" ySuspAmt ->" + ySuspAmt);
                    ySupAmtD = new BigDecimal((ySuspAmt == null || ySuspAmt.isEmpty()) ? "0" : ySuspAmt);
                    yTotSupAmt = yTotSupAmt.add(ySupAmtD);
                    yPettyCashTotalOverrideLog.info(" yTotSupAmt in Loc Table -> " + yTotSupAmt);
                }
            }
        } catch (Exception ex) {
            yTotSupAmt = BigDecimal.ZERO;
            yPettyCashTotalOverrideLog.info(" yTotSupAmt 0 -> " + yTotSupAmt);
            yPettyCashTotalOverrideLog.info("EB.FF.SNATCH.FRAUD.AMT.UPD Rec Not Found -> " + ex);
        }
    }

}