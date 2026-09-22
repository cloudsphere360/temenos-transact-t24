package com.temenos.fusion;

import java.util.List;

import com.temenos.api.TStructure;
import com.temenos.t24.api.complex.eb.servicehook.TransactionData;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.dates.DatesRecord;
import com.temenos.t24.api.records.ebffpettycashroupld.EbFfPettyCashRoUpldRecord;
import com.temenos.t24.api.tables.ebffpettycashroupld.EbFfPettyCashRoUpldTable;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Date;
import com.temenos.t24.api.system.Session;

/**
 * @author jh116454
 * @Attached To: VERSION > IM.DOCUMENT.UPLOAD,PETTY.CASH.IMG
 * @Attached As: Auth ROUTINE > EB.API > FF.PETTY.CASH.IMG.UPD
 * @Description: To Update the IMG ID in EB.FF.PETTY.CASH.RO.UPLD table
 * 
 */

public class FfPettyCashImgUpd extends RecordLifecycle {

    private static final FusionFileLogger yPettyImgIDLog = FusionFileLogger.getLogger(FfPettyCashImgUpd.class);

    DataAccess da = new DataAccess(this);

    @Override
    public void postUpdateRequest(String application, String currentRecordId, TStructure currentRecord,
            List<TransactionData> transactionData, List<TStructure> currentRecords,
            TransactionContext transactionContext) {

        String yImgDocID = currentRecordId;
        yPettyImgIDLog.info("yImgDocID ->" + yImgDocID);
        String yTodDt = "";
        String yPettyCashID = "";

        Session session = new Session(this);
        String coCode = session.getCompanyId();
        Date yDate = new Date(this);
        DatesRecord yDateRec = yDate.getDates();
        yTodDt = yDateRec.getToday().getValue();

        yPettyCashID = coCode + "-" + yTodDt;

        try {
            EbFfPettyCashRoUpldRecord yPettyCahDetRec = new EbFfPettyCashRoUpldRecord(
                    da.getRecord("EB.FF.PETTY.CASH.RO.UPLD", yPettyCashID));

            EbFfPettyCashRoUpldTable yPettyCahDetTable = new EbFfPettyCashRoUpldTable(this);

            yPettyCahDetRec.setSupportingDoc(yImgDocID);
            yPettyCahDetTable.write(yPettyCashID, yPettyCahDetRec);
        } catch (Exception e) {
            yPettyImgIDLog.info("Record Not Found");
        }

    }

}
