package com.temenos.fusion;

import com.temenos.api.TStructure;
import com.temenos.t24.api.complex.eb.servicehook.TransactionData;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.ebfffunderdetails.EbFfFunderDetailsRecord;
import com.temenos.t24.api.records.imdocumentimage.ImDocumentImageRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.tables.ebfffunderdetails.EbFfFunderDetailsTable;

/**
 * TODO: Document me!
 *
 * @author sr115630
 *
 */
public class ImgUpdAuthRtn extends RecordLifecycle {

    private static final FusionFileLogger imgUpdAuthRtn = FusionFileLogger.getLogger(ImgUpdAuthRtn.class);

    DataAccess da = new DataAccess(this);

    @Override
    public void postUpdateRequest(String application, String currentRecordId, TStructure currentRecord,
            java.util.List<TransactionData> transactionData, java.util.List<TStructure> currentRecords,
            TransactionContext transactionContext) {

        String yImgDocID = currentRecordId;
        imgUpdAuthRtn.info("yImgDocID -> " + yImgDocID);
        ImDocumentImageRecord imgDocRec = new ImDocumentImageRecord(currentRecord);
        String yCusId = imgDocRec.getImageReference().getValue();
        imgUpdAuthRtn.info("yCusId"+yCusId);

        try {

            imgUpdAuthRtn.info("Fetching record from EB.FF.FUNDER.DETAILS");

            EbFfFunderDetailsRecord yPettyCahDetRec = new EbFfFunderDetailsRecord(
                    da.getRecord("EB.FF.FUNDER.DETAILS", yCusId));

            imgUpdAuthRtn.info("Record fetched successfully");

            EbFfFunderDetailsTable yPettyCahDetTable = new EbFfFunderDetailsTable(this);

            imgUpdAuthRtn.info("Updating Document field with -> " + yImgDocID);

            yPettyCahDetRec.setDocument(yImgDocID);
            

            imgUpdAuthRtn.info("Writing updated record into table");

            yPettyCahDetTable.write(yCusId, yPettyCahDetRec);

            imgUpdAuthRtn.info("Record updated successfully for ID -> " + yImgDocID);

        } catch (Exception e) {

            imgUpdAuthRtn.info("Exception occurred while processing record");
            imgUpdAuthRtn.info("Error Message -> " + e.getMessage());
            imgUpdAuthRtn.info("Record Not Found");
        }

    }

}
