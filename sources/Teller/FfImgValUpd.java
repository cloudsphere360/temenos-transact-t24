package com.temenos.fusion;

import com.temenos.api.TStructure;
import com.temenos.api.TValidationResponse;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.ebffftbranchadminexpensesupd.EbFfFtBranchAdminExpensesUpdRecord;
import com.temenos.t24.api.records.ebffpettycashroupld.EbFfPettyCashRoUpldRecord;
import com.temenos.t24.api.records.imdocumentimage.ImDocumentImageRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;

/**
 * @author jh116454
 * @Attached To: VERSION>IM.DOCUMENT.IMAGE,PETTY.CASH.IMG
 *           VERSION>IM.DOCUMENT.IMAGE,BRANCH.ADMIN.EXP.ING
 * @Attached As: Input routine > EB.API > FF.IMG.VAL.UPD
 * @Description: If image is already uploaded then throw error
 * 
 */

public class FfImgValUpd extends RecordLifecycle {

    private static final FusionFileLogger yImgValUpdLog = FusionFileLogger.getLogger(FfImgValUpd.class);
    DataAccess da = new DataAccess(this);
    Session ses = new Session(this);
  
    @Override
    public TValidationResponse validateRecord(String application, String currentRecordId, TStructure currentRecord,
            TStructure unauthorisedRecord, TStructure liveRecord, TransactionContext transactionContext) {

        ImDocumentImageRecord yImgRecord = new ImDocumentImageRecord(currentRecord);
    
        String ySupportingDoc = "";

        String yAppliName = yImgRecord.getImageApplication().getValue();
        yImgValUpdLog.info("Application Name-> " + yAppliName);

        String yImgRef = yImgRecord.getImageReference().getValue();
        yImgValUpdLog.info("Img Ref -> " + yImgRef);

        if (yAppliName.equals("EB.FF.PETTY.CASH.RO.UPLD")) {
            yImgValUpdLog.info("inside petty cash");
            EbFfPettyCashRoUpldRecord yPettyCashUpd = new EbFfPettyCashRoUpldRecord(
                    da.getRecord("", yAppliName, "", yImgRef));
            ySupportingDoc = yPettyCashUpd.getSupportingDoc().getValue();

            if (!ySupportingDoc.equals("")) {
                yImgRecord.getImageReference().setError(" - Image Proof already uploaded");
            }
        }

        if (yAppliName.equals("EB.FF.FT.BRANCH.ADMIN.EXPENSES.UPD")) {
            yImgValUpdLog.info("inside Branch Admin");
            EbFfFtBranchAdminExpensesUpdRecord yBranAdminUpd = new EbFfFtBranchAdminExpensesUpdRecord(
                    da.getRecord("", yAppliName, "", yImgRef));
            ySupportingDoc = yBranAdminUpd.getSupportingDoc().getValue();

            if (!ySupportingDoc.equals("")) {
                yImgRecord.getImageReference().setError(" - Image Proof already uploaded");
            }
        }
        currentRecord.set(yImgRecord.toStructure());

        return yImgRecord.getValidationResponse();
    }

}
