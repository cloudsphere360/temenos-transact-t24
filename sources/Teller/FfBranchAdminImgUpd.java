package com.temenos.fusion;

import java.util.List;

import com.temenos.api.TStructure;
import com.temenos.t24.api.complex.eb.servicehook.TransactionData;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.dates.DatesRecord;
import com.temenos.t24.api.records.ebffftbranchadminexpensesupd.EbFfFtBranchAdminExpensesUpdRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Date;
import com.temenos.t24.api.system.Session;
import com.temenos.t24.api.tables.ebffftbranchadminexpensesupd.EbFfFtBranchAdminExpensesUpdTable;

/**
 * @Attached To: VERSION > IM.DOCUMENT.UPLOAD,BRANCH.ADMIN.IMG
 * @Attached As: Auth ROUTINE > EB.API > FF.BRANCH.ADMIN.IMG.UPD
 * @Description: To Update the IMG ID in EB.FF.FT.BRANCH.ADMIN.EXPENSES.UPD table
 * 
 */

public class FfBranchAdminImgUpd extends RecordLifecycle {

    private static final FusionFileLogger FfBranchAdminImgUpdRtn = FusionFileLogger.getLogger(FfBranchAdminImgUpd.class);

    DataAccess da = new DataAccess(this);

    @Override
    public void postUpdateRequest(String application, String currentRecordId, TStructure currentRecord,
            List<TransactionData> transactionData, List<TStructure> currentRecords,
            TransactionContext transactionContext) {

        String yImgDocID = currentRecordId;
        FfBranchAdminImgUpdRtn.info("yImgDocID ->" + yImgDocID);
        String yTodDt = "";
        String yBranchAdminID = "";

        Session session = new Session(this);
        String coCode = session.getCompanyId();
        Date yDate = new Date(this);
        DatesRecord yDateRec = yDate.getDates();
        yTodDt = yDateRec.getToday().getValue();

        yBranchAdminID = coCode + "-" + yTodDt;

        try {
            EbFfFtBranchAdminExpensesUpdRecord yBranchAdminrec = new EbFfFtBranchAdminExpensesUpdRecord(
                    da.getRecord("EB.FF.FT.BRANCH.ADMIN.EXPENSES.UPD", yBranchAdminID));

            EbFfFtBranchAdminExpensesUpdTable yBranchAdmintable = new EbFfFtBranchAdminExpensesUpdTable(this);

            yBranchAdminrec.setSupportingDoc(yImgDocID);

            yBranchAdmintable.write(yBranchAdminID, yBranchAdminrec);
        } catch (Exception e) {
            FfBranchAdminImgUpdRtn.info("Record Not Found");
        }

    }

}