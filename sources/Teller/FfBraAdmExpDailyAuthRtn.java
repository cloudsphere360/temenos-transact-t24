package com.temenos.fusion;

import java.util.List;

import com.temenos.api.TStructure;
import com.temenos.t24.api.complex.eb.servicehook.TransactionData;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.ebffbranchadminexpdaily.EbFfBranchAdminExpDailyRecord;
import com.temenos.t24.api.records.ebffftbranchadminexpensesupd.EbFfFtBranchAdminExpensesUpdRecord;
import com.temenos.t24.api.records.ebffftbranchadminexpensesupd.FfBaExpTypeClass;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;
import com.temenos.t24.api.tables.ebffbranchadminexpdaily.EbFfBranchAdminExpDailyTable;

/**
 *
 * @author ar116388
 *
 */
public class FfBraAdmExpDailyAuthRtn extends RecordLifecycle {
    
    private static final FusionFileLogger FfBraAdmExpDailyAuth = FusionFileLogger.getLogger(FfBraAdmExpDailyAuthRtn.class);

    DataAccess da = new DataAccess(this);

    String ytoday = "";
    String id = "";
    String yCreditAmt = "";
    String yDebitValueDate = "";

    @Override
    public void postUpdateRequest(String application, String currentRecordId, TStructure currentRecord,
            List<TransactionData> transactionData, List<TStructure> currentRecords,
            TransactionContext transactionContext) {

        Session sess = new Session(this);
        String coCode = sess.getCompanyId();

        ytoday = sess.getCurrentVariable("!TODAY");

        FfBraAdmExpDailyAuth.info("todayDate " + ytoday);

        id = coCode + "-" + ytoday;

        FfBraAdmExpDailyAuth.info("Branch Admin Expenses Daily Auth Routine starts...");

        String yExpType = "";

        EbFfBranchAdminExpDailyRecord exprecdaily = new EbFfBranchAdminExpDailyRecord(this);

        try {
            EbFfFtBranchAdminExpensesUpdRecord exprec = new EbFfFtBranchAdminExpensesUpdRecord(
                    da.getRecord("EB.FF.FT.BRANCH.ADMIN.EXPENSES.UPD", id));

            yCreditAmt = exprec.getCreditAmt().getValue();
            yDebitValueDate = exprec.getDebitValueDate().getValue();

            List<FfBaExpTypeClass> yExpTypeListt = exprec.getFfBaExpType();
            for (int i = 0; i < yExpTypeListt.size(); i++) {
                yExpType = yExpTypeListt.get(i).getFfBaExpType().getValue();

                String yAmt = "";

                yAmt = yExpTypeListt.get(i).getFfBaExpAmt().getValue();

                com.temenos.t24.api.records.ebffbranchadminexpdaily.FfBaExpTypeClass yExpTypeClass = new com.temenos.t24.api.records.ebffbranchadminexpdaily.FfBaExpTypeClass();

                yExpTypeClass.setFfBaExpAmt(yAmt);
                yExpTypeClass.setFfBaExpType(yExpType);

                exprecdaily.addFfBaExpType(yExpTypeClass);

            }

            exprecdaily.setCreditAmt(exprec.getCreditAmt());
            exprecdaily.setDebitValueDate(exprec.getDebitValueDate());

        } catch (Exception e) {
            FfBraAdmExpDailyAuth.info("Exception is : " + e);
        }

        try {
            EbFfBranchAdminExpDailyTable branchadmexptab = new EbFfBranchAdminExpDailyTable(this);
            branchadmexptab.write(id, exprecdaily);
        } catch (Exception e) {
            FfBraAdmExpDailyAuth.info("Exception caught : " + e);
        }

    }

}
