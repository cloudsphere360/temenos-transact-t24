package com.bct.fusion.bulk.upload;

import java.util.ArrayList;
import java.util.List;

import com.temenos.api.TField;
import com.temenos.api.TStructure;
import com.temenos.api.TValidationResponse;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.ebcompanychange.EbCompanyChangeRecord;
import com.temenos.t24.api.records.ebffcentredetail.EbFfCentreDetailRecord;
import com.temenos.t24.api.records.ebfileupload.EbFileUploadRecord;
import com.temenos.t24.api.records.user.UserRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;

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
public class FusionValidateCompany extends RecordLifecycle {

    @Override
    public TValidationResponse validateRecord(String application, String currentRecordId, TStructure currentRecord,
            TStructure unauthorisedRecord, TStructure liveRecord, TransactionContext transactionContext) {
        DataAccess da = new DataAccess(this);
        if (application.equals("EB.COMPANY.CHANGE")) {
            EbCompanyChangeRecord ebCompanyChangeRecord = new EbCompanyChangeRecord(currentRecord);
            getValidationCC(ebCompanyChangeRecord, da);

            currentRecord.set(ebCompanyChangeRecord.toStructure());
            return ebCompanyChangeRecord.getValidationResponse();
        }
        if (application.equals("EB.FF.CENTRE.DETAIL")) {
            EbFfCentreDetailRecord ebFfCentreDetailRecord = new EbFfCentreDetailRecord(currentRecord);
            getValidationRO(ebFfCentreDetailRecord, transactionContext, da);
            return ebFfCentreDetailRecord.getValidationResponse();
        }
        return null;

    }

    /**
     * @param ebFfCentreDetailRecord
     * @param transactionContext
     * @param da
     */
    private void getValidationRO(EbFfCentreDetailRecord ebFfCentreDetailRecord, TransactionContext transactionContext,
            DataAccess da) {
        try {
            EbFileUploadRecord ebFileUploadRecord = new EbFileUploadRecord(
                    da.getRecord("EB.FILE.UPLOAD", transactionContext.getCurrentVersionId()));
            boolean errorFlag = getValidation(ebFileUploadRecord, da, ebFfCentreDetailRecord.getBranch().getValue());
            if (errorFlag) {
                List<String> Error = new ArrayList<>();
                Error.add("EB-FF.INVALID.ACCOUNT.COMPANY");
                Error.add(String.valueOf("Centre"));
                ebFfCentreDetailRecord.getBranch().setError(Error.toString());

            }

        } catch (Exception e) {
        }
    }

    /**
     * @param ebCompanyChangeRecord
     * @param da
     */
    private void getValidationCC(EbCompanyChangeRecord ebCompanyChangeRecord, DataAccess da) {
        String notes = ebCompanyChangeRecord.getLocalRefField("FF.NOTES").getValue();
        try {
            if (notes.contains("&") && (!notes.split("[&]", -1)[1].equals(""))) {
                EbFileUploadRecord ebFileUploadRecord = new EbFileUploadRecord(
                        da.getRecord("EB.FILE.UPLOAD", notes.split("[&]", -1)[1]));
                boolean errorFlag = getValidation(ebFileUploadRecord, da, (new Session(this)).getCompanyId());
                if (errorFlag) {
                    List<String> Error = new ArrayList<>();
                    Error.add("EB-FF.INVALID.ACCOUNT.COMPANY");
                    Error.add(String.valueOf("Account"));
                    ebCompanyChangeRecord.getContractKey().setError(Error.toString()); // account
                } else {
                    ebCompanyChangeRecord.getLocalRefField("FF.NOTES").setValue(notes.split("[&]", -1)[0]);

                }
            }
        } catch (Exception e) {
        }
    }

    /**
     * @param ebFileUploadRecord
     * @param ebCompanyChangeRecord
     * @param da
     * @return
     */
    private boolean getValidation(EbFileUploadRecord ebFileUploadRecord, DataAccess da, String company) {

        try {
            UserRecord userRecord = new UserRecord(da.getRecord("USER", ebFileUploadRecord.getUploadUser().getValue()));

            for (TField companyCode : userRecord.getCompanyCode()) {
                if (companyCode.getValue().equals(company) || companyCode.getValue().equals("ALL")) {
                    return false;
                }
            }

        } catch (Exception e) {
        }
        return true;
    }

}
