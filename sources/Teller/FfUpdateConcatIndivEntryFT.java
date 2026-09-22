package com.temenos.fusion;

import java.util.List;

import com.temenos.api.TStructure;
import com.temenos.t24.api.complex.eb.servicehook.TransactionData;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.ebffrocollectionapi.EbFfRoCollectionApiRecord;
import com.temenos.t24.api.records.fundstransfer.FundsTransferRecord;
import com.temenos.t24.api.system.Session;
import com.temenos.t24.api.tables.ebffrocollectionapi.EbFfRoCollectionApiTable;

/**
 * TODO: Document me!
 *
 * @author mp116300
 *
 */
public class FfUpdateConcatIndivEntryFT extends RecordLifecycle {
    private final Session ses = new Session();

    private static final FusionFileLogger FfUpdateConcatIndivEntryFT = FusionFileLogger
            .getLogger(FfUpdateConcatIndivEntryFT.class);

    @Override
    public void postUpdateRequest(String application, String currentRecordId, TStructure currentRecord,
            List<TransactionData> transactionData, List<TStructure> currentRecords,
            TransactionContext transactionContext) {

        FfUpdateConcatIndivEntryFT.info("=== START postUpdateRequest ===");

        try {
            /*
             * we will update the new concat table EB.FF.RO.COLLECTION.API
             * 
             * @ID --> 12122*20251102-IN0011007*FT12343211
             * 
             * RO Id: 12122 RO Emp Name: Parvesh Pay Mode: Cash/UPI/BBPS Amount: 2000
             */

            FundsTransferRecord ft = new FundsTransferRecord(currentRecord);

            String compCode = safeString(ses.getCompanyId());
            String processDate = safeString(ft.getProcessingDate());
            String postBy = safeString(ft.getLocalRefField("FF.POSTED.BY"));
            String postName = safeString(ft.getLocalRefField("FF.POST.LGLNAME"));
            String credAmount = safeString(ft.getCreditAmount());
            String payMode = safeString(ft.getLocalRefField("FF.PYMT.MODE"));

            String idConcat = postBy + "*" + processDate + "-" + compCode + "*" + currentRecordId;
            FfUpdateConcatIndivEntryFT.info("Id of the new concat " + idConcat);

            EbFfRoCollectionApiRecord newConcat = new EbFfRoCollectionApiRecord(this);

            newConcat.setRoEmpId(postBy);
            newConcat.setRoEmpName(postName);
            newConcat.setPaymentMode(payMode);
            newConcat.setAmount(credAmount);
            FfUpdateConcatIndivEntryFT.info("NewConcat record " + newConcat.toString());

            try {
                EbFfRoCollectionApiTable newConcatTab = new EbFfRoCollectionApiTable(this);
                newConcatTab.write(idConcat, newConcat);
            } catch (Exception e) {
                e.getMessage();
            }

            /*
             * TransactionData transactionDataObj = new TransactionData();
             * transactionDataObj.setVersionId(OFS_VERSION);
             * transactionDataObj.setFunction("INPUT");
             * transactionDataObj.setSourceId(OFS_SOURCE);
             * transactionDataObj.setNumberOfAuthoriser("0");
             * transactionDataObj.setTransactionId(idConcat);
             * transactionData.add(transactionDataObj);
             * currentRecords.add(newConcat.toStructure());
             * FfUpdateConcatIndivEntryFT.info("FfUpdateConcatIndivEntryFT transactionData "
             * + transactionData.toString());
             */

        } catch (Exception e) {
            e.getMessage();
        }

    }

    private String safeString(Object v) {
        return v == null ? "" : v.toString().trim();
    }

}
