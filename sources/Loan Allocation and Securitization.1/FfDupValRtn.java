package com.temenos.fusion;

import com.temenos.api.TField;
import com.temenos.api.TStructure;
import com.temenos.api.TValidationResponse;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.ebffcustomerfunder.EbFfCustomerFunderRecord;
import java.util.HashSet;
import java.util.Set;

/**
 * TODO: Document me!
 *
 * @author sr115630
 *
 */


public class FfDupValRtn extends RecordLifecycle {
    private static final FusionFileLogger FfDupValRtn = FusionFileLogger
            .getLogger(FfDupValRtn.class);
    @Override
    public TValidationResponse validateField(String application, String recordId, String fieldData, TStructure record) {
        FfDupValRtn.info("Entering to method");
        EbFfCustomerFunderRecord ebfflCustomer = new EbFfCustomerFunderRecord(record);
        FfDupValRtn.info("ebfflCustomer"+ ebfflCustomer);
        Set<String> funderSet = new HashSet<>();
        for(TField funderType:ebfflCustomer.getFunderType()) {
            if(!funderSet.add(funderType.getValue())) {
                funderType.setError("This is duplicate");
                FfDupValRtn.info("funderSet"+funderSet);
                FfDupValRtn.info("funderType"+funderType);
            }
            
        }
          
        return ebfflCustomer.getValidationResponse();
    }
}
