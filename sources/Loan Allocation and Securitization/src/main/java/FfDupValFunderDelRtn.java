package com.temenos.fusion;

import java.util.HashSet;
import java.util.Set;
import com.temenos.api.TStructure;
import com.temenos.api.TValidationResponse;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.ebfffunderdetails.EbFfFunderDetailsRecord;

/**
 * TODO: Document me!
 *
 * @author sr115630
 *
 */
public class FfDupValFunderDelRtn extends RecordLifecycle {
    private static final FusionFileLogger FfDupValFunderDelRtn = FusionFileLogger.getLogger(FfDupValFunderDelRtn.class);

     private static final String EB_FUNDER_NAME_TRANCHE="EB-FUNDER.NAME.TRANCHE";
    @Override
    public TValidationResponse validateField(String application, String recordId, String fieldData, TStructure record) {

        FfDupValFunderDelRtn.info("Entering to method");
        EbFfFunderDetailsRecord ebfflCustomer = new EbFfFunderDetailsRecord(record);
        FfDupValFunderDelRtn.info("ebfflCustomer" + ebfflCustomer);
        try {
            Set<String> yExpNewSet = new HashSet<>();

            for (int j = 0; j < ebfflCustomer.getFfFunderNameTranche().size(); j++) {

                String yExpenseType = ebfflCustomer.getFfFunderNameTranche().get(j).getFfFunderNameTranche().getValue();

                if (yExpenseType != null) {

                    String normalizedValue = yExpenseType.trim().toUpperCase();

                    if (!normalizedValue.isEmpty() && !yExpNewSet.add(normalizedValue)) {

                        ebfflCustomer.getFfFunderNameTranche().get(j).getFfFunderNameTranche()
                                .setError(EB_FUNDER_NAME_TRANCHE);

                        FfDupValFunderDelRtn.info("Repeated Exp");
                    }
                }
            }
        } catch (Exception ex) {
            FfDupValFunderDelRtn.info("Repeat Missing");
        }

        return ebfflCustomer.getValidationResponse();
    }

}
