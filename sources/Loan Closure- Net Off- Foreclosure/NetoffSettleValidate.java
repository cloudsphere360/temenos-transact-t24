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
import com.temenos.t24.api.records.aaprddessettlement.AaPrdDesSettlementRecord;
import com.temenos.t24.api.records.aaproductcatalog.AaProductCatalogRecord;

/**
 * TODO: Document me!
 *
 * @author mp116300
 *
 */
public class NetoffSettleValidate extends ActivityLifecycle {

    private static final String L3API = "L3API";
    private static final Logger LOGGER = LoggerFactory.getLogger(L3API);

    @Override
    public TValidationResponse validateRecord(AaAccountDetailsRecord accountDetailRecord,
            AaArrangementActivityRecord arrangementActivityRecord, ArrangementContext arrangementContext,
            AaArrangementRecord arrangementRecord, AaArrangementActivityRecord masterActivityRecord,
            TStructure productPropertyRecord, AaProductCatalogRecord productRecord, TStructure record) {

        FfGetPayOutAmt getPayObj = new FfGetPayOutAmt();
        Contract contracRec = new Contract(this);
        AaPrdDesSettlementRecord tsettleRec = null;

        try {
            LOGGER.info("validate routine triggers");
            String arrangementId = arrangementActivityRecord.getArrangement().getValue();
            LOGGER.info("arrangementId: " + arrangementId);
            contracRec.setContractId(arrangementId);

            String tpayoutAmtSettle = getPayObj.getpayOutAmt(contracRec);
            LOGGER.info("The Payout amount of Settlement: " + tpayoutAmtSettle);

            try {
                tsettleRec = new AaPrdDesSettlementRecord(record);
                LOGGER.info("settleRec" + tsettleRec);
            } catch (Exception e) {
                e.getMessage();
            }
            if (new BigDecimal(tpayoutAmtSettle.replace(",", "")).compareTo(BigDecimal.ZERO) < 0) {
                LOGGER.info(
                        "The PayoutAmt from the settlement inside if condition of less than zero: " + tpayoutAmtSettle);

               // tsettleRec.getPayoutCurrency(0).getPayoutAccount(0).getPayoutAmount()
                 //       .setError("EB-FF.NETOFF.SETTLE.VALIDATE.ERROR");
                throw new T24CoreException("", "EB-FF.NETOFF.SETTLE.VALIDATE.ERROR");
            }

        } catch (Exception e) {
            e.getMessage();

        }

        record.set(tsettleRec.toStructure());
        return tsettleRec.getValidationResponse();

    }

}
