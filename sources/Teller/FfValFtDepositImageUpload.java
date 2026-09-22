package com.temenos.fusion;

import com.temenos.api.TStructure;
import com.temenos.api.TValidationResponse;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.company.CompanyRecord;
import com.temenos.t24.api.records.fundstransfer.FundsTransferRecord;
import com.temenos.t24.api.records.imdocumentimage.ImDocumentImageRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;

/**
 * -----------------------------------------------------------------------------
 * Modification History :
 * -----------------------------------------------------------------------------
 * Description : This Routine used to Validate the Image Upload process
 *
 * Developed By : Harshini Sakthivel
 *
 * Development Reference : Teller Report
 *
 * Attached To : VERSION>IM.DOCUMENT.IMAGE,CAPTURE.IMG >FF.FT.IMAGE.UPLOAD.VAL
 * 
 * Attached As : Input Routine
 * 
 * -----------------------------------------------------------------------------
 */
public class FfValFtDepositImageUpload extends RecordLifecycle {

    private static final FusionFileLogger ftDepositImageUpload = FusionFileLogger
            .getLogger(FfValFtDepositImageUpload.class);
    DataAccess da = new DataAccess(this);
    Session ss = new Session(this);
    String finMnemonic = "";
    ImDocumentImageRecord imgRecord = null;

    @Override
    public TValidationResponse validateRecord(String application, String currentRecordId, TStructure currentRecord,
            TStructure unauthorisedRecord, TStructure liveRecord, TransactionContext transactionContext) {
        ftDepositImageUpload.info("FfValFtDepositImageUpload is triggered Successfully");

        try {
            initialiseCompanyInfo(ss.getCompanyId());
            imgRecord = new ImDocumentImageRecord(currentRecord);
            String ftId = imgRecord.getImageReference().getValue();
            ftDepositImageUpload.info("ImageReference" + ftId);

            FundsTransferRecord ftRec = new FundsTransferRecord(da.getRecord(finMnemonic, "FUNDS.TRANSFER", "", ftId));
            String supportingDoc = ftRec.getLocalRefField("SUPPORTING.DOC").getValue();
            if ((!supportingDoc.isEmpty())) {
                imgRecord.getImageReference().setError(" - Image Proof already uploaded");
            }
            currentRecord.set(imgRecord.toStructure());
        } catch (Exception e) {
            ftDepositImageUpload.error("FfValFtDepositImageUpload issue" + e);
        }
        return imgRecord.getValidationResponse();
    }

    /**
     * @param companyId
     */
    private void initialiseCompanyInfo(String companyId) {
        try {
            CompanyRecord companyObj = new CompanyRecord(da.getRecord("COMPANY", companyId));
            finMnemonic = companyObj.getFinancialMne().getValue();
        } catch (Exception e) {
            ftDepositImageUpload.error("initialiseCompanyInfo issue" + e);
        }

    }

}
