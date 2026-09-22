package com.temenos.fusion;

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
public class FfMandtryFldValdn extends RecordLifecycle {
    private static final FusionFileLogger FF_MANDTRY_FLD_VALDN = FusionFileLogger.getLogger(FfMandtryFldValdn.class);
    @Override
    public TValidationResponse validateField(String application, String recordId, String fieldData, TStructure record) {
        
        EbFfFunderDetailsRecord ebfflCustomer = new EbFfFunderDetailsRecord(record);
        try {
            for (int j = 0; j < ebfflCustomer.getFfFunderNameTranche().size(); j++) {

                String funderTranche = ebfflCustomer.getFfFunderNameTranche().get(j).getFfFunderNameTranche().getValue();
                String funderClassification=ebfflCustomer.getFfFunderNameTranche().get(j).getFunderClassification().getValue();
                String fundingAmount= ebfflCustomer.getFfFunderNameTranche().get(j).getFfFundingAmount().getValue();
                String dateOfFunding = ebfflCustomer.getFfFunderNameTranche().get(j).getFfDateOfFunding().getValue();
                String effStartDate =ebfflCustomer.getFfFunderNameTranche().get(j).getFfEffectiveStartDateOfFund().getValue();
                String interestRate= ebfflCustomer.getFfFunderNameTranche().get(j).getFfInterestRate().getValue();
                if (funderTranche.equals("")) {
                    ebfflCustomer.getFfFunderNameTranche().get(j).getFfFunderNameTranche().setError("Ff Funder Name Tranche is missing ");
                    FF_MANDTRY_FLD_VALDN.info("petty Cash Details Missing ");
                }
    
                if (funderClassification.equals("")) {
                    FF_MANDTRY_FLD_VALDN.info(" Bill Id Empty");
                    ebfflCustomer.getFfFunderNameTranche().get(j).getFunderClassification().setError("Ff Funder Calssification is missing ");
                }
    
                if (fundingAmount.equals("")) {
                    FF_MANDTRY_FLD_VALDN.info(" Empty");
                    ebfflCustomer.getFfFunderNameTranche().get(j).getFfFundingAmount().setError("Ff Funding Amount is missing ");
                }
                if (dateOfFunding.equals("")) {
                    FF_MANDTRY_FLD_VALDN.info("its Empty");
                    ebfflCustomer.getFfFunderNameTranche().get(j).getFfDateOfFunding().setError("Ff Funding Amount is missing ");
                }
                if (effStartDate.equals("")) {
                    FF_MANDTRY_FLD_VALDN.info(" Amt Empty");
                    ebfflCustomer.getFfFunderNameTranche().get(j).getFfEffectiveStartDateOfFund().setError("Ff Effective Start Date is missing ");
                }
                if (interestRate.equals("")) {
                    FF_MANDTRY_FLD_VALDN.info(" Amt Empty");
                    ebfflCustomer.getFfFunderNameTranche().get(j).getFfInterestRate().setError("Ff Interest Rate is missing ");
                }
                               
            }
        } catch (Exception e) {
            FF_MANDTRY_FLD_VALDN.error("Routine not working");
        }
        
        return ebfflCustomer.getValidationResponse();
    }
    

}
