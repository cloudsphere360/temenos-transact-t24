package com.temenos.fusion;

import com.temenos.api.TStructure;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.dates.DatesRecord;
import com.temenos.t24.api.records.ebffcollpostingscreen.EbFfCollPostingScreenRecord;
import com.temenos.t24.api.records.ebffeodscreen.EbFfEodScreenRecord;
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
 */
public class FtSuspDebAmtUpd extends RecordLifecycle {

    private static final FusionFileLogger ySuspDebAmtUpdLog = FusionFileLogger.getLogger(FtSuspDebAmtUpd.class);
    DataAccess yDataAcc = new DataAccess(this);
    EbFfCollPostingScreenRecord yEbCollPostRec = null;

    @Override
    public void defaultFieldValues(String application, String currentRecordId, TStructure currentRecord,
            TStructure unauthorisedRecord, TStructure liveRecord, TransactionContext transactionContext) {

        ySuspDebAmtUpdLog.info("!---- Debit Amount Default Routine Starts ----!");

        Date date = new Date(this);
        DatesRecord dateRec = date.getDates();

        FundsTransferRecord yFtRec = new FundsTransferRecord(currentRecord);

        String strTodayVal = dateRec.getToday().getValue();

        Session sess = new Session(this);
        String yDiffAmt = "";

        String ycoCode = sess.getCompanyId();
        String yRecordId = ycoCode + "-" + strTodayVal;

        String yVersionID = transactionContext.getCurrentVersionId();
        ySuspDebAmtUpdLog.info("Version Name -> " + yVersionID);

        if (yVersionID.equals(",FF.FT.SUSP.AMT")) {
            try {
                yEbCollPostRec = new EbFfCollPostingScreenRecord(
                        yDataAcc.getRecord("EB.FF.COLL.POSTING.SCREEN", yRecordId));

                yDiffAmt = yEbCollPostRec.getTotalPendingCash().getValue();
                yFtRec.setDebitAmount(yDiffAmt);
            } catch (Exception e) {
                ySuspDebAmtUpdLog.info("No Rec found in EB.FF.COLL.POSTING.SCREEN with that ID");
            }
        }

        if (yVersionID.equals(",FF.FT.REV.SUSP.AMT")) {
            try {
                int yIntTodayDt = Integer.parseInt(strTodayVal) - 1;
                String yIntTod = String.valueOf(yIntTodayDt);
                String yPrevDt = ycoCode + "-" + yIntTod;

                EbFfEodScreenRecord yEbFfEODRec = new EbFfEodScreenRecord(
                        yDataAcc.getRecord("EB.FF.EOD.SCREEN", yPrevDt));
                String yPrevSuspAmt = yEbFfEODRec.getSuspenseAmount().getValue();
                yFtRec.setDebitAmount(yPrevSuspAmt);
            } catch (Exception e) {
                ySuspDebAmtUpdLog.info("No Rec in EB.FF.EOD.SCREEN,INPUT");
            }
        }
        currentRecord.set(yFtRec.toStructure());
    }
}
