package com.temenos.fusion;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.temenos.api.TStructure;
import com.temenos.api.TValidationResponse;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.dates.DatesRecord;
import com.temenos.t24.api.records.ebffftpettycashlimit.EbFfFtPettyCashLimitRecord;
import com.temenos.t24.api.records.ebffpettycashroupld.EbFfPettyCashRoUpldRecord;
import com.temenos.t24.api.records.ebffpettycashroupld.FfRoExpTypeClass;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Date;
import com.temenos.t24.api.system.Session;

/**
 * @author jh116454
 * @Attached To: VERSION > EB.FF.RO.PETTY.CASH.UPD,PETTY.CASH
 * @Attached As: INPUT Routine > EB.API > FF.PETTY.LIM.OVRRIDE
 * @Description: 1)To throw error if Unutilized Amt is greater than Petty Limit.
 *               2)Same Exp Type then throw error . 3)if Exp Type , Exp Amt n
 *               Bill No is empty then throw error
 * 
 */
public class FfPettyLimOvrride extends RecordLifecycle {

    private static final FusionFileLogger yPettyLimOvrrideLog = FusionFileLogger.getLogger(FfPettyLimOvrride.class);

    String yPettyCashLimitID = "";
    String yAvailAmtS = "";
    String yExpAmt = "";
    String yExpBillID = "";
    String yCreditAmt = "";
    String yExpType = "";
    String coCode = "";
    String yYear = "";
    String yMnth = "";

    int i = 0;
    EbFfFtPettyCashLimitRecord yFfPetCasLimRec = null;
    EbFfPettyCashRoUpldRecord yEbFfPettyCashDetRec = null;
    
    DataAccess yDataAcc = new DataAccess(this);


    double yCreditAmtD = 0.0;
    double yAvailLimit = 0.0;
    double yPettyLimit = 0.0;
    double yAvailAmt = 0.0;
    
    @Override
    public TValidationResponse validateRecord(String application, String currentRecordId, TStructure currentRecord,
            TStructure unauthorisedRecord, TStructure liveRecord, TransactionContext transactionContext) {

        yEbFfPettyCashDetRec = new EbFfPettyCashRoUpldRecord(currentRecord);

        Session session = new Session(this);
        coCode = session.getCompanyId();
        Date yDate = new Date(this);
        DatesRecord yDateRec = yDate.getDates();
        String yTodDt = yDateRec.getToday().getValue();

        yYear = yTodDt.substring(0, 4);
        yMnth = yTodDt.substring(4, 6);

        getPettiLimit();

        yPettyLimOvrrideLog.info("Available Amt -> " + yAvailLimit);
        yPettyLimOvrrideLog.info("Petty Limit -> " + yPettyLimit);      
        
        try {
            List<FfRoExpTypeClass> yExpTypeList = yEbFfPettyCashDetRec.getFfRoExpType();
            for (i = 0; i < yExpTypeList.size(); i++) {

                yExpAmt = yEbFfPettyCashDetRec.getFfRoExpType().get(i).getFfRoExpAmt().getValue();
                yExpBillID = yEbFfPettyCashDetRec.getFfRoExpType().get(i).getFfRoExpBillRef().getValue();

                yExpType = yEbFfPettyCashDetRec.getFfRoExpType().get(i).getFfRoExpType().getValue();

                getExpDet();               
            }

        } catch (Exception ex) {
            yPettyLimOvrrideLog.info("petty Cash Details Missing ");
        }

        try {
            yCreditAmt = yEbFfPettyCashDetRec.getCreditAmt().getValue();
            yCreditAmtD = Double.parseDouble(yCreditAmt);
        } catch (Exception ex) {
            yPettyLimOvrrideLog.info("Credit Amt Missing");
        }
        yPettyLimOvrrideLog.info("Credit Amt -> " + yCreditAmt);

       

        if (yAvailAmtS.equals("")) {
            yAvailAmt = yPettyLimit - yCreditAmtD;
        } else {
            yAvailAmt = yAvailLimit - yCreditAmtD;
        }
        yPettyLimOvrrideLog.info("Avail Limit - Credit Amt -> " + yAvailAmt);

        if (yAvailAmt < 0) {
            yEbFfPettyCashDetRec.getUnutilizedAmt().setError("- Amount Limit Exceed than EB.PETTY.CASH.LIMIT");
        }

        try {
            Set<String> yExpNewSet = new HashSet<>();
            for (int j = 0; j < yEbFfPettyCashDetRec.getFfRoExpType().size(); j++) {

                String yExpenseType = yEbFfPettyCashDetRec.getFfRoExpType().get(j).getFfRoExpType().getValue();

                if ((yExpenseType != null) && (!yExpenseType.isEmpty()) && (!yExpNewSet.add(yExpenseType))) {
                    yEbFfPettyCashDetRec.getFfRoExpType().get(j).getFfRoExpType()
                            .setError(" - Repeated Exp Type Not Allowed");
                    yPettyLimOvrrideLog.info("Repeated Exp");
                }
            }
        } catch (Exception ex) {
            yPettyLimOvrrideLog.info("Repeat Exp Missing");
        }

        currentRecord.set(yEbFfPettyCashDetRec.toStructure());

        yPettyLimOvrrideLog.info("----Petty Lim error Ends----");
        return yEbFfPettyCashDetRec.getValidationResponse();
    }

    private void getPettiLimit() {
        try {
            yPettyCashLimitID = coCode + "-" + yMnth + yYear;
            yPettyLimOvrrideLog.info("yPettyCashLimitID -> " + yPettyCashLimitID);

            yFfPetCasLimRec = new EbFfFtPettyCashLimitRecord(
                    yDataAcc.getRecord("EB.FF.FT.PETTY.CASH.LIMIT", yPettyCashLimitID));
        } catch (Exception ex) {
            yPettyLimOvrrideLog.info("Petty Lim Rec Missing");
        }
        
        try {
            yAvailAmtS = yFfPetCasLimRec.getAvailableLimit().getValue();
            yAvailLimit = Double.parseDouble(yAvailAmtS);
        } catch (Exception ex) {
            yAvailAmtS = "";
            yPettyLimOvrrideLog.info("Availble Amt Missing");
        }
        

        try {
            yPettyLimit = Double.parseDouble(yFfPetCasLimRec.getPettyCashLimit().getValue());
        } catch (Exception ex) {
            yPettyLimOvrrideLog.info("PettyLimit Missing");
        }      
        
    }
    private void getExpDet() {

        if (yExpType.equals("")) {
            yEbFfPettyCashDetRec.getFfRoExpType().get(i).getFfRoExpType().setError(" - Input Exp Type");
            yPettyLimOvrrideLog.info("petty Cash Details Missing ");
        }

        if ((!yExpType.equals("")) && (yExpBillID.equals(""))) {
            yPettyLimOvrrideLog.info(" Bill Id Empty");
            yEbFfPettyCashDetRec.getFfRoExpType().get(i).getFfRoExpBillRef().setError(" - Bill No Missing");
        }

        if ((!yExpType.equals("")) && (yExpAmt.equals(""))) {
            yPettyLimOvrrideLog.info(" Amt Empty");
            yEbFfPettyCashDetRec.getFfRoExpType().get(i).getFfRoExpAmt().setError(" - Exp Amount Missing");
        }

        if (!yExpAmt.equals("")) {
            double yExpAmtD = Double.parseDouble(yExpAmt);
            
            if (yExpAmtD < 0) {
                yPettyLimOvrrideLog.info(" Amt cannot be NEG");
                yEbFfPettyCashDetRec.getFfRoExpType().get(i).getFfRoExpAmt()
                        .setError(" - Amount cannot be Negative");
            }
            
            if (yExpAmt.equals("0")) {
                yPettyLimOvrrideLog.info(" Amt Zero");
                yEbFfPettyCashDetRec.getFfRoExpType().get(i).getFfRoExpAmt()
                        .setError(" - Amount cannot be 0");
            }
            
        }
 
    }

}