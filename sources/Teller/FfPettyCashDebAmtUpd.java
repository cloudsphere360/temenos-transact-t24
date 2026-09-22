package com.temenos.fusion;

import java.util.List;

import com.temenos.api.TStructure;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.dates.DatesRecord;
import com.temenos.t24.api.records.ebffftpettycashlimit.EbFfFtPettyCashLimitRecord;
import com.temenos.t24.api.records.ebffparameter.EbFfParameterRecord;
import com.temenos.t24.api.records.ebffparameter.MatchingItemClass;
import com.temenos.t24.api.records.ebffpettycashroupld.EbFfPettyCashRoUpldRecord;
import com.temenos.t24.api.records.ebffpettycashroupld.FfRoExpTypeClass;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Date;
import com.temenos.t24.api.system.Session;

/**
 * @author jh116454
 * @Attached To: VERSION > EB.FF.PETTY.CASH.RO.UPLD,RO.PETTY.CASH
 * @Attached As: DEFAULT ROUTINE > EB.API > FF.PETTY.CASH.DEB.AMT.UPD
 * @Description: To default the sum off all the EXP amount(credit amt),
 *               withdrawal amt from the table EB.FF.FT.PETTY.CASH.LIMIT(petty
 *               limit), unutilzd amt, all credit accounts from the parameter
 *               EB.FF.PARAMETER > RO.PETTY.CREDIT.ACC
 * 
 */

public class FfPettyCashDebAmtUpd extends RecordLifecycle {
    private static final FusionFileLogger yPettyCashDebAmtUpdLog = FusionFileLogger
            .getLogger(FfPettyCashDebAmtUpd.class);

    String coCode = "";
    String yYear = "";
    String yMnth = "";
    String yExpType = "";
    String yPrCreAcc = "";
    String yEbparmExpType = "";
    String yExpAmt = "";
    String yAvaiLimAmt = "";
    String yPettyLim = "";
    String yUnUtiziedAmt = "";
    String yCreditAcct = "";

    double yCretAmt = 0.0;
    double yTotalAmt = 0.0;
    double yPettyLimit = 0.0;

    EbFfFtPettyCashLimitRecord yFfPetCasLimRec = null;
    EbFfParameterRecord yCrediAmtParam = null;
    EbFfPettyCashRoUpldRecord yEbFfPettyCashDetRec = null;
    
    DataAccess yDataAcc = new DataAccess(this);
    int i = 0;
    @Override
    public void defaultFieldValues(String application, String currentRecordId, TStructure currentRecord,
            TStructure unauthorisedRecord, TStructure liveRecord, TransactionContext transactionContext) {

        Session session = new Session(this);
        coCode = session.getCompanyId();
        Date yDate = new Date(this);
        DatesRecord yDateRec = yDate.getDates();
        String yTodDt = yDateRec.getToday().getValue();

        yYear = yTodDt.substring(0, 4);
        yMnth = yTodDt.substring(4, 6);

        yPettyCashDebAmtUpdLog.info("!---- Petty Cash Deb Amt Upd Starts ---!");

        yEbFfPettyCashDetRec = new EbFfPettyCashRoUpldRecord(currentRecord);
        

        List<FfRoExpTypeClass> yExpTypeList = yEbFfPettyCashDetRec.getFfRoExpType();

        try {
            for (i = 0; i < yExpTypeList.size(); i++) {
                yExpType = yEbFfPettyCashDetRec.getFfRoExpType().get(i).getFfRoExpType().getValue();

                getPLAccount();                

                yExpAmt = yEbFfPettyCashDetRec.getFfRoExpType().get(i).getFfRoExpAmt().getValue();
                if (yExpAmt != null && !yExpAmt.isEmpty()) {
                    yTotalAmt = Double.parseDouble(yExpAmt);
                }
                yCretAmt += yTotalAmt;                
            }
            yPettyCashDebAmtUpdLog.info(" yCretAmt -> " + yCretAmt);
        } catch (Exception ex) {
            yPettyCashDebAmtUpdLog.info("Eb Parameter Missing");
        }
        
        yPettyCashDebAmtUpdLog.info("Total Credit Amt -> " + yCretAmt);

        getPettyLim();
        getyUnUtiziedAmt();
        getCredAcct();

        yEbFfPettyCashDetRec.setCreditAcNo(yCreditAcct);
        yEbFfPettyCashDetRec.setUnutilizedAmt(yUnUtiziedAmt);
        yEbFfPettyCashDetRec.setCreditAmt(String.valueOf(yCretAmt));
        yEbFfPettyCashDetRec.setWithdAmt(String.valueOf(yPettyLimit));

        currentRecord.set(yEbFfPettyCashDetRec.toStructure());
    }

    private void getPLAccount() {

        try {
            yCrediAmtParam = new EbFfParameterRecord(
                    yDataAcc.getRecord("EB.FF.PARAMETER", "RO.PETTY.CREDIT.ACC"));
            List<MatchingItemClass> yDebExp = yCrediAmtParam.getMatchingItem();
            for (int j = 0; j < yDebExp.size(); j++) {
                yEbparmExpType = yCrediAmtParam.getMatchingItem().get(j).getMatchingItem().getValue();
                if (yEbparmExpType.equals(yExpType)) {
                    yPrCreAcc = yCrediAmtParam.getMatchingItem(j).getMatchingValue().getValue();
                    yEbFfPettyCashDetRec.getFfRoExpType().get(i).setFfRoExpAcc(yPrCreAcc);
                    yPettyCashDebAmtUpdLog.info(yEbparmExpType);
                    yPettyCashDebAmtUpdLog.info("PL Credit Acct No -> " + yPrCreAcc);
                }
            }

        } catch (Exception ex) {
            yPettyCashDebAmtUpdLog.info("Eb Parameter Missing");
        }
        
    }
    private void getPettyLim() {
        try {

            String yPettyCashLimitID = coCode + "-" + yMnth + yYear;

            yFfPetCasLimRec = new EbFfFtPettyCashLimitRecord(
                    yDataAcc.getRecord("EB.FF.FT.PETTY.CASH.LIMIT", yPettyCashLimitID));
            yAvaiLimAmt = yFfPetCasLimRec.getAvailableLimit().getValue();
            yPettyLim = yFfPetCasLimRec.getPettyCashLimit().getValue();
        } catch (Exception ex) {
            yPettyCashDebAmtUpdLog.info("Petty Limit Rec " + ex);
        }

        if (yPettyLim != null && !yPettyLim.isEmpty()) {
            yPettyLimit = Double.parseDouble(yPettyLim);
        }
        yPettyCashDebAmtUpdLog.info(" yPettyLimit -> " + yPettyLimit);
    }

    private void getyUnUtiziedAmt() {

        if (yAvaiLimAmt != null && !yAvaiLimAmt.isEmpty()) {
            double yAvailLimit = Double.parseDouble(yAvaiLimAmt);
            double yUnutliAmtD = yAvailLimit - yCretAmt;
            yUnUtiziedAmt = String.valueOf(yUnutliAmtD);
        } else {
            double yUnutliAmtD = yPettyLimit - yCretAmt;
            yUnUtiziedAmt = String.valueOf(yUnutliAmtD);
        }

        yPettyCashDebAmtUpdLog.info("UnutliAmtD-> " + yUnUtiziedAmt);
    }

    private void getCredAcct() {
        String yCredAcct = "INR100700001";
        String ySubCode = coCode.substring(5, 9);
        yCreditAcct = yCredAcct + ySubCode;
        yPettyCashDebAmtUpdLog.info(" CreditAcct -> " + yCreditAcct);
    }
}
