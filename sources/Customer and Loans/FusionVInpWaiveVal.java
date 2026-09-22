package com.bct.fusion.bulk.upload;

import com.temenos.api.TStructure;
import com.temenos.api.TValidationResponse;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.aaactivityhistory.AaActivityHistoryRecord;
import com.temenos.t24.api.records.aaactivityhistory.ActivityRefClass;
import com.temenos.t24.api.records.aaactivityhistory.EffectiveDateClass;
import com.temenos.t24.api.records.aaarrangementactivity.AaArrangementActivityRecord;
import com.temenos.t24.api.system.DataAccess;

/*---------------------------------------------------------------------------------
* * Product          :
* * Developed by     : Gopal
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
public class FusionVInpWaiveVal extends RecordLifecycle {
    DataAccess da = new DataAccess(this);

    @Override
    public TValidationResponse validateRecord(String application, String currentRecordId, TStructure currentRecord,
            TStructure unauthorisedRecord, TStructure liveRecord, TransactionContext transactionContext) {

        AaArrangementActivityRecord aaaRec = new AaArrangementActivityRecord(currentRecord);
        String arrId = aaaRec.getArrangement().getValue();

        try {
            AaActivityHistoryRecord actHisRec = new AaActivityHistoryRecord(da.getRecord("AA.ACTIVITY.HISTORY", arrId));
            for (EffectiveDateClass effDte : actHisRec.getEffectiveDate()) {
                for (ActivityRefClass act : effDte.getActivityRef()) {
                    if (act.getActivity().getValue().equalsIgnoreCase("LENDING-WRITE.OFF-BAL.MAINTAIN")) {
                        aaaRec.getActivity().setError("EB-AA.WAIVE.OFF");
                    }
                }
            }
        } catch (Exception e) {

        }

        return aaaRec.getValidationResponse();
    }

}
