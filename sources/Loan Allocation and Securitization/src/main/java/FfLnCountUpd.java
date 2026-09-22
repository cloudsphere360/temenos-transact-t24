package com.temenos.fusion;
import java.util.List;
import com.temenos.api.TStructure;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.ebfffunderdetails.EbFfFunderDetailsRecord;
import com.temenos.t24.api.records.ebfffunderdetails.FfFunderNameTrancheClass;

/**
 * TODO: Document me!
 *
 * @author sr115630
 *
 */
public class FfLnCountUpd extends RecordLifecycle {
    private static final FusionFileLogger FF_LN_COUNT_UPD = FusionFileLogger.getLogger(FfLnCountUpd.class);
    @Override
    public void defaultFieldValues(String application, String currentRecordId, TStructure currentRecord,
            TStructure unauthorisedRecord, TStructure liveRecord, TransactionContext transactionContext) {
        EbFfFunderDetailsRecord liveFunder = new EbFfFunderDetailsRecord(liveRecord);
        EbFfFunderDetailsRecord currentFunder = new EbFfFunderDetailsRecord(currentRecord);

        List<FfFunderNameTrancheClass> liveList = liveFunder.getFfFunderNameTranche();
        List<FfFunderNameTrancheClass> currentList = currentFunder.getFfFunderNameTranche();

        for (int i = 0; i < currentList.size(); i++) {

            if (i >= liveList.size()) {
                currentList.get(i).getFfLoanCount().set("");
                continue;
            }

            String liveTranche = liveList.get(i).getFfFunderNameTranche().getValue();
            FF_LN_COUNT_UPD.info("liveTranche"+liveTranche);
            String currentTranche = currentList.get(i).getFfFunderNameTranche().getValue();
            FF_LN_COUNT_UPD.info("");
            if (!liveTranche.equals(currentTranche)) {
                currentList.get(i).getFfLoanCount().set("");
            }
            currentRecord.set(currentFunder.toStructure());
            FF_LN_COUNT_UPD.info("");
        }

    }
}
