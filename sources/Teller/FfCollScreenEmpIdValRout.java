package com.temenos.fusion;

import java.util.List;

import com.temenos.api.TStructure;
import com.temenos.api.TValidationResponse;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.ebffcollpostingscreen.EbFfCollPostingScreenRecord;
import com.temenos.t24.api.records.ebffcollpostingscreen.EmployeeIdClass;

/**
 * TODO: Document me!
 *
 * @author mp116300
 *
 */
public class FfCollScreenEmpIdValRout extends RecordLifecycle {

    @Override
    public TValidationResponse validateRecord(String application, String currentRecordId, TStructure currentRecord,
            TStructure unauthorisedRecord, TStructure liveRecord, TransactionContext transactionContext) {

        EbFfCollPostingScreenRecord ebCollRec = new EbFfCollPostingScreenRecord(currentRecord);
        List<EmployeeIdClass> empIdList = ebCollRec.getEmployeeId();
        int lisSize = empIdList.size();

        for (int i = 0; i < lisSize; i++) {
            EmployeeIdClass empIdObj = empIdList.get(i);
            String empId = empIdObj.getEmployeeId().toString();
            if (empId.equals("")) {
                empIdObj.getEmployeeId().setError("Employee Id should not be null");
            }

        }

        currentRecord.set(ebCollRec.toStructure());
        return ebCollRec.getValidationResponse();

    }

}
