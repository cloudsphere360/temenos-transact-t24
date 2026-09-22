package com.temenos.fusion;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.temenos.api.TStructure;
import com.temenos.t24.api.complex.eb.servicehook.TransactionData;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.dates.DatesRecord;
import com.temenos.t24.api.records.ebffpettycashroupld.EbFfPettyCashRoUpldRecord;
import com.temenos.t24.api.records.ebffpettycashroupld.FfRoExpTypeClass;
import com.temenos.t24.api.tables.ebffpettycashroupld.EbFfPettyCashRoUpldTable;
import com.temenos.t24.api.records.fundstransfer.FundsTransferRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Date;
import com.temenos.t24.api.system.Session;

/**
 * @author jh116454
 * @Attached To: VERSION > FUNDS.TRANSFER,PETTY.AUTH
 * @Attached As: AUTH Routine > EB.API > FF.PETTY.CASH.FTID.UPD
 * @Description: Updating the FT ID in the table EB.FF.PETTY.CASH.RO.UPLD
 * 
 * 
 */

public class FfPettyCashFtidUpd extends RecordLifecycle {
    private static final FusionFileLogger yPettyCashFtidUpdLog = FusionFileLogger.getLogger(FfPettyCashFtidUpd.class);

    DataAccess yDataAcc = new DataAccess(this);

    @Override
    public void postUpdateRequest(String application, String currentRecordId, TStructure currentRecord,
            List<TransactionData> transactionData, List<TStructure> currentRecords,
            TransactionContext transactionContext) {

        yPettyCashFtidUpdLog.info("!---- Petty Cash FT ID Routine Starts ----!");
        Session session = new Session(this);
        String coCode = session.getCompanyId();

        String yFundTranID = "";
        String yFfPetycashExp = "";
        String yPettyCashID = "";
        String yTodDt = "";
        String yExpType = "";

        Date yDate = new Date(this);
        DatesRecord yDateRec = yDate.getDates();
        yTodDt = yDateRec.getToday().getValue();

        yFundTranID = currentRecordId;

        yPettyCashID = coCode + "-" + yTodDt;

        EbFfPettyCashRoUpldRecord yEbFfPettyCashDetRec = new EbFfPettyCashRoUpldRecord(
                yDataAcc.getRecord("EB.FF.PETTY.CASH.RO.UPLD", yPettyCashID));
        EbFfPettyCashRoUpldTable yEbFfPettyCashDetTable = new EbFfPettyCashRoUpldTable(this);

        try {

            FundsTransferRecord fundsTransferRecord = new FundsTransferRecord(currentRecord);
            yFfPetycashExp = fundsTransferRecord.getLocalRefField("FF.PCASH.EXP").getValue();

            List<FfRoExpTypeClass> yExpTypeList = yEbFfPettyCashDetRec.getFfRoExpType();
            Map<String, String> yAllTypeList = new HashMap<>();

            yAllTypeList.put("PrinSta", "Printing & Station Exp");
            yAllTypeList.put("CourPost", "Courier & Postage Exp");
            yAllTypeList.put("StafWel", "Staff Welfare Exp");
            yAllTypeList.put("RepMain", "Repair & Maintenance Exp");
            yAllTypeList.put("Conven", "Conveyance Exp");
            yAllTypeList.put("Office", "Office Exp");
            yAllTypeList.put("PettyOth", "Petty Other's Exp");

            String yExpDesc = yAllTypeList.get(yFfPetycashExp);
            yPettyCashFtidUpdLog.info("yExpDesc ->" + yExpDesc);
            for (int i = 0; i < yExpTypeList.size(); i++) {

                yExpType = yExpTypeList.get(i).getFfRoExpType().getValue();
                yPettyCashFtidUpdLog.info("yExpType ->" + yExpType);
                if (yExpType.equals(yExpDesc)) {
                    yEbFfPettyCashDetRec.getFfRoExpType().get(i).setFfRoExpTxnRef(yFundTranID);
                    yPettyCashFtidUpdLog.info("Inside write ->" + yEbFfPettyCashDetRec);
                    yEbFfPettyCashDetTable.write(yPettyCashID, yEbFfPettyCashDetRec);
                }
            }

        } catch (Exception e) {
            yPettyCashFtidUpdLog.info("Rec Missing");
        }

        yPettyCashFtidUpdLog.info("!---- Petty Cash FT ID Routine Ends ----!");
    }
}
