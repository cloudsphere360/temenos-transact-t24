package com.temenos.fusion;

import java.time.LocalTime;
import java.util.List;

import com.temenos.api.TStructure;
import com.temenos.api.exceptions.T24CoreException;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.complex.eb.templatehook.TransactionData;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.ebffparameter.EbFfParameterRecord;
import com.temenos.t24.api.records.ebffparameter.ParamDescClass;
import com.temenos.t24.api.system.DataAccess;

/**
 *
 * @author ar116388
 *
 */
public class FfEODBefAuthRtn extends RecordLifecycle {

    private static final FusionFileLogger EODBefAuthRtn = FusionFileLogger.getLogger(FfEODBefAuthRtn.class);
    private static final LocalTime DEFAULT_CUTOFF_TIME = LocalTime.of(20, 30);

    @Override
    public void updateRecord(String application, String currentRecordId, TStructure currentRecord,
            TStructure unauthorisedRecord, TStructure liveRecord, TransactionContext transactionContext,
            List<TransactionData> transactionData, List<TStructure> currentRecords) {

        EODBefAuthRtn.info("Starting EOD Before Auth Routine");
        EODBefAuthRtn.info("Application: " + application + ", Current Record ID: " + currentRecordId);

        DataAccess da = new DataAccess(this);

        LocalTime cutoffTime = DEFAULT_CUTOFF_TIME;
        EODBefAuthRtn.info("Default cutoff time set to: " + cutoffTime);

        // Fetch cutoff time from EB.FF.PARAMETER

        try {
            EODBefAuthRtn.info("Fetching cutoff time from EB.FF.PARAMETER...");

            EbFfParameterRecord paramRec = new EbFfParameterRecord(da.getRecord("EB.FF.PARAMETER", "FF.CUTOFF.TIME"));

            EODBefAuthRtn.info("paramRec is " + paramRec);

            String paramValue = "";

            for (ParamDescClass paramDesc : paramRec.getParamDesc()) {
                String paramName = paramDesc.getParamName().getValue();

                if ("CUTOFF.TIME".equalsIgnoreCase(paramName)) {
                    paramValue = paramDesc.getParamValue().getValue();
                    EODBefAuthRtn.info("paramValue is: " + paramValue);
                    break;
                }
            }

            if (paramValue != null && !paramValue.trim().isEmpty()) {
                cutoffTime = LocalTime.parse(paramValue.trim());
                EODBefAuthRtn.info("Cutoff Time from parameter table: " + cutoffTime);
            } else {
                EODBefAuthRtn.info("Cutoff parameter value is empty. Using default: " + cutoffTime);
            }

        } catch (Exception e) {
            EODBefAuthRtn.error("Error fetching cutoff time. Using default: " + cutoffTime, e);
        }

        // Validate against cutoff time

        try {
            LocalTime currentTime = LocalTime.now(); // server time
            EODBefAuthRtn.info("Current Time [Server]: " + currentTime);

            EODBefAuthRtn.info("Cutoff time for validation: " + cutoffTime);

            if (!currentTime.isBefore(cutoffTime)) {
                EODBefAuthRtn.error("Current time " + currentTime + " exceeds cutoff " + cutoffTime);

                // Business exception
                throw new T24CoreException("", "EOD/Re-EOD not allowed after cutoff time " + cutoffTime);
            }

            EODBefAuthRtn.info("Current time is before cutoff. EOD allowed.");

        } catch (T24CoreException e) {
            // Rethrow business exception
            EODBefAuthRtn.info("Current time EXCEEDS cutoff. EOD NOT allowed.");
            throw e;

        } catch (Exception e) {

            EODBefAuthRtn.error("Unexpected error during time validation", e);

        }

        EODBefAuthRtn.info("Ending EOD Before Auth Routine");

    }

}
