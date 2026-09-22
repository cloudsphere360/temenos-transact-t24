package com.temenos.fusion;

import java.math.BigDecimal;
import java.util.List;

import com.temenos.api.TStructure;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.dates.DatesRecord;
import com.temenos.t24.api.records.ebffcollpostingscreen.EbFfCollPostingScreenRecord;
import com.temenos.t24.api.records.ebffeodscreen.EbFfEodScreenRecord;
import com.temenos.t24.api.records.ebffsnatchfraudamtupd.DateOfTxnClass;
import com.temenos.t24.api.records.ebffsnatchfraudamtupd.EbFfSnatchFraudAmtUpdRecord;
import com.temenos.t24.api.records.fundstransfer.FundsTransferRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Date;
import com.temenos.t24.api.system.Session;

/**
 * @author jh116454
 * @Attached To: VERSION > FUNDS.TRANSFER,FF.FT.SUSP.AMT
 * @Attached As: VALIDATION ROUTINE > EB.API > FT.SUSP.DEB.AMT.UPD
 * @Description: To fetch the value of Total Pending cash (Difference) from
 *               table EbFfCollPostingScreen and update in update in
 *               DEBIT.AMOUNT field
 * 
 *               01-07-26 - Changed the Logic to: fetch the value of Total
 *               Pending cash (Difference) from table EbFfCollPostingScreen.if
 *               the raising the FT 2nd time then Minus the Pending Collection
 *               Amount from the concat table EB.FF.SNATCH.FRAUD.AMT.UPD amount
 *               and put FT.
 * 
 *               IF the Total Pending Collection is equal to Sum of all the
 *               amount in concat table EB.FF.SNATCH.FRAUD.AMT.UPD under
 *               suspense amount then throw an error.
 */

public class FtSuspDebAmtUpd extends RecordLifecycle {

    private static final FusionFileLogger ySuspDebAmtUpdLog = FusionFileLogger.getLogger(FtSuspDebAmtUpd.class);
    DataAccess yDataAcc = new DataAccess(this);
    EbFfCollPostingScreenRecord yEbCollPostRec = null;

    FundsTransferRecord yFtRec = null;

    String yRecordId = "";
    BigDecimal yTotPendAmtD = BigDecimal.ZERO;
    BigDecimal ySupAmtD = BigDecimal.ZERO;
    BigDecimal yTotSupAmt = BigDecimal.ZERO;
    BigDecimal yFinalSupAmt = BigDecimal.ZERO;

    @Override
    public void defaultFieldValues(String application, String currentRecordId, TStructure currentRecord,
            TStructure unauthorisedRecord, TStructure liveRecord, TransactionContext transactionContext) {

        ySuspDebAmtUpdLog.info("!---- Debit Amount Default Routine Starts ----!");

        Date date = new Date(this);
        DatesRecord dateRec = date.getDates();

        yFtRec = new FundsTransferRecord(currentRecord);

        String strTodayVal = dateRec.getToday().getValue();

        Session sess = new Session(this);

        String ycoCode = sess.getCompanyId();
        yRecordId = ycoCode + "-" + strTodayVal;

        String yVersionID = transactionContext.getCurrentVersionId();
        ySuspDebAmtUpdLog.info("Version Name -> " + yVersionID);

        if (yVersionID.equals(",FF.FT.SUSP.AMT")) {
            getCollScrnDet();
        }

        if (yVersionID.equals(",FF.FT.REV.SUSP.AMT")) {
            try {
                int yIntTodayDt = Integer.parseInt(strTodayVal) - 1;
                String yIntTod = String.valueOf(yIntTodayDt);
                String yPrevDt = ycoCode + "-" + yIntTod;

                EbFfEodScreenRecord yEbFfEODRec = new EbFfEodScreenRecord(
                        yDataAcc.getRecord("EB.FF.EOD.SCREEN", yPrevDt));
                String yPrevSuspAmt = yEbFfEODRec.getSuspenseAmount().getValue();
                double yPrevAmtD = Math.abs(Double.parseDouble(yPrevSuspAmt));
                yPrevSuspAmt = String.valueOf(yPrevAmtD);
                yFtRec.setDebitAmount(yPrevSuspAmt);
            } catch (Exception e) {
                ySuspDebAmtUpdLog.info("No Rec in EB.FF.EOD.SCREEN,INPUT");
            }
        }

        currentRecord.set(yFtRec.toStructure());
    }

    private void getCollScrnDet() {
        try {
            yEbCollPostRec = new EbFfCollPostingScreenRecord(
                    yDataAcc.getRecord("EB.FF.COLL.POSTING.SCREEN", yRecordId));

            String yTotPendAmt = yEbCollPostRec.getTotalPendingCash().getValue();
            yTotPendAmtD = new BigDecimal((yTotPendAmt == null || yTotPendAmt.isEmpty()) ? "0" : yTotPendAmt);
            ySuspDebAmtUpdLog.info(" Total Pending Amt -> " + yTotPendAmtD);

            getSnatchFraudDet();

            yFinalSupAmt = yTotSupAmt.subtract(yTotPendAmtD);
            ySuspDebAmtUpdLog.info(" yFinalSupAmt -> " + yFinalSupAmt);

            yTotPendAmt = String.valueOf(yFinalSupAmt);
            yFtRec.setDebitAmount(yTotPendAmt);

        } catch (Exception e) {
            ySuspDebAmtUpdLog.info("No Rec found in EB.FF.COLL.POSTING.SCREEN with that ID");
        }
    }

    private void getSnatchFraudDet() {
        try {
            EbFfSnatchFraudAmtUpdRecord yEbSnaFraRec = new EbFfSnatchFraudAmtUpdRecord(
                    yDataAcc.getRecord("EB.FF.SNATCH.FRAUD.AMT.UPD", yRecordId));
            List<DateOfTxnClass> yDateList = yEbSnaFraRec.getDateOfTxn();

            for (int j = 0; j < yDateList.size(); j++) {
                String yIncType = yDateList.get(j).getIncidentTyp().getValue();
                ySuspDebAmtUpdLog.info(" yIncType ->" + yIncType);

                if (yIncType.equals("Suspense")) {
                    String ySuspAmt = yDateList.get(j).getFfSnaFrdAmt().getValue();
                    ySuspDebAmtUpdLog.info(" ySuspAmt ->" + ySuspAmt);
                    ySupAmtD = new BigDecimal((ySuspAmt == null || ySuspAmt.isEmpty()) ? "0" : ySuspAmt);
                    yTotSupAmt = yTotSupAmt.add(ySupAmtD);
                    ySuspDebAmtUpdLog.info(" yTotSupAmt in Loc Table -> " + yTotSupAmt);
                }
            }
        } catch (Exception ex) {
            yTotSupAmt = BigDecimal.ZERO;
            ySuspDebAmtUpdLog.info(" yTotSupAmt 0 -> " + yTotSupAmt);
            ySuspDebAmtUpdLog.info("EB.FF.SNATCH.FRAUD.AMT.UPD Rec Not Found -> " + ex);
        }
    }
}
