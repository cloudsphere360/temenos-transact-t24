package com.temenos.fusion;

import java.util.List;

import com.temenos.api.TStructure;
import com.temenos.t24.api.complex.eb.enquiryhook.EnquiryContext;
import com.temenos.t24.api.complex.eb.enquiryhook.FilterCriteria;
import com.temenos.t24.api.hook.system.Enquiry;
import com.temenos.t24.api.records.ebffeodscreen.EbFfEodScreenRecord;
import com.temenos.t24.api.system.DataAccess;

/**
 *
 * @author hs115664
 *
 */
public class FfConvBMNameDisplay extends Enquiry {

    private static final FusionFileLogger convBMNameDisplay = FusionFileLogger.getLogger(FfConvBMNameDisplay.class);
    DataAccess da = new DataAccess(this);
    String preEodInputt = "";

    @Override
    public String setValue(String value, String currentId, TStructure currentRecord,
            List<FilterCriteria> filterCriteria, EnquiryContext enquiryContext) {
        convBMNameDisplay.info("FfConvBMNameDisplay is Triggered" + currentId);
        try {
            EbFfEodScreenRecord eodRec = new EbFfEodScreenRecord(currentRecord);
            String reviewedBy = eodRec.getFfReviewedBy().getValue();
            convBMNameDisplay.info("reviewedBy" + reviewedBy);
            String inputt = eodRec.getInputter().get(0).toString();
            convBMNameDisplay.info("inputt" + inputt);
            String currNum = eodRec.getCurrNo();
            convBMNameDisplay.info("currNum" + currNum);
            if (inputt.contains("AUTO") && (reviewedBy.equalsIgnoreCase("Yes"))) {
                int previousCurrNum = Integer.parseInt(currNum) - 1;
                convBMNameDisplay.info("previousCurrNum" + previousCurrNum + "***" + currentId + ";" + previousCurrNum);
                EbFfEodScreenRecord preEodRec = null;
                try { 
                    preEodRec = new EbFfEodScreenRecord(
                            da.getRecord("", "EB.FF.EOD.SCREEN", "$HIS", currentId + ";" + previousCurrNum));
                    preEodInputt = preEodRec.getInputter(0).split("_")[1];
                    convBMNameDisplay.info("preEodInputt" + preEodInputt);
                } catch (Exception e) {
                    preEodInputt = inputt.split("_")[1];
                    convBMNameDisplay.error("preEodRec error" + e.getMessage());
                }

            } else {
                preEodInputt = inputt.split("_")[1];
                convBMNameDisplay.info("preEodInputt from live" + preEodInputt);
            }
        } catch (Exception e) {
            convBMNameDisplay.error("FfConvBMNameDisplay error" + e.getMessage());
        }

        return preEodInputt;
    }
}
