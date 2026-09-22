package com.temenos.fusion;

import java.util.List;

import com.temenos.api.TStructure;
import com.temenos.t24.api.complex.eb.servicehook.TransactionData;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.fundstransfer.FundsTransferRecord;
import com.temenos.t24.api.records.imdocumentimage.ImDocumentImageRecord;
import com.temenos.t24.api.system.DataAccess;

/**
 * -----------------------------------------------------------------------------
 * Modification History :
 * -----------------------------------------------------------------------------
 * Description : This Routine used to update IM.DOCUMENT.IMAGE application @ID
 * updated in the Funds Transfer application supporting document field during
 * authorisation.
 *
 * Developed By : Preethi Selvam
 *
 * Development Reference : Teller Report
 *
 * Attached To : VERSION>IM.DOCUMENT.UPLOAD,CAPTURE.IMG >FF.FT.AUTH.UPDIMG
 * 
 * Attached As : Auth Routine
 * 
 * -----------------------------------------------------------------------------
 */

public class FfVAuthUpdImg extends RecordLifecycle {
    private static final FusionFileLogger AuthUpdImg = FusionFileLogger.getLogger(FfVAuthUpdImg.class);
    DataAccess da = new DataAccess(this);
    ImDocumentImageRecord imgRecord = null;
    FundsTransferRecord ftRecord = null;
    String imgReferance = "";
    String applicationImg = "";
    String imgType = "";
    public static final String FTTABLE = "FUNDS.TRANSFER";

    @Override
    public void postUpdateRequest(String application, String currentRecordId, TStructure currentRecord,
            List<TransactionData> transactionData, List<TStructure> currentRecords,
            TransactionContext transactionContext) {

        try {
            AuthUpdImg.info("FfVAuthUpdImg: Routine triggered for Funds Transfer Supporting Document Field update.");
            imgRecord = new ImDocumentImageRecord(da.getRecord("IM.DOCUMENT.IMAGE", currentRecordId));
            imgReferance = imgRecord.getImageReference().getValue();
            applicationImg = imgRecord.getImageApplication().getValue();
            imgType = imgRecord.getImageType().getValue();
        } catch (Exception e) {
            AuthUpdImg.error("FfVAuthUpdImg:- Error fetching record for Image Application {}: {}" + e);
        }

        if ((applicationImg.equals(FTTABLE)) && (imgType.equals("PHOTOS"))) {
            try {
                ftRecord = new FundsTransferRecord(da.getRecord(FTTABLE, imgReferance));
                String supportingDocument = currentRecordId.replace("-", "");
                AuthUpdImg.info("FfVAuthUpdImg:- supportingDocument: {}" + supportingDocument);
                ftRecord.getLocalRefField("SUPPORTING.DOC").setValue(supportingDocument);
                da.updateLocalfields(FTTABLE, imgReferance, ftRecord.toStructure());
            } catch (Exception ex2) {
                AuthUpdImg.error("FfVAuthUpdImg:- Error fetching record for Image Application {}: {}", ex2);
            }
        }

    }

}