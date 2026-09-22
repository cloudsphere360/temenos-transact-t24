package com.temenos.fusion;

import java.math.BigDecimal;

import com.temenos.api.TStructure;
import com.temenos.api.TValidationResponse;
import com.temenos.api.exceptions.T24CoreException;
import com.temenos.logging.facade.Logger;
import com.temenos.logging.facade.LoggerFactory;
import com.temenos.t24.api.arrangement.accounting.Contract;
import com.temenos.t24.api.complex.aa.activityhook.ArrangementContext;
import com.temenos.t24.api.hook.arrangement.ActivityLifecycle;
import com.temenos.t24.api.records.aaaccountdetails.AaAccountDetailsRecord;
import com.temenos.t24.api.records.aaarrangement.AaArrangementRecord;
import com.temenos.t24.api.records.aaarrangementactivity.AaArrangementActivityRecord;
import com.temenos.t24.api.records.aaprddespaymentschedule.AaPrdDesPaymentScheduleRecord;
import com.temenos.t24.api.records.aaproductcatalog.AaProductCatalogRecord;

/**
 * TODO: Document me!
 *
 * @author mp116300
 *
 */
public class NetoffScheduleValidate extends ActivityLifecycle {

    private static final String L3API = "L3API";
    private static final Logger LOGGER = LoggerFactory.getLogger(L3API);

    @Override
    public TValidationResponse validateRecord(AaAccountDetailsRecord accountDetailRecord,
            AaArrangementActivityRecord arrangementActivityRecord, ArrangementContext arrangementContext,
            AaArrangementRecord arrangementRecord, AaArrangementActivityRecord masterActivityRecord,
            TStructure productPropertyRecord, AaProductCatalogRecord productRecord, TStructure record) {

        Contract contracRec = new Contract(this);
        FfGetPayOutAmt getPayObj = new FfGetPayOutAmt();

        AaPrdDesPaymentScheduleRecord tschedRec = null;
        try {
            LOGGER.info("validate routine triggers settle");
            String arrangementId = arrangementActivityRecord.getArrangement().getValue();
            LOGGER.info("arrangementId: " + arrangementId);
            contracRec.setContractId(arrangementId);
            String tpayoutAmtSched = "";

            try {
                tschedRec = new AaPrdDesPaymentScheduleRecord(record);
                LOGGER.info("schedRec: " + tschedRec);
            } catch (Exception e) {
                e.getMessage();
            }
            tpayoutAmtSched = getPayObj.getpayOutAmt(contracRec);

            if (new BigDecimal(tpayoutAmtSched.replace(",", "")).compareTo(BigDecimal.ZERO) < 0) {
                LOGGER.info(
                        "The PayoutAmt from the schedule inside if condition of less than zero: " + tpayoutAmtSched);
                // tschedRec.getPaymentType(0).getPercentage(0).getActualAmt()
                // .setError("EB-FF.NETOFF.SCHED.VALIDATE.ERROR");

                throw new T24CoreException("", "EB-FF.NETOFF.SCHED.VALIDATE.ERROR");
            }

        } catch (Exception e) {
            e.getMessage();
        }

        record.set(tschedRec.toStructure());
        return tschedRec.getValidationResponse();

    }
}
