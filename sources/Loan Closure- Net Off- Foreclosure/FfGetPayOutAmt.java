package com.temenos.fusion;

import java.math.BigDecimal;
import java.util.List;

import com.temenos.api.LocalRefGroup;
import com.temenos.t24.api.arrangement.accounting.Contract;
import com.temenos.t24.api.records.aaprddesaccount.AaPrdDesAccountRecord;
import com.temenos.t24.api.records.aaprddescharge.AaPrdDesChargeRecord;

/**
 * TODO: Document me!
 *
 * @author mp116300
 *
 */
public class FfGetPayOutAmt {
    
    
    public String getpayOutAmt(Contract contracRec) {
        String payoutAmt = "";

        try {
            AaPrdDesAccountRecord accRec = null;
            AaPrdDesChargeRecord cgstChargeRec = null;
            AaPrdDesChargeRecord sgstChargeRec = null;
            AaPrdDesChargeRecord igstChargeRec = null;
            AaPrdDesChargeRecord processChargeRec = null;
            AaPrdDesChargeRecord insuranceChargeRec = null;
            AaPrdDesChargeRecord pennyDropChargeRec = null;
            AaPrdDesChargeRecord hospiCashChargeRec = null;
            try {
                accRec = new AaPrdDesAccountRecord(contracRec.getConditionForProperty("ACCOUNT"));
            } catch (Exception e) {
                e.getMessage();
            }
            try {
                cgstChargeRec = new AaPrdDesChargeRecord(contracRec.getConditionForProperty("CGST"));
            } catch (Exception e) {
                e.getMessage();
            }
            try {
                sgstChargeRec = new AaPrdDesChargeRecord(contracRec.getConditionForProperty("SGST"));
            } catch (Exception e) {
                e.getMessage();
            }
            try {
                igstChargeRec = new AaPrdDesChargeRecord(contracRec.getConditionForProperty("IGST"));
            } catch (Exception e) {
                e.getMessage();
            }
            try {
                processChargeRec = new AaPrdDesChargeRecord(contracRec.getConditionForProperty("PROCESSINGFEE"));
            } catch (Exception e) {
                e.getMessage();
            }
            try {
                insuranceChargeRec = new AaPrdDesChargeRecord(contracRec.getConditionForProperty("INSURANCEFEE"));
            } catch (Exception e) {
                e.getMessage();
            }

            try {
                pennyDropChargeRec = new AaPrdDesChargeRecord(contracRec.getConditionForProperty("PENNYDROP"));
            } catch (Exception e) {
                e.getMessage();
            }
            try {
                hospiCashChargeRec = new AaPrdDesChargeRecord(contracRec.getConditionForProperty("HOSPICASH"));
            } catch (Exception e) {
                e.getMessage();
            }
            
            BigDecimal sancAmt = new BigDecimal(contracRec.getTermAmount().toString()); // stores the actual
                                                                                        // sanctioned amount
            
            BigDecimal processAmt = new BigDecimal(processChargeRec.getFixedAmount().toString());
          
            BigDecimal insurAmt = new BigDecimal(insuranceChargeRec.getFixedAmount().toString());
           // System.out.println("insurAmt" + insurAmt);
            BigDecimal igstAmt = new BigDecimal(igstChargeRec.getFixedAmount().toString());
           // System.out.println("igstAmt" + igstAmt);
            BigDecimal sgstAmt = new BigDecimal(sgstChargeRec.getFixedAmount().toString());
           // System.out.println("sgstAmt" + sgstAmt);
            BigDecimal cgstAmt = new BigDecimal(cgstChargeRec.getFixedAmount().toString());
           // System.out.println("cgstAmt" + cgstAmt);
            BigDecimal pennyAmt = new BigDecimal(pennyDropChargeRec.getFixedAmount().toString());
            BigDecimal hospiAmt = new BigDecimal(hospiCashChargeRec.getFixedAmount().toString());
            BigDecimal chargeTotal = processAmt.add(insurAmt).add(igstAmt).add(sgstAmt).add(cgstAmt).add(pennyAmt).add(hospiAmt); 
                                                                                            

            List<LocalRefGroup> closeLoanList = accRec.getLocalRefGroups("FF.CLS.LOAN");
           // System.out.println("closeLoanList" + closeLoanList);
            List<LocalRefGroup> repayLoanList = accRec.getLocalRefGroups("FF.REPAY.LOAN");
           // System.out.println("repayLoanList" + repayLoanList);

            BigDecimal ffCloseLoanAmt = getCloseLoanAmt(closeLoanList);
           // System.out.println("ffCloseLoanAmt" + ffCloseLoanAmt);
            BigDecimal ffRepayLoanAmt = getRepayLoanAmt(repayLoanList);
           // System.out.println("ffRepayLoanAmt" + ffRepayLoanAmt);
            BigDecimal fLocalrefTotal = ffCloseLoanAmt.add(ffRepayLoanAmt); // Total Sum of Local reference
                                                                            // fields
           // System.out.println("fLocalrefTotal" + fLocalrefTotal);
            BigDecimal completeSum = chargeTotal.add(fLocalrefTotal);
           // System.out.println("completeSum" + completeSum);
            BigDecimal subtractFromTerm = sancAmt.subtract(completeSum); // Subtraction
           // System.out.println("subtractFromTerm" + subtractFromTerm);
            payoutAmt = String.valueOf(subtractFromTerm);

        } catch (Exception e) {
            e.getMessage();
        }
        return payoutAmt;

    }

    public BigDecimal getCloseLoanAmt(List<LocalRefGroup> closeLoanList) {

        BigDecimal tempCloseLoan = BigDecimal.ZERO;

        try {

            for (int i = 0; i < closeLoanList.size(); i++) {
                LocalRefGroup lRefObj = closeLoanList.get(i);
                String closeAmtStr = lRefObj.getLocalRefField("FF.CLS.AMT").toString();

                if (!closeAmtStr.isEmpty()) {

                    BigDecimal closeAmt = new BigDecimal(closeAmtStr);
                    tempCloseLoan = tempCloseLoan.add(closeAmt);
                }
            }
        } catch (Exception e) {
            e.getMessage();
        }
        return tempCloseLoan;
    }

    public BigDecimal getRepayLoanAmt(List<LocalRefGroup> repayLoanList) {

        BigDecimal tempRapayLoan = BigDecimal.ZERO;

        try {
            for (int i = 0; i < repayLoanList.size(); i++) {
                LocalRefGroup lRefObj = repayLoanList.get(i);
                String repayAmtStr = lRefObj.getLocalRefField("FF.REPAY.AMT").toString();

                if (!repayAmtStr.isEmpty()) {
                    BigDecimal repayAmt = new BigDecimal(repayAmtStr);
                    tempRapayLoan = tempRapayLoan.add(repayAmt);
                }
            }
        } catch (Exception e) {
            e.getMessage();
        }
        return tempRapayLoan;
    }
    

}
