package com.temenos.fusion;

import com.temenos.api.TStructure;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.ebffeodscreen.EbFfEodScreenRecord;

public class FfEODDenomDef extends RecordLifecycle {

    private static final FusionFileLogger FfEODDenomDefRtn = FusionFileLogger.getLogger(FfEODDenomDef.class);

    @Override
    public void defaultFieldValues(String application, String currentRecordId, TStructure currentRecord,
            TStructure unauthorisedRecord, TStructure liveRecord, TransactionContext transactionContext) {

        FfEODDenomDefRtn.info("FfEODDenomDef Routine Started");

        EbFfEodScreenRecord ebodrecc = new EbFfEodScreenRecord(currentRecord);

        FfEODDenomDefRtn.info("EOD Record Loaded Successfully");

        long grandTotal = 0;

        try {

            FfEODDenomDefRtn.info("STARTING DENOMINATION CALCULATION");

            grandTotal += calculateDenomination(ebodrecc.getDenomQuantity1().getValue(), 1,
                    ebodrecc::setDenomTypeTotal1);

            grandTotal += calculateDenomination(ebodrecc.getDenomQuantity2().getValue(), 2,
                    ebodrecc::setDenomTypeTotal2);

            grandTotal += calculateDenomination(ebodrecc.getDenomQuantity5().getValue(), 5,
                    ebodrecc::setDenomTypeTotal5);

            grandTotal += calculateDenomination(ebodrecc.getDenomQuantity10().getValue(), 10,
                    ebodrecc::setDenomTypeTotal10);

            grandTotal += calculateDenomination(ebodrecc.getDenomQuantity20().getValue(), 20,
                    ebodrecc::setDenomTypeTotal20);

            grandTotal += calculateDenomination(ebodrecc.getDenomQuantity50().getValue(), 50,
                    ebodrecc::setDenomTypeTotal50);

            grandTotal += calculateDenomination(ebodrecc.getDenomQuantity100().getValue(), 100,
                    ebodrecc::setDenomTypeTotal100);

            grandTotal += calculateDenomination(ebodrecc.getDenomQuantity200().getValue(), 200,
                    ebodrecc::setDenomTypeTotal200);

            grandTotal += calculateDenomination(ebodrecc.getDenomQuantity500().getValue(), 500,
                    ebodrecc::setDenomTypeTotal500);

        } catch (Exception e) {

            FfEODDenomDefRtn.info("Exception occurred during denomination calculation: " + e);

        }

        FfEODDenomDefRtn.info("FINAL GRAND TOTAL CALCULATED: " + grandTotal);

        ebodrecc.getDenomOverallTotal().set(String.valueOf(grandTotal));

        FfEODDenomDefRtn.info("Denomination Overall Total Set in Record");

        currentRecord.set(ebodrecc.toStructure());

        FfEODDenomDefRtn.info("FfEODDenomDef Routine Completed");
    }

    /**
     * Helper method to calculate denomination totals
     */
    private long calculateDenomination(String quantityValue, int denomination,
            java.util.function.Consumer<String> totalSetter) {

        long result = 0;

        try {

            FfEODDenomDefRtn.info("Processing Denomination: " + denomination);

            if (quantityValue != null && !quantityValue.isEmpty()) {

                long quantity = Long.parseLong(quantityValue);

                FfEODDenomDefRtn.info("Quantity Entered: " + quantity);

                result = denomination * quantity;

                FfEODDenomDefRtn.info("Calculated Amount: " + result);

                totalSetter.accept(String.valueOf(result));

                FfEODDenomDefRtn.info("Stored Total for Denomination " + denomination);

            } else {

                FfEODDenomDefRtn.info("Quantity Empty for Denomination: " + denomination);

            }

        } catch (Exception e) {

            FfEODDenomDefRtn.info("Error calculating denomination " + denomination + " : " + e);
        }

        return result;
    }
}