package com.temenos.fusion;

import java.math.BigDecimal;
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
public class FfCollScreenCashByBmValRout extends RecordLifecycle {

    private static final FusionFileLogger FfCollScreenCashByBmValRout = FusionFileLogger
            .getLogger(FfCollScreenCashByBmValRout.class);

    @Override
    public TValidationResponse validateRecord(String application, String currentRecordId, TStructure currentRecord,
            TStructure unauthorisedRecord, TStructure liveRecord, TransactionContext transactionContext) {

        EbFfCollPostingScreenRecord collScrObj = new EbFfCollPostingScreenRecord(currentRecord);
        FfCollScreenCashByBmValRout.info("Current Record " + currentRecord.toString());
        List<EmployeeIdClass> collScrnList = collScrObj.getEmployeeId();
        FfCollScreenCashByBmValRout.info("collScrnList Record " + collScrnList.toString());
        for (int i = 0; i < collScrnList.size(); i++) {
            EmployeeIdClass collScrnObj = collScrnList.get(i);
            FfCollScreenCashByBmValRout.info("collScrnObj " + collScrnObj.toString());
            String pendCollRo = collScrnObj.getRoPendingCollection().getValue();
            FfCollScreenCashByBmValRout.info("pendCollRo " + pendCollRo);
            BigDecimal pendCollBD = new BigDecimal(pendCollRo);
            FfCollScreenCashByBmValRout.info("pendCollBD " + pendCollBD.toString());
            String cashByBM = collScrnObj.getRoCashCollected().getValue();
            FfCollScreenCashByBmValRout.info("cashByBM " + cashByBM);
            BigDecimal cashByBMBD = new BigDecimal(cashByBM);
            FfCollScreenCashByBmValRout.info("cashByBMBD " + cashByBMBD.toString());
            if (pendCollBD.compareTo(BigDecimal.ZERO) < 0) {
                FfCollScreenCashByBmValRout.info("Entering if block ");
                collScrnObj.getRoPendingCollection()
                        .setError("Collected amount cannot be greater than pending amount, please re-verify.");
            }
            if (cashByBMBD.compareTo(BigDecimal.ZERO) < 0) {
                collScrnObj.getRoCashCollected().setError("Negative Amount not allowed to Enter");
            }

        }

        currentRecord.set(collScrObj.toStructure());
        return collScrObj.getValidationResponse();
    }

}
