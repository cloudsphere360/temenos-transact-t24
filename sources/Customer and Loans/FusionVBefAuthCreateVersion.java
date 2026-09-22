package com.bct.fusion.bulk.upload;

import java.util.List;

import com.temenos.api.TStructure;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.complex.eb.templatehook.TransactionData;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.ebfileupload.EbFileUploadRecord;
import com.temenos.t24.api.records.ebfileuploadtype.EbFileUploadTypeRecord;
import com.temenos.t24.api.records.version.VersionRecord;
import com.temenos.t24.api.system.DataAccess;

/*---------------------------------------------------------------------------------
* * Product          :
* * Developed by     : 
* * Routine Type     : 
* * Date             :  
* * Description      :  
* * Attached To      : 
* * EB.API Record ID :
* * In Parameters    : 
* * Out Parameters   :
* * Reference        :
* *--------------------------------------------------------------------------------
* * Revision History :
* *-----------------
* * Date          - <Developer> -  Description
* *---------------------------------------------------------------------------------
* *--------------------------------------------------------------------------------*/
public class FusionVBefAuthCreateVersion extends RecordLifecycle {

    @Override
    public void updateRecord(String application, String currentRecordId, TStructure currentRecord,
            TStructure unauthorisedRecord, TStructure liveRecord, TransactionContext transactionContext,
            List<TransactionData> transactionData, List<TStructure> currentRecords) {
        DataAccess da = new DataAccess(this);
        EbFileUploadRecord ebFileUploadRecord = new EbFileUploadRecord(currentRecord);

        try {
            EbFileUploadTypeRecord ebFileUploadTyRecord = new EbFileUploadTypeRecord(
                    da.getRecord("EB.FILE.UPLOAD.TYPE", ebFileUploadRecord.getUploadType().getValue()));
            String itemsUpdVersion = ebFileUploadTyRecord.getItemsUpdVersion().getValue();
            String itemsUpdApplication = ebFileUploadTyRecord.getItemsUpdAppl().getValue();
            VersionRecord versionRecord = new VersionRecord(da.getRecord("VERSION", itemsUpdVersion));
            currentRecords.add(versionRecord.toStructure());
            TransactionData txnData = new TransactionData();
            txnData.setFunction("I");
            txnData.setNumberOfAuthoriser("0");
            txnData.setTransactionId(itemsUpdApplication + "?" + currentRecordId);
            txnData.setVersionId("VERSION,FILEUPD");
            transactionData.add(txnData);
        } catch (Exception e) {
        }

    }

}
