package com.temenos.fusion;

import java.math.BigDecimal;

import com.temenos.api.TStructure;
import com.temenos.api.TValidationResponse;
import com.temenos.api.exceptions.T24CoreException;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.ebpettycashlimit.EbPettyCashLimitRecord;

/**
 * TODO: Document me!
 *
 * @author mp116300
 *
 */
public class FfValRtnPettyUpld extends RecordLifecycle {

    @Override
    public TValidationResponse validateRecord(String application, String currentRecordId, TStructure currentRecord,
            TStructure unauthorisedRecord, TStructure liveRecord, TransactionContext transactionContext) {

        EbPettyCashLimitRecord ebRec = new EbPettyCashLimitRecord(currentRecord);
        String pettyCash = ebRec.getPettyCash().toString();
        BigDecimal pettyCashBd = new BigDecimal(pettyCash);
        if (pettyCashBd.compareTo(BigDecimal.ZERO) < 0) {
            throw new T24CoreException("", "EB-FF.VAL.PETTY.UPLOAD");
        }

        currentRecord.set(ebRec.toStructure());
        return ebRec.getValidationResponse();

    }

}
