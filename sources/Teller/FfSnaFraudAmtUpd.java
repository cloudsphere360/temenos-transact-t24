package com.temenos.fusion;

import java.util.List;

import com.temenos.api.TStructure;
import com.temenos.t24.api.complex.eb.servicehook.TransactionData;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.dates.DatesRecord;
import com.temenos.t24.api.records.ebffsnatchfraudamtupd.DateOfTxnClass;
import com.temenos.t24.api.records.ebffsnatchfraudamtupd.EbFfSnatchFraudAmtUpdRecord;
import com.temenos.t24.api.records.fundstransfer.FundsTransferRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Date;
import com.temenos.t24.api.system.Session;
import com.temenos.t24.api.tables.ebffsnatchfraudamtupd.EbFfSnatchFraudAmtUpdTable;

/**
 * @author jh116454
 * @Attached To: VERSION > FUNDS.TRANSFER,FF.SNATCHING.FRAUD.COLL
 * @Attached As: Auth ROUTINE > EB.API > FF.SNA.FRAUD.AMT.UPD
 * @Description: To Update the Amount in the table EB.FF.SNATCH.FRAUD.AMT.UPD
 * 
 */

public class FfSnaFraudAmtUpd extends RecordLifecycle {
    private static final FusionFileLogger ySnaFraudAmtUpdLog = FusionFileLogger.getLogger(FfSnaFraudAmtUpd.class);
    DataAccess yDataAcc = new DataAccess(this);

    @Override
    public void postUpdateRequest(String application, String currentRecordId, TStructure currentRecord,
            List<TransactionData> transactionData, List<TStructure> currentRecords,
            TransactionContext transactionContext) {

        ySnaFraudAmtUpdLog.info("!---  Routine Starts ---!");

        Date yDate = new Date(this);
        DatesRecord yDateRec = yDate.getDates();
        String yVersionID = transactionContext.getCurrentVersionId();
        ySnaFraudAmtUpdLog.info("Version Name -> " + yVersionID);

        FundsTransferRecord ySnaFraFtRec = new FundsTransferRecord(currentRecord);

        String yDateofTxn = "";
        String yIncidentTyp = "";
        String yDebAmt = "";

        String yTodayDt = yDateRec.getToday().getValue();
        Session ySession = new Session(this);
        String yCoCode = ySession.getCompanyId();

        String yRecordId = yCoCode + "-" + yTodayDt;
        ySnaFraudAmtUpdLog.info(" Record ID -> " + yRecordId);

        if (yVersionID.equals(",FF.SNATCHING.FRAUD.COLL")) {
            yDateofTxn = ySnaFraFtRec.getDebitValueDate().getValue();
            ySnaFraudAmtUpdLog.info("Snatch/fraud Date of Txn -> " + yDateofTxn);
            yIncidentTyp = ySnaFraFtRec.getLocalRefField("INCIDENT.TYPE").getValue();
            yDebAmt = ySnaFraFtRec.getDebitAmount().getValue();

        }

        if (yVersionID.equals(",FF.FT.SUSP.AMT")) {
            yDateofTxn = ySnaFraFtRec.getDebitValueDate().getValue();
            yIncidentTyp = ySnaFraFtRec.getLocalRefField("FF.SUSP.AMT").getValue();
            yDebAmt = ySnaFraFtRec.getDebitAmount().getValue();
            ySnaFraudAmtUpdLog.info("Suspence Debit Amt -> " + yDebAmt);
        }

        if (yVersionID.equals(",FF.FT.REV.SUSP.AMT")) {
            yDateofTxn = ySnaFraFtRec.getDebitValueDate().getValue();
            ySnaFraudAmtUpdLog.info("Reverse Date of Txn -> " + yDateofTxn);
            yIncidentTyp = ySnaFraFtRec.getLocalRefField("FF.SUSP.AMT").getValue();
            yDebAmt = ySnaFraFtRec.getDebitAmount().getValue();
        }

        EbFfSnatchFraudAmtUpdRecord yEbSnaFraRec = new EbFfSnatchFraudAmtUpdRecord(this);
        EbFfSnatchFraudAmtUpdTable yEbSnaFraTab = new EbFfSnatchFraudAmtUpdTable(this);

        try {
            yEbSnaFraRec = new EbFfSnatchFraudAmtUpdRecord(yDataAcc.getRecord("EB.FF.SNATCH.FRAUD.AMT.UPD", yRecordId));
            DateOfTxnClass yDateofTxnOne = new DateOfTxnClass();

            yDateofTxnOne.setDateOfTxn(yDateofTxn);
            yDateofTxnOne.setIncidentTyp(yIncidentTyp);
            yDateofTxnOne.setFfSnaFrdAmt(yDebAmt);

            yEbSnaFraRec.insertDateOfTxn(yDateofTxnOne, 0);
            ySnaFraudAmtUpdLog.info("Final list -> " + yEbSnaFraRec);
        } catch (Exception e) {
            DateOfTxnClass yDateofTxnNew = new DateOfTxnClass();

            yDateofTxnNew.setDateOfTxn(yDateofTxn);
            yDateofTxnNew.setIncidentTyp(yIncidentTyp);
            yDateofTxnNew.setFfSnaFrdAmt(yDebAmt);

            yEbSnaFraRec.insertDateOfTxn(yDateofTxnNew, 0);
            ySnaFraudAmtUpdLog.info("Final list when its null -> " + yEbSnaFraRec);

        }

        try {
            yEbSnaFraTab.write(yRecordId, yEbSnaFraRec);
        } catch (Exception e) {
            ySnaFraudAmtUpdLog.info("Could not write the file");
        }
        ySnaFraudAmtUpdLog.info("!---  Ends Starts ---!");
    }
}
