package com.temenos.fusion;

import java.util.ArrayList;
import java.util.List;

import com.temenos.t24.api.complex.eb.enquiryhook.EnquiryContext;
import com.temenos.t24.api.complex.eb.enquiryhook.FilterCriteria;
import com.temenos.t24.api.hook.system.Enquiry;
import com.temenos.t24.api.records.ebpettycashlimit.EbPettyCashLimitRecord;
import com.temenos.t24.api.system.DataAccess;

/**
 *
 * @author jh116454
 *
 */
public class ENoifleFfPettyLimView extends Enquiry {

    private static final FusionFileLogger yPettyViewLog = FusionFileLogger.getLogger(ENoifleFfPettyLimView.class);

    List<String> yPettyLimBranList = new ArrayList<>();
    List<String> yAllpettyLimIds = null;

    DataAccess yDataAcc = new DataAccess();

    String yBranchID = "";
    String yTodDt = "";
    String yPettyCash = "";

    @Override
    public List<String> setIds(List<FilterCriteria> filterCriteria, EnquiryContext enquiryContext) {

        yPettyViewLog.info("---- Starts----");
        for (FilterCriteria criteria : filterCriteria) {

            if (criteria.getFieldname().equals("BRANCH.ID")) {
                yBranchID = criteria.getValue();
                yPettyViewLog.info(" Branch ID -> " + yBranchID);
            }
        }

        if (!yBranchID.equals("")) {
            yAllpettyLimIds = yDataAcc.selectRecords("", "EB.PETTY.CASH.LIMIT", "", " WITH @ID EQ " + yBranchID);
            yPettyViewLog.info(" yAllpettyLimIds only Branch-> " + yAllpettyLimIds);
        } else {
            yAllpettyLimIds = yDataAcc.selectRecords("", "EB.PETTY.CASH.LIMIT", "", " WITH @ID NE '' ");
            yPettyViewLog.info(" yAllpettyLimIds -> " + yAllpettyLimIds);
        }

        for (String yPettyID : yAllpettyLimIds) {
            try {
                EbPettyCashLimitRecord yEbPettyCashRec = new EbPettyCashLimitRecord(
                        yDataAcc.getRecord("EB.PETTY.CASH.LIMIT", yPettyID));

                yPettyCash = yEbPettyCashRec.getPettyCash().getValue();

            } catch (Exception e) {
                yPettyCash = "0";
                yPettyViewLog.info("Petty Cash Missing -> " + e);
            }
            
            yPettyLimBranList.add(yBranchID + "*" + yPettyCash);
            yPettyViewLog.info("---- Ends ----");

        }
        return yPettyLimBranList;
    }

}
